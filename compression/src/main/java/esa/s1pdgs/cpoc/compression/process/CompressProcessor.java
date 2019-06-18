package esa.s1pdgs.cpoc.compression.process;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException.ErrorCode;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.compression.config.ApplicationProperties;
import esa.s1pdgs.cpoc.compression.file.FileDownloader;
import esa.s1pdgs.cpoc.compression.file.FileUploader;
import esa.s1pdgs.cpoc.compression.mqi.OutputProducerFactory;
import esa.s1pdgs.cpoc.compression.obs.ObsService;
import esa.s1pdgs.cpoc.compression.status.AppStatus;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.client.StatusService;
import esa.s1pdgs.cpoc.mqi.model.queue.CompressionJobDto;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;
import esa.s1pdgs.cpoc.mqi.model.rest.AckMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.LoggerReporting;
import esa.s1pdgs.cpoc.report.Reporting;

@Service
public class CompressProcessor {
	/**
	 * Logger
	 */
	private static final Logger LOGGER = LogManager.getLogger(CompressProcessor.class);

	/**
	 * Application properties
	 */
	private final ApplicationProperties properties;

	/**
	 * Application status
	 */
	private final AppStatus appStatus;

	/**
	 * Output processsor
	 */
	private final ObsService obsService;

	/**
	 * Output processsor
	 */
	private final OutputProducerFactory producerFactory;

	/**
	 * MQI service for reading message
	 */
	private final GenericMqiService<CompressionJobDto> mqiService;

	/**
	 * MQI service for stopping the MQI
	 */
	private final StatusService mqiStatusService;

	private final ErrorRepoAppender errorAppender;

	@Autowired
	public CompressProcessor(final AppStatus appStatus, final ApplicationProperties properties,
			final ObsService obsService, final OutputProducerFactory producerFactory,
			@Qualifier("mqiServiceForCompression") final GenericMqiService<CompressionJobDto> mqiService,
			final ErrorRepoAppender errorAppender,
			@Qualifier("mqiServiceForStatus") final StatusService mqiStatusService) {
		this.appStatus = appStatus;
		this.properties = properties;
		this.obsService = obsService;
		this.producerFactory = producerFactory;

		this.mqiService = mqiService;
		this.mqiStatusService = mqiStatusService;
		this.errorAppender = errorAppender;
	}

	/**
	 * Consume and execute jobs
	 */
	@Scheduled(fixedDelayString = "${compression.fixed-delay-ms}", initialDelayString = "${compression.init-delay-poll-ms}")
	public void process() {
		LOGGER.trace("[MONITOR] [step 0] Waiting message");

		// ----------------------------------------------------------
		// Read Message
		// ----------------------------------------------------------
		LOGGER.trace("[MONITOR] [step 0] Waiting message");
		if (appStatus.isShallBeStopped()) {
			LOGGER.info("[MONITOR] [step 0] The wrapper shall be stopped");
			this.appStatus.forceStopping();
			return;
		}
		GenericMessageDto<CompressionJobDto> message = null;
		try {
			message = mqiService.next();
			this.appStatus.setWaiting();
		} catch (AbstractCodedException ace) {
			LOGGER.error("[MONITOR] [step 0] [code {}] {}", ace.getCode().getCode(), ace.getLogMessage());
			message = null;
			this.appStatus.setError("NEXT_MESSAGE");
		}
		if (message == null || message.getBody() == null) {
			LOGGER.trace("[MONITOR] [step 0] No message received: continue");
			return;
		}
		if (message.getBody().getFamily().equals(ProductFamily.L0_SEGMENT)) {
			// FIXME: Segment does contain productName and key null and this not working
			LOGGER.info("Compression job is L0 segment. Deactivated due to incompatible data structure");
			return;
		}
		appStatus.setProcessing(message.getIdentifier());
		LOGGER.info("Initializing job processing {}", message);

		// ----------------------------------------------------------
		// Initialize processing
		// ------------------------------------------------------
		final Reporting.Factory reportingFactory = new LoggerReporting.Factory(LOGGER, "CompressionProcessing");

		String workDir = properties.getWorkingDirectory();

		final Reporting report = reportingFactory.newReporting(0);
		report.reportStart("Start compression processing");

		CompressionJobDto job = message.getBody();

		// Initialize the pool processor executor
		CompressExecutorCallable procExecutor = new CompressExecutorCallable(job, // getPrefixMonitorLog(MonitorLogUtils.LOG_PROCESS,
																					// job),
				"CompressionProcessor - process", properties);
		ExecutorService procExecutorSrv = Executors.newSingleThreadExecutor();
		ExecutorCompletionService<Void> procCompletionSrv = new ExecutorCompletionService<>(procExecutorSrv);

		// Initialize the input downloader
		FileDownloader fileDownloader = new FileDownloader(obsService, workDir, job,
				this.properties.getSizeBatchDownload(),
				// getPrefixMonitorLog(MonitorLogUtils.LOG_INPUT, job),
				"CompressionProcessor");

		FileUploader fileUploader = new FileUploader(obsService, producerFactory, workDir, message, job);

		// ----------------------------------------------------------
		// Process message
		// ----------------------------------------------------------
		processTask(message, fileDownloader, fileUploader, procExecutorSrv, procCompletionSrv, procExecutor, report);

		report.reportStop("End compression processing");
	}

