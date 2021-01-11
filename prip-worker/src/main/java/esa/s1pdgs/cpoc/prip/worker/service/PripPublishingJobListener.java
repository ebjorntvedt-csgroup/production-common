package esa.s1pdgs.cpoc.prip.worker.service;

import static esa.s1pdgs.cpoc.mqi.model.queue.util.CompressionEventUtil.removeZipSuffix;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.appstatus.AppStatus;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.common.utils.Retries;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.MessageFilter;
import esa.s1pdgs.cpoc.mqi.client.MqiConsumer;
import esa.s1pdgs.cpoc.mqi.client.MqiListener;
import esa.s1pdgs.cpoc.mqi.client.MqiMessageEventHandler;
import esa.s1pdgs.cpoc.mqi.client.MqiPublishingJob;
import esa.s1pdgs.cpoc.mqi.model.queue.NullMessage;
import esa.s1pdgs.cpoc.mqi.model.queue.PripPublishingJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.prip.metadata.PripMetadataRepository;
import esa.s1pdgs.cpoc.prip.model.Checksum;
import esa.s1pdgs.cpoc.prip.model.GeoShapePolygon;
import esa.s1pdgs.cpoc.prip.model.PripGeoCoordinate;
import esa.s1pdgs.cpoc.prip.model.PripMetadata;
import esa.s1pdgs.cpoc.prip.worker.configuration.ApplicationProperties;
import esa.s1pdgs.cpoc.prip.worker.mapping.MdcToPripMapper;
import esa.s1pdgs.cpoc.prip.worker.report.PripReportingInput;
import esa.s1pdgs.cpoc.prip.worker.report.PripReportingOutput;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingInput;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

@Service
public class PripPublishingJobListener implements MqiListener<PripPublishingJob> {

	private static final Logger LOGGER = LogManager.getLogger(PripPublishingJobListener.class);

	private final GenericMqiClient mqiClient;
	private final List<MessageFilter> messageFilter;
	private final ObsClient obsClient;
	private final MetadataClient metadataClient;
	private final long pollingIntervalMs;
	private final long pollingInitialDelayMs;
	private final PripMetadataRepository pripMetadataRepo;
	private final AppStatus appStatus;
	private final ApplicationProperties props;
	private final ErrorRepoAppender errorAppender;
	private final MdcToPripMapper mdcToPripMapper;
	
	@Autowired
	public PripPublishingJobListener(
			final GenericMqiClient mqiClient,
			final List<MessageFilter> messageFilter,
			final ObsClient obsClient,
			final MetadataClient metadataClient,
			final PripMetadataRepository pripMetadataRepo,
			@Value("${prip-worker.publishing-job-listener.polling-interval-ms}") final long pollingIntervalMs,
			@Value("${prip-worker.publishing-job-listener.polling-initial-delay-ms}") final long pollingInitialDelayMs,
			final AppStatus appStatus,
			final ApplicationProperties props,
			final ErrorRepoAppender errorAppender
	) {
		this.mqiClient = mqiClient;
		this.messageFilter = messageFilter;
		this.obsClient = obsClient;
		this.metadataClient = metadataClient;
		this.pripMetadataRepo = pripMetadataRepo;
		this.pollingIntervalMs = pollingIntervalMs;
		this.pollingInitialDelayMs = pollingInitialDelayMs;
		this.appStatus = appStatus;
		this.props = props;
		this.errorAppender = errorAppender;
		mdcToPripMapper = new MdcToPripMapper(props.getProductTypeRegexp(), props.getMetadataMapping());
	}
	
	@PostConstruct
	public void initService() {
		if (pollingIntervalMs > 0) {
			final ExecutorService service = Executors.newFixedThreadPool(1);
			service.execute(new MqiConsumer<>(
					mqiClient,
					ProductCategory.PRIP_JOBS,
					this,
					messageFilter,
					pollingIntervalMs,
					pollingInitialDelayMs,
					appStatus
			));
		}
	}

	@Override
	public MqiMessageEventHandler onMessage(final GenericMessageDto<PripPublishingJob> message) {
		LOGGER.debug("starting saving PRIP metadata, got message: {}", message);

		final PripPublishingJob publishingJob = message.getBody();

		final Reporting reporting = ReportingUtils.newReportingBuilder()
				.predecessor(publishingJob.getUid())
				.newReporting("PripWorker");

		final String name = removeZipSuffix(publishingJob.getKeyObjectStorage());

		final ReportingInput in = PripReportingInput.newInstance(name, publishingJob.getProductFamily());
		reporting.begin(in, new ReportingMessage("Publishing file %s in PRIP", name));
		
		return new MqiMessageEventHandler.Builder<NullMessage>(ProductCategory.UNDEFINED)
				.onSuccess(res -> reporting.end(
						PripReportingOutput.newInstance(new Date()), 
						new ReportingMessage("Finished publishing file %s in PRIP", name)
				))
				.onError(e -> {
					final String errorMessage = String.format("Error on publishing file %s in PRIP: %s", name, LogUtils.toString(e));
					reporting.error(new ReportingMessage(errorMessage));
					LOGGER.error(errorMessage);
					errorAppender.send(
							new FailedProcessingDto(props.getHostname(), new Date(), errorMessage, message));

				})
				.publishMessageProducer(() -> {
					createAndSave(publishingJob);
		    		return new MqiPublishingJob<NullMessage>(Collections.emptyList());
				})
				.newResult();
	}