	protected void processTask(final GenericMessageDto<CompressionJobDto> message, final FileDownloader fileDownloader,
			final FileUploader fileUploader, final ExecutorService procExecutorSrv,
			final ExecutorCompletionService<Void> procCompletionSrv, final CompressExecutorCallable procExecutor,
			final Reporting report) {
		CompressionJobDto job = message.getBody();
		int step = 0;
		boolean ackOk = false;
		String errorMessage = "";

		final FailedProcessingDto<GenericMessageDto<CompressionJobDto>> failedProc = new FailedProcessingDto<GenericMessageDto<CompressionJobDto>>();

		try {
			step = 2;

			checkThreadInterrupted();
			LOGGER.info("{} Preparing local working directory", "LOG_INPUT", // getPrefixMonitorLog(MonitorLogUtils.LOG_INPUT
					job);
			fileDownloader.processInputs();

			step = 3;
			LOGGER.info("{} Starting process executor", "LOG PROCESS"// getPrefixMonitorLog(MonitorLogUtils.LOG_PROCESS
					, job);
			procCompletionSrv.submit(procExecutor);

			step = 3;
			this.waitForPoolProcessesEnding(procCompletionSrv);
			step = 4;
			checkThreadInterrupted();
			LOGGER.info("{} Processing l0 outputs", "LOG_OUTPUT", // getPrefixMonitorLog(MonitorLogUtils.LOG_OUTPUT
					job);

			fileUploader.processOutput();

			ackOk = true;
		} catch (AbstractCodedException ace) {
			ackOk = false;

			errorMessage = String.format(
					"[s1pdgsCompressionTask] [subTask processing] [STOP KO] %s [step %d] %s [code %d] %s", "LOG_DFT", // getPrefixMonitorLog(MonitorLogUtils.LOG_DFT,
																														// job),
					step, "LOG_ERROR", // getPrefixMonitorLog(MonitorLogUtils.LOG_ERROR, job),
					ace.getCode().getCode(), ace.getLogMessage());
			report.reportError("[code {}] {}", ace.getCode().getCode(), ace.getLogMessage());

			failedProc.processingType(CompressionJobDto.class.getName()).topic(message.getInputKey())
					.processingStatus(MqiStateMessageEnum.READ).productCategory(ProductCategory.COMPRESSED_PRODUCTS)
					.failedPod(properties.getHostname()).failureDate(new Date()).failureMessage(errorMessage)
					.processingDetails(message);

		} catch (InterruptedException e) {
			ackOk = false;
			errorMessage = String.format(
					"%s [step %d] %s [code %d] [s1pdgsCompressionTask] [STOP KO] [subTask processing] [msg interrupted exception]",
					"LOG_DFT", // getPrefixMonitorLog(MonitorLogUtils.LOG_DFT, job),
					step, "LOG_ERROR", // getPrefixMonitorLog(MonitorLogUtils.LOG_ERROR, job),
					ErrorCode.INTERNAL_ERROR.getCode());
			report.reportError("Interrupted job processing");

			failedProc.processingType(CompressionJobDto.class.getName()).topic(message.getInputKey())
					.processingStatus(MqiStateMessageEnum.READ).productCategory(ProductCategory.COMPRESSED_PRODUCTS)
					.failedPod(properties.getHostname()).failureDate(new Date()).failureMessage(errorMessage)
					.processingDetails(message);

			cleanCompressionProcessing(job, procExecutorSrv);
		}

		// Ack and check if application shall stopped
		ackProcessing(message, failedProc, ackOk, errorMessage);
	}