	private final void createAndSave(final PripPublishingJob publishingJob) throws MetadataQueryException, InterruptedException {
		final LocalDateTime creationDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

		final SearchMetadata searchMetadata = queryMetadata(
				publishingJob.getProductFamily(),
				publishingJob.getKeyObjectStorage()
		);

		PripMetadata pripMetadata = new PripMetadata();		
		pripMetadata.setId(UUID.randomUUID());
		pripMetadata.setObsKey(publishingJob.getKeyObjectStorage());
		pripMetadata.setName(publishingJob.getKeyObjectStorage());
		pripMetadata.setProductFamily(publishingJob.getProductFamily());
		pripMetadata.setContentType(PripMetadata.DEFAULT_CONTENTTYPE);
		pripMetadata.setContentLength(
				getContentLength(publishingJob.getProductFamily(), publishingJob.getKeyObjectStorage()));
		pripMetadata.setCreationDate(creationDate);
		pripMetadata.setEvictionDate(creationDate.plusDays(PripMetadata.DEFAULT_EVICTION_DAYS));
		pripMetadata
				.setChecksums(getChecksums(publishingJob.getProductFamily(), publishingJob.getKeyObjectStorage()));
		
		// ValidityStart: mandatory field, only optional when plan and report
		if (! ProductFamily.PLAN_AND_REPORT_ZIP.equals(publishingJob.getProductFamily()) || Strings.isNotEmpty(searchMetadata.getValidityStart())) {
			pripMetadata.setContentDateStart(DateUtils.parse(searchMetadata.getValidityStart()).truncatedTo(ChronoUnit.MILLIS));
		}
		
		// ValidityStop: mandatory field, only optional when plan and report
		if (! ProductFamily.PLAN_AND_REPORT_ZIP.equals(publishingJob.getProductFamily()) || Strings.isNotEmpty(searchMetadata.getValidityStop())) {
			pripMetadata.setContentDateEnd(DateUtils.parse(searchMetadata.getValidityStop()).truncatedTo(ChronoUnit.MILLIS));
		}
				
		Map<String, Object> pripAttributes = mdcToPripMapper.map(publishingJob.getKeyObjectStorage(),
				searchMetadata.getProductType(), searchMetadata.getAdditionalProperties());		
		pripMetadata.setAttributes(pripAttributes);
		
		final List<PripGeoCoordinate> coordinates = new ArrayList<>();
		if (null != searchMetadata.getFootprint()) {
			for (final List<Double> p : searchMetadata.getFootprint()) {
				coordinates.add(new PripGeoCoordinate(p.get(0), p.get(1)));
			}
		}
		if (!coordinates.isEmpty()) {
			pripMetadata.setFootprint(new GeoShapePolygon(coordinates));
		}	
		pripMetadataRepo.save(pripMetadata);

		LOGGER.debug("end of saving PRIP metadata: {}", pripMetadata);
	}
	
	private SearchMetadata queryMetadata(final ProductFamily productFamily, final String keyObjectStorage) throws MetadataQueryException, InterruptedException {
		return Retries.performWithRetries(() -> {
				return metadataClient.queryByFamilyAndProductName(
						removeZipSuffix(productFamily.name()),
						removeZipSuffix(keyObjectStorage)
				);
			}, 
			"metadata query for " + keyObjectStorage, 
			props.getMetadataUnavailableRetriesNumber(), 
			props.getMetadataUnavailableRetriesIntervalMs()
	    );

	}
	
	private long getContentLength(final ProductFamily family, final String key) {
		long contentLength = 0;
		try {
			contentLength = obsClient.size(new ObsObject(family, key));

		} catch (final ObsException e) {
			LOGGER.warn(String.format("could not determine content length of %s", key), e);
		}
		return contentLength;
	}

	private List<Checksum> getChecksums(final ProductFamily family, final String key) {
		final Checksum checksum = new Checksum();
		checksum.setAlgorithm("");
		checksum.setValue("");
		try {
			final String value = obsClient.getChecksum(new ObsObject(family, key));
			checksum.setAlgorithm(Checksum.DEFAULT_ALGORITHM);
			checksum.setValue(value);
		} catch (final ObsException e) {
			LOGGER.warn(String.format("could not determine checksum of %s", key), e);

		}
		return Arrays.asList(checksum);
	}
	
}