	/**
	 * Check if thread interrupted
	 * 
	 * @throws InterruptedException
	 */
	protected void checkThreadInterrupted() throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Current thread is interrupted");
		}
	}

	/**
	 * Wait for the processes execution completion
	 * 
	 * @throws InterruptedException
	 * @throws AbstractCodedException
	 */
	protected void waitForPoolProcessesEnding(final ExecutorCompletionService<?> procCompletionSrv)
			throws InterruptedException, AbstractCodedException {
		checkThreadInterrupted();
		try {
			procCompletionSrv.take().get(properties.getTmProcAllTasksS(), TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof AbstractCodedException) {
				throw (AbstractCodedException) e.getCause();
			} else {
				throw new InternalErrorException(e.getMessage(), e);
			}
		} catch (TimeoutException e) {
			throw new InternalErrorException(e.getMessage(), e);
		}
	}

	/**
	 * @param job
	 * @param poolProcessing
	 * @param procExecutorSrv
	 */
	protected void cleanCompressionProcessing(final CompressionJobDto job, final ExecutorService procExecutorSrv) {
		procExecutorSrv.shutdownNow();
		try {
			procExecutorSrv.awaitTermination(properties.getTmProcStopS(), TimeUnit.SECONDS);
			// TODO send kill if fails
		} catch (InterruptedException e) {
			// Conserves the interruption
			Thread.currentThread().interrupt();
		}

		this.eraseDirectory(job);
	}

	private void eraseDirectory(final CompressionJobDto job) {
		try {
			LOGGER.info("Erasing local working directory for job {}", job);
			Path p = Paths.get(properties.getWorkingDirectory());
			Files.walk(p, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).map(Path::toFile)
					.peek(System.out::println).forEach(File::delete);
		} catch (IOException e) {
			LOGGER.error("{} [code {}] Failed to erase local working directory", job,
					ErrorCode.INTERNAL_ERROR.getCode());
			this.appStatus.setError("PROCESSING");
		}
	}

	/**
	 * Ack job processing and stop app if needed
	 * 
	 * @param dto
	 * @param ackOk
	 * @param errorMessage
	 */
	protected void ackProcessing(final GenericMessageDto<CompressionJobDto> dto,
			final FailedProcessingDto<GenericMessageDto<CompressionJobDto>> failed, final boolean ackOk,
			final String errorMessage) {
		boolean stopping = appStatus.getStatus().isStopping();

		// Ack
		if (ackOk) {
			ackPositively(stopping, dto);
		} else {
			ackNegatively(stopping, dto, errorMessage);
			errorAppender.send(failed);
		}

		// Check status
        LOGGER.info("Checking status consumer {}", dto.getBody());
		if (appStatus.getStatus().isStopping()) {
			// TODO send stop to the MQI
			try {
				mqiStatusService.stop();
			} catch (AbstractCodedException ace) {
				LOGGER.error("MQI service couldn't be stopped {}",ace);
//                LOGGER.error("{} {} Checking status consumer",
//                        getPrefixMonitorLog(MonitorLogUtils.LOG_STATUS,
//                                dto.getBody()),
//                        ace.getLogMessage());
			}
			System.exit(0);
		} else if (appStatus.getStatus().isFatalError()) {
			System.exit(-1);
		} else {
			appStatus.setWaiting();
		}
	}

	/**
	 * @param dto
	 * @param errorMessage
	 */
	protected void ackNegatively(final boolean stop, final GenericMessageDto<CompressionJobDto> dto,
			final String errorMessage) {
        LOGGER.info("Acknowledging negatively {} ",dto.getBody());
		try {
			mqiService.ack(new AckMessageDto(dto.getIdentifier(), Ack.ERROR, errorMessage, stop));
		} catch (AbstractCodedException ace) {
			LOGGER.error("Unable to confirm negatively request:{}",ace);
//            LOGGER.error("{} [step 5] {} [code {}] {}",
//                    getPrefixMonitorLog(MonitorLogUtils.LOG_DFT, dto.getBody()),
//                    getPrefixMonitorLog(MonitorLogUtils.LOG_ERROR,
//                            dto.getBody()),
//                    ace.getCode().getCode(), ace.getLogMessage());
		}
		appStatus.setError("PROCESSING");
	}

	protected void ackPositively(final boolean stop, final GenericMessageDto<CompressionJobDto> dto) {
		LOGGER.info("Acknowledging positively {}", dto.getBody());
		try {
			mqiService.ack(new AckMessageDto(dto.getIdentifier(), Ack.OK, null, stop));
		} catch (AbstractCodedException ace) {
			LOGGER.error("Unable to confirm positively request:{}",ace);
//            LOGGER.error("{} [step 5] {} [code {}] {}",
//                    getPrefixMonitorLog(MonitorLogUtils.LOG_DFT, dto.getBody()),
//                    getPrefixMonitorLog(MonitorLogUtils.LOG_ERROR,
//                            dto.getBody()),
//                    ace.getCode().getCode(), ace.getLogMessage());
			appStatus.setError("PROCESSING");
		}
	}

}
