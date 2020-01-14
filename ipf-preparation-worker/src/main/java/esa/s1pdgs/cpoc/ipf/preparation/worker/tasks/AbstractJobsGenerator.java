package esa.s1pdgs.cpoc.ipf.preparation.worker.tasks;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerBuildTaskTableException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerInputsMissingException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessConfiguration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.JobGeneration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.ProductMode;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.converter.TaskTableToJobOrderConverter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrder;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderInput;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderInputFile;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderOutput;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderProcParam;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderSensingTime;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderTimeInterval;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.enums.JobOrderDestination;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.enums.JobOrderFileNameType;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.metadata.SearchMetadataResult;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTable;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableDynProcParam;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableInput;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableInputAlternative;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTablePool;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableTask;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.enums.TaskTableInputOrigin;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.enums.TaskTableMandatoryEnum;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.XmlConverter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.mqi.OutputProducerFactory;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.client.SearchMetadataQuery;
import esa.s1pdgs.cpoc.metadata.model.AbstractMetadata;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobInputDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobOutputDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobPoolDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobTaskDto;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;
import esa.s1pdgs.cpoc.report.message.output.JobOrderReportingOutput;

/**
 * Class for processing product for a given task table
 * 
 * @author Cyrielle Gailliard
 */
public abstract class AbstractJobsGenerator implements Runnable {

    /**
     * Logger
     */
    protected static final Logger LOGGER = LogManager.getLogger(AbstractJobsGenerator.class);

    /**
     * Use to generate an incremental id for locally upload session files
     */
    private static final AtomicInteger INCREMENT_JOB = new AtomicInteger(0);

    /**
     * Producer in topic
     */
    private final OutputProducerFactory outputFactory;

    /**
     * XML converter
     */
    protected final XmlConverter xmlConverter;

    /**
     * 
     */
    protected final MetadataClient metadataClient;

    /**
     * 
     */
    protected final ProcessSettings l0ProcessSettings;

    protected final IpfPreparationWorkerSettings ipfPreparationWorkerSettings;

    /**
     * Applicative data service
     */
    private final AppCatalogJobClient<CatalogEvent> appDataService;

    private final String hostname;

    /**
     * Task table
     */
    protected String taskTableXmlName;
    protected TaskTable taskTable;
    protected List<List<String>> tasks;
    protected ProductMode mode;
    protected String prefixLogMonitor;
    protected String prefixLogMonitorRemove;

    /**
     * Template of job order. Contains all information except ones specific to
     * the session:
     * <ul>
     * <li>Inputs</li>
     * <li>Configuration > Sensing time</li>
     * <li>Partial outputs: the work directory shall be put in front of each
     * filename</li>
     * </ul>
     */
    protected JobOrder jobOrderTemplate;

    /**
     * List of queries for metadata
     */
    protected final Map<Integer, SearchMetadataQuery> metadataSearchQueries;

    /**
     * Constructor
     * 
     * @param xmlConverter
     */
    public AbstractJobsGenerator(final XmlConverter xmlConverter,
            final MetadataClient metadataClient,
            final ProcessSettings l0ProcessSettings,
            final IpfPreparationWorkerSettings taskTablesSettings,
            final OutputProducerFactory outputFactory,
            final AppCatalogJobClient<CatalogEvent> appDataService,
            final ProcessConfiguration processConfiguration) {
        this.xmlConverter = xmlConverter;
        this.metadataClient = metadataClient;
        this.l0ProcessSettings = l0ProcessSettings;
        this.ipfPreparationWorkerSettings = taskTablesSettings;
        this.metadataSearchQueries = new HashMap<>();
        this.outputFactory = outputFactory;
        this.tasks = new ArrayList<>();
        this.mode = ProductMode.BLANK;
        this.appDataService = appDataService;
        this.hostname = processConfiguration.getHostname();
    }

    // ----------------------------------------------------
    // INITIALIZATION
    // ----------------------------------------------------

    /**
     * @param mode
     *            the mode to set
     */
    public void setMode(final ProductMode mode) {
        this.mode = mode;
    }

    /**
     * Initialize the processor from the tasktable XML file
     * 
     * @param xmlFile
     */
    public void initialize(final File xmlFile) throws IpfPrepWorkerBuildTaskTableException {

        // Build task table
        this.taskTableXmlName = xmlFile.getName();
        this.buildTaskTable(xmlFile);
        this.prefixLogMonitor =
                "[MONITOR] [step 3] [taskTable " + this.taskTableXmlName + "]";
        this.prefixLogMonitorRemove =
                "[MONITOR] [step 4] [taskTable " + this.taskTableXmlName + "]";

        // Build jobOrder
        this.buildJobOrderTemplate();

        // Build list of metadata search query and linked to task table input
        // alternative
        this.buildMetadataSearchQuery();

        // Build the tasks
        this.buildTasks();

        // Retrieve list of inputs
        LOGGER.info(String.format("TaskTable %s initialized",
                taskTable.getProcessorName()));
    }

    /**
     * Build the object TaskTable from XML file
     * 
     * @param xmlFile
     * @throws BuildTaskTableException
     */
    private void buildTaskTable(final File xmlFile)
            throws IpfPrepWorkerBuildTaskTableException {
        // Retrieve task table
        try {
            this.taskTable = (TaskTable) xmlConverter
                    .convertFromXMLToObject(xmlFile.getAbsolutePath());
            this.taskTable.setLevel(this.l0ProcessSettings.getLevel());
        } catch (IOException | JAXBException e) {
            throw new IpfPrepWorkerBuildTaskTableException(this.taskTableXmlName,
                    e.getMessage(), e);
        }
    }

    private void buildJobOrderTemplate() {
        // Build from task table
        final TaskTableToJobOrderConverter converter = new TaskTableToJobOrderConverter();
        jobOrderTemplate = converter.apply(this.taskTable);

        // Update values from configuration file
        jobOrderTemplate.getConf().getProcParams().forEach(item -> {
            if (l0ProcessSettings.getParams().containsKey(item.getName())) {
                item.setValue(l0ProcessSettings.getParams().get(item.getName()));                
            }
        });
        jobOrderTemplate.getConf().setStdoutLogLevel(l0ProcessSettings.getLoglevelstdout());
        jobOrderTemplate.getConf().setStderrLogLevel(l0ProcessSettings.getLoglevelstderr());
        jobOrderTemplate.getConf().setProcessingStation(l0ProcessSettings.getProcessingstation());

        // Update outputs from configuration file
        this.jobOrderTemplate.getProcs().stream()
                .filter(proc -> !proc.getOutputs().isEmpty())
                .flatMap(proc -> proc.getOutputs().stream())
                .filter(output -> output
                        .getFileNameType() == JobOrderFileNameType.REGEXP)
                .forEach(output -> {
                    if (this.l0ProcessSettings.getOutputregexps()
                            .containsKey(output.getFileType())) {
                        output.setFileName(this.l0ProcessSettings
                                .getOutputregexps().get(output.getFileType()));
                    } else {
                        output.setFileName(
                                "^.*" + output.getFileType() + ".*$");
                    }
                });

        // Update the output family according configuration file
        this.jobOrderTemplate.getProcs().stream()
                .filter(proc -> !proc.getOutputs().isEmpty())
                .flatMap(proc -> proc.getOutputs().stream()).forEach(output -> {
                    if (this.ipfPreparationWorkerSettings.getOutputfamilies()
                            .containsKey(output.getFileType())) {
                        output.setFamily(this.ipfPreparationWorkerSettings
                                .getOutputfamilies().get(output.getFileType()));
                    } else {
                        output.setFamily(ProductFamily.fromValue(
                                this.ipfPreparationWorkerSettings.getDefaultfamily()));
                    }
                });
        
        // S1PRO-642: createParameter type map        
//        for (final TaskTableDynProcParam param : taskTable.getDynProcParams()) {
//        	parameterTypes.put(param.getName(), tasktableParameterTypeToReportingString(param.getType()));        	
//        }
    }
    
//    private final String tasktableParameterTypeToReportingString(final String type) {
//    	String or number or
//    	datenumber
//    	
//    	
//    	if ("string".equalsIgnoreCase(type)) {
//    		return "string";
//    	}
//    }


    
    
    private void buildMetadataSearchQuery() {
        final AtomicInteger counter = new AtomicInteger(0);
        this.taskTable.getPools().stream()
                .filter(pool -> !CollectionUtils.isEmpty(pool.getTasks()))
                .flatMap(pool -> pool.getTasks().stream())
                .filter(task -> !CollectionUtils.isEmpty(task.getInputs()))
                .flatMap(task -> task.getInputs().stream())
                .filter(input -> !CollectionUtils
                        .isEmpty(input.getAlternatives()))
                .flatMap(input -> input.getAlternatives().stream())
                .filter(alt -> alt.getOrigin() == TaskTableInputOrigin.DB)
                .collect(Collectors.groupingBy(
                        TaskTableInputAlternative::getTaskTableInputAltKey))
                .forEach((k, v) -> {
                    String fileType = k.getFileType();
                    if (this.ipfPreparationWorkerSettings.getMapTypeMeta()
                            .containsKey(k.getFileType())) {
                        fileType = this.ipfPreparationWorkerSettings.getMapTypeMeta()
                                .get(k.getFileType());
                    }
                    ProductFamily family = ProductFamily.BLANK;
                    if (this.ipfPreparationWorkerSettings.getInputfamilies()
                            .containsKey(fileType)) {
                        family = this.ipfPreparationWorkerSettings.getInputfamilies()
                                .get(fileType);
                    }
                    final SearchMetadataQuery query =
                            new SearchMetadataQuery(counter.incrementAndGet(),
                                    k.getRetrievalMode(), k.getDeltaTime0(),
                                    k.getDeltaTime1(), fileType, family);
                    this.metadataSearchQueries.put(counter.get(), query);
                    v.forEach(alt -> {
                        alt.setIdSearchMetadataQuery(counter.get());
                    });
                });
    }

    protected void buildTasks() {
        this.taskTable.getPools().forEach(pool -> {
            this.tasks.add(
                    pool.getTasks().stream().map(TaskTableTask::getFileName)
                            .collect(Collectors.toList()));
        });
    }

    // ----------------------------------------------------
    // JOB GENERATION
    // ----------------------------------------------------

    @Override
    public void run() {
        JobGeneration job = null;
        // Get a job to generate
        final Reporting reporting = ReportingUtils.newReportingBuilder().newTaskReporting("JobGenerator");
        
        try {        	
            final List<AppDataJob<CatalogEvent>> jobs = appDataService
                    .findNByPodAndGenerationTaskTableWithNotSentGeneration(
                            l0ProcessSettings.getTriggerHostname(), taskTableXmlName);
            
            // Determine job to process
            if (CollectionUtils.isEmpty(jobs)) {
            	LOGGER.trace("==  no job found in AppCatalog for taskTableXmlName {}",
						taskTableXmlName);
		
                job = null;
            } else {
                for (final AppDataJob<CatalogEvent> appDataJob : jobs) {
                    // Check if we can do a loop
                    final long currentTimestamp = System.currentTimeMillis();
                    boolean todo = false;
                    job = new JobGeneration(appDataJob, taskTableXmlName);
                    LOGGER.debug ("== new JobGeneration of job {}", job.toString());
                    switch (job.getGeneration().getState()) {
                        case INITIAL:                 
                            if (job.getGeneration().getLastUpdateDate() == null
                                    || job.getGeneration().getLastUpdateDate()
                                            .getTime() < currentTimestamp
                                                    - ipfPreparationWorkerSettings
                                                            .getWaitprimarycheck()
                                                            .getTempo()) {
                                todo = true;
                            }
                            break;
                        case PRIMARY_CHECK:
                            if (job.getGeneration().getLastUpdateDate()
                                    .getTime() < currentTimestamp
                                            - ipfPreparationWorkerSettings
                                                    .getWaitmetadatainput()
                                                    .getTempo()) {
                                todo = true;
                            }
                            break;
                        default:
                            todo = true;
                            break;
                    }
                    if (todo) {
                        job.setJobOrder(new JobOrder(this.jobOrderTemplate,
                                this.l0ProcessSettings.getLevel()));
                        for (final Integer key : metadataSearchQueries.keySet()) {
                            final SearchMetadataQuery query =
                                    metadataSearchQueries.get(key);
                            job.getMetadataQueries().put(
                            		key,
                                    new SearchMetadataResult(new SearchMetadataQuery(query))
                            );
                        }
                        break;
                    } else {
                        job = null;
                    }
                }
            }
        } catch (final AbstractCodedException ace) {
            LOGGER.error("{} cannot retrieve the current jobs: {}",
                    this.prefixLogMonitor, ace.getLogMessage());
        }

        if (job != null) {
            final String productName =
                    job.getAppDataJob().getProduct().getProductName();
            
            // Joborder name for reporting
            String jobOrderName = "NOT_KNOWN";
                  
            try {
                LOGGER.debug(
                        "{} [productName {}] [status {}] Trying job generation",
                        this.prefixLogMonitor, productName,
                        job.getGeneration().getState());

                reporting.begin(new ReportingMessage("Start job generation"));        
                
                // Check primary input
                if (job.getGeneration().getState() == AppDataJobGenerationState.INITIAL) {
                 	final Reporting reportInit = reporting.getChildFactory().newChild("JobGenerator.Init");
                    reportInit.begin(new ReportingMessage("Start init job generation"));
                    try { 
                        LOGGER.info(
                                "{} [productName {}] 1 - Checking the pre-requirements",
                                this.prefixLogMonitor, productName);
                        this.preSearch(job);

						final AppDataJob<CatalogEvent> modifiedJob = appDataService.patchJob(
                                job.getAppDataJob().getId(),
                                job.getAppDataJob(), false, true, false);
                        job.setAppDataJob(modifiedJob);
                        updateState(job, AppDataJobGenerationState.PRIMARY_CHECK, reportInit.getRootUID());
                        reportInit.end(new ReportingMessage("End init job generation"));
                    } catch (final AbstractCodedException e) {
                        LOGGER.error(
                                "{} [productName {}] 1 - Pre-requirements not checked: {}",
                                this.prefixLogMonitor, productName,
                                e.getLogMessage());                      
                        updateState(job, AppDataJobGenerationState.INITIAL, reportInit.getRootUID());
                        reportInit.error(new ReportingMessage("[code {}] {}", e.getCode().getCode(), e.getLogMessage()));
                    }
                }

                // Search input
                if (job.getGeneration().getState() == AppDataJobGenerationState.PRIMARY_CHECK) {
                	
                	final Reporting reportInputs = reporting.getChildFactory().newChild("JobGenerator.Search");
                	
                	reportInputs.begin(new ReportingMessage("Start searching inputs"));
                	
                    try {
                        LOGGER.info("{} [productName {}] 2 - Searching inputs",
                                this.prefixLogMonitor, job.getAppDataJob()
                                        .getProduct().getProductName());
                        this.inputsSearch(job);
                        updateState(job, AppDataJobGenerationState.READY, reportInputs.getRootUID());
                        reportInputs.end(new ReportingMessage("End searching inputs"));
                        
                    } catch (final AbstractCodedException e) {
                        LOGGER.error(
                                "{} [productName {}] 2 - Inputs not found: {}",
                                this.prefixLogMonitor, productName,
                                e.getLogMessage());
                        updateState(job,AppDataJobGenerationState.PRIMARY_CHECK, reportInputs.getRootUID());
                        reportInputs.error(new ReportingMessage("[code {}] {}", e.getCode().getCode(), e.getLogMessage()));
                    }
                }

                // Prepare and send job if ready
                if (job.getGeneration().getState() == AppDataJobGenerationState.READY) {
                	
                	final Reporting reportPrep = reporting.getChildFactory().newChild("JobGenerator.PrepAndSend");                  	
                  	reportPrep.begin(new ReportingMessage("Start job preparation and sending"));
                	
                    try {
                        LOGGER.info("{} [productName {}] 2 - Searching inputs",
                                this.prefixLogMonitor, job.getAppDataJob()
                                        .getProduct().getProductName());
                        this.inputsSearch(job);
                        LOGGER.info("{} [productName {}] 3 - Sending job",
                                this.prefixLogMonitor, job.getAppDataJob()
                                        .getProduct().getProductName());
                        jobOrderName = this.send(job);
                
                        updateState(job, AppDataJobGenerationState.SENT, reportPrep.getRootUID());
                       
						if (job.getGeneration().getState() == AppDataJobGenerationState.SENT) {
							reportPrep.end(new ReportingMessage("End job preparation and sending"));

						} else {
							reportPrep.error(new ReportingMessage("Job generation finished but job not sent"));
						}
                    } catch (final AbstractCodedException e) {
                        LOGGER.error("{} [productName {}] 3 - Job not send: {}",
                                this.prefixLogMonitor, productName,
                                e.getLogMessage());
                        updateState(job, AppDataJobGenerationState.READY, reportPrep.getRootUID());
                        reportPrep.error(new ReportingMessage("[code {}] {}", e.getCode().getCode(), e.getLogMessage()));
                    }
                }
                reporting.end(
                		new JobOrderReportingOutput(jobOrderName, toProcParamMap(job)), 
                		new ReportingMessage("End job generation")
                );
            } catch (final AbstractCodedException ace) {
                LOGGER.error(
                        "{} [productName {}] [code ] Cannot generate job: {}",
                        this.prefixLogMonitor, productName,
                        ace.getCode().getCode(), ace.getLogMessage());
                reporting.error(new ReportingMessage("[code {}] {}", ace.getCode().getCode(), ace.getLogMessage()));
            }        
        }
    }
    
    private final Map<String,String> toProcParamMap(final JobGeneration jobGen) {    	
    	try {
    		final Map<String,String> result = new HashMap<>();
    		
    		for (final JobOrderProcParam param : jobGen.getJobOrder().getConf().getProcParams()) {  
        		// S1PRO-699: Determine type of parameter to use the appropriate suffix in reporting
    			final String reportingType = mapTasktableTypeToReportingType(
    					getTypeForParameterName(param.getName())
    			);
    			result.put(param.getName() + reportingType, param.getValue());
    		}
    		return result;
		} catch (final Exception e) {
			// this is only used for reporting so don't break anything if this goes wrong here 
			// and provide the error message
			LOGGER.error(e);
			return Collections.singletonMap("error", LogUtils.toString(e));
		}
    }

    private void updateState(final JobGeneration job,
            final AppDataJobGenerationState newState,
            final UUID reportingRootUID
    )
        throws AbstractCodedException {
    	
    	LOGGER.info("Job generation before update: {} - {} - {} - {}", 
    			job.getAppDataJob().getId(),
                job.getGeneration().getTaskTable(), 
                newState,
                job.getGeneration()
        );
        final AppDataJob<CatalogEvent> modifiedJob = appDataService.patchTaskTableOfJob(
                job.getAppDataJob().getId(),
                job.getGeneration().getTaskTable(), newState, reportingRootUID);
        
        if (modifiedJob == null)
        {
        	throw new InternalErrorException("Catalog query returned null");
        }       
        
        LOGGER.info("Modified job generations: {}",  modifiedJob.getGenerations());
        job.updateAppDataJob(modifiedJob, taskTableXmlName);        
        LOGGER.info("Job generation after update: {}", job.getGeneration());
    	
        // Log functional logs, not clear when this is called
        if (job.getAppDataJob().getState() == AppDataJobState.TERMINATED) {
			final AppDataJob<CatalogEvent> jobDto = job.getAppDataJob();
            final List<String> taskTables =  jobDto.getGenerations().stream()
            	.map(g -> g.getTaskTable())
            	.collect(Collectors.toList());

            LOGGER.info(
                    "{} [s1pdgsTask {}JobGeneration] [STOP OK] [productName {}] [outputs {}] Job finished",
                    this.prefixLogMonitor, this.taskTable.getLevel(),
                    job.getAppDataJob().getProduct().getProductName(),
                    taskTables);
        }
    	LOGGER.debug("== Job order {} updated to state {}",  job.getAppDataJob().getId(), newState);
        
    }

    protected abstract void preSearch(JobGeneration job)
            throws IpfPrepWorkerInputsMissingException;

    protected void inputsSearch(final JobGeneration job)
            throws IpfPrepWorkerInputsMissingException {
        // First, we evaluate each input query with no found file
        LOGGER.info("{} [productName {}] 2a - Requesting metadata",
                this.prefixLogMonitor,
                job.getAppDataJob().getProduct().getProductName());
        job.getMetadataQueries().forEach((k, v) -> {
            if (v != null && v.getResult() == null) {
                try {
                	final String productType = v.getQuery().getProductType();
                	LOGGER.debug("Querying input product of type {}, AppJobId {}", productType, job.getAppDataJob().getId());
                	
                	// S1PRO-707: only "AUX_ECE" requires to query polarisation
                	final String polarisation;
                	if ("AUX_ECE".equals(productType.toUpperCase())) {
                		polarisation = getPolarisationFor(job.getAppDataJob().getProduct());
                	}
                	else {
                		polarisation = null;
                	}                	
                    final List<SearchMetadata> file = this.metadataClient.search(
                    		v.getQuery(),
                    		DateUtils.convertToAnotherFormat(
                    				job.getAppDataJob().getProduct().getStartTime(),
                    				AppDataJobProduct.TIME_FORMATTER,
                    				AbstractMetadata.METADATA_DATE_FORMATTER
                    		),
                    		DateUtils.convertToAnotherFormat(
                    				job.getAppDataJob().getProduct().getStopTime(),
                    				AppDataJobProduct.TIME_FORMATTER,
                    				AbstractMetadata.METADATA_DATE_FORMATTER
                    		),
                    		job.getAppDataJob().getProduct().getSatelliteId(),
                            job.getAppDataJob().getProduct().getInsConfId(),
                            job.getAppDataJob().getProduct().getProcessMode(),
                            polarisation
                    );
                    if (!file.isEmpty()) {
                        v.setResult(file);
                    }
                } catch (final MetadataQueryException me) {
                    LOGGER.warn(
                            "{} [productName {}] [alternative {}] Exception occurred when searching alternative: {}",
                            this.prefixLogMonitor,
                            job.getAppDataJob().getProduct().getProductName(),
                            v.getQuery().toLogMessage(), me.getMessage());
                }
            }
        });
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Search metadata queries, AppJobId {}, Query {}",job.getAppDataJob().getId(),
                    job.getMetadataQueries());
        }

        // Second, for each task check if input is mandatory and if a file exist
        LOGGER.info("{} [productName {}] 2b - Try building inputs for appJob {}",
                this.prefixLogMonitor,
                job.getAppDataJob().getProduct().getProductName(),
                job.getAppDataJob().getId());
        int counterProc = 0;
        final Map<String, JobOrderInput> referenceInputs = new HashMap<>();
        for (final TaskTablePool pool : this.taskTable.getPools()) {
            for (final TaskTableTask task : pool.getTasks()) {
                final Map<String, String> missingMetadata = new HashMap<>();
                final List<JobOrderInput> futureInputs = new ArrayList<>();
                for (final TaskTableInput input : task.getInputs()) {
                    // If it is a reference
                    if (StringUtils.isEmpty(input.getReference())) {

                        if (ProductMode.isCompatibleWithTaskTableMode(this.mode,
                                input.getMode())) {
                            final int currentOrder = 99;
                            List<JobOrderInput> inputsToAdd = new ArrayList<>();
                            for (final TaskTableInputAlternative alt : input
                                    .getAlternatives()) {
                                // We ignore input not DB
                                if (alt.getOrigin() == TaskTableInputOrigin.DB) {
                                    if (!CollectionUtils.isEmpty(job
                                            .getMetadataQueries()
                                            .get(alt.getIdSearchMetadataQuery())
                                            .getResult())) {

                                        JobOrderFileNameType type =
                                                JobOrderFileNameType.BLANK;
                                        switch (alt.getFileNameType()) {
                                            case PHYSICAL:
                                                type = JobOrderFileNameType.PHYSICAL;
                                                break;
                                            case DIRECTORY:
                                                type = JobOrderFileNameType.DIRECTORY;
                                                break;
                                            case REGEXP:
                                                type = JobOrderFileNameType.REGEXP;
                                                break;
                                            default:
                                                break;
                                        }

                                        // Retrieve family
                                        ProductFamily family =
                                                ProductFamily.fromValue(
                                                        this.ipfPreparationWorkerSettings
                                                                .getDefaultfamily());
                                        if (this.ipfPreparationWorkerSettings
                                                .getInputfamilies().containsKey(
                                                        alt.getFileType())) {
                                            family = this.ipfPreparationWorkerSettings
                                                    .getInputfamilies()
                                                    .get(alt.getFileType());
                                        }

                                        // Check order
                                        final List<JobOrderInputFile> jobOrderInputFiles =
                                                job.getMetadataQueries().get(alt
                                                        .getIdSearchMetadataQuery())
                                                        .getResult().stream()
                                                        .map(file -> new JobOrderInputFile(
                                                                file.getProductName(),
                                                                file.getKeyObjectStorage()))
                                                        .collect(Collectors
                                                                .toList());
                                        final List<JobOrderTimeInterval> jobOrderTimeIntervals =
                                                job.getMetadataQueries().get(alt
                                                        .getIdSearchMetadataQuery())
                                                        .getResult().stream()
                                                        .map(file -> new JobOrderTimeInterval(
                                                                DateUtils
                                                                        .convertToAnotherFormat(
                                                                                file.getValidityStart(),
                                                                                AbstractMetadata.METADATA_DATE_FORMATTER,
                                                                                JobOrderTimeInterval.DATE_FORMATTER),
                                                                DateUtils
                                                                        .convertToAnotherFormat(
                                                                                file.getValidityStop(),
                                                                                AbstractMetadata.METADATA_DATE_FORMATTER,
                                                                                JobOrderTimeInterval.DATE_FORMATTER),
                                                                file.getProductName()))
                                                        .collect(Collectors
                                                                .toList());

                                        if (currentOrder == alt.getOrder()) {

                                            inputsToAdd.add(new JobOrderInput(
                                                    alt.getFileType(), type,
                                                    jobOrderInputFiles,
                                                    jobOrderTimeIntervals,
                                                    family));
                                        } else if (currentOrder > alt
                                                .getOrder()) {
                                            inputsToAdd = new ArrayList<>();
                                            inputsToAdd.add(new JobOrderInput(
                                                    alt.getFileType(), type,
                                                    jobOrderInputFiles,
                                                    jobOrderTimeIntervals,
                                                    family));
                                        }
                                        break;
                                    }
                                } else {
                                    final DateTimeFormatter outFormatter =
                                            DateTimeFormatter.ofPattern(
                                                    "yyyyMMdd_HHmmssSSSSSS");
                                    final String startDate =
                                            DateUtils.convertToAnotherFormat(
                                                    job.getAppDataJob()
                                                            .getProduct()
                                                            .getStartTime(),
                                                    AppDataJobProduct.TIME_FORMATTER,
                                                    outFormatter);
                                    final String stopDate =
                                            DateUtils.convertToAnotherFormat(
                                                    job.getAppDataJob()
                                                            .getProduct()
                                                            .getStopTime(),
                                                    AppDataJobProduct.TIME_FORMATTER,
                                                    outFormatter);
                                    String filename = alt.getFileType();
                                    if (this.ipfPreparationWorkerSettings
                                            .getMapTypeMeta()
                                            .containsKey(alt.getFileType())) {
                                        filename = this.ipfPreparationWorkerSettings
                                                .getMapTypeMeta()
                                                .get(alt.getFileType());
                                    }
                                    inputsToAdd.add(new JobOrderInput(
                                            alt.getFileType(),
                                            JobOrderFileNameType.REGEXP,
                                            Arrays.asList(new JobOrderInputFile(
                                                    filename, "")),
                                            Arrays.asList(
                                                    new JobOrderTimeInterval(
                                                            startDate, stopDate,
                                                            filename,
                                                            outFormatter)),
                                            ProductFamily.BLANK));
                                }
                            }
                            if (!inputsToAdd.isEmpty()) {
                                // We take a random one
                                final int indexToTake = ThreadLocalRandom.current()
                                        .nextInt(0, inputsToAdd.size());
                                futureInputs.add(inputsToAdd.get(indexToTake));
                                if (!StringUtils.isEmpty(input.getId())) {
                                    referenceInputs.put(input.getId(),
                                            inputsToAdd.get(indexToTake));
                                }

                            } else {
                                if (input
                                        .getMandatory() == TaskTableMandatoryEnum.YES) {
                                    missingMetadata.put(input.toLogMessage(),
                                            "");
                                }
                            }
                        }
                    } else {
                        // We shall add inputs of the reference
                        if (referenceInputs.containsKey(input.getReference())) {
                            futureInputs.add(new JobOrderInput(
                                    referenceInputs.get(input.getReference())));
                        }
                    }
                }
                counterProc++;
                if (missingMetadata.isEmpty()) {
                    job.getJobOrder().getProcs().get(counterProc - 1)
                            .setInputs(futureInputs);
                } else {
                    throw new IpfPrepWorkerInputsMissingException(missingMetadata);
                }
            }
        }
    }

    protected String send(final JobGeneration job) throws AbstractCodedException {
        LOGGER.info("{} [productName {}] 3a - Building common job",
                this.prefixLogMonitor,
                job.getAppDataJob().getProduct().getProductName());
        final int inc = INCREMENT_JOB.incrementAndGet();
        final String workingDir = "/data/localWD/" + inc + "/";
        final String joborderName = "JobOrder." + inc + ".xml";
        final String jobOrder = workingDir +  joborderName;
        
        // Second, build the DTO


        // For each input and output of the job order, prefix by the working
        // directory
        job.getJobOrder().getProcs().stream()
                .filter(proc -> proc != null
                        && !CollectionUtils.isEmpty(proc.getInputs()))
                .flatMap(proc -> proc.getInputs().stream()).forEach(input -> {
                    input.getFilenames().forEach(filename -> {
                        filename.setFilename(
                                workingDir + filename.getFilename());
                    });
                    input.getTimeIntervals().forEach(interval -> {
                        interval.setFileName(
                                workingDir + interval.getFileName());
                    });
                });
        job.getJobOrder().getProcs().stream()
                .filter(proc -> proc != null
                        && !CollectionUtils.isEmpty(proc.getOutputs()))
                .flatMap(proc -> proc.getOutputs().stream()).forEach(output -> {
                    output.setFileName(workingDir + output.getFileName());
                });

        // Apply implementation build job
        job.getJobOrder().getConf()
                .setSensingTime(new JobOrderSensingTime(
                        DateUtils.convertToAnotherFormat(
                                job.getAppDataJob().getProduct().getStartTime(),
                                AppDataJobProduct.TIME_FORMATTER,
                                JobOrderSensingTime.DATETIME_FORMATTER),

                        DateUtils.convertToAnotherFormat(
                                job.getAppDataJob().getProduct().getStopTime(),
                                AppDataJobProduct.TIME_FORMATTER,
                                JobOrderSensingTime.DATETIME_FORMATTER)));

        // Custom Job order according implementation
        this.customJobOrder(job);


        ProductFamily family = ProductFamily.L0_JOB;
        switch (l0ProcessSettings.getLevel()) {
            case L0:
                family = ProductFamily.L0_JOB;
                break;
            case L0_SEGMENT:
                family = ProductFamily.L0_SEGMENT_JOB;
                break;
            case L1:
                family = ProductFamily.L1_JOB;
                break;
            case L2:
                family = ProductFamily.L2_JOB;
                break;
        }
        final IpfExecutionJob r = new IpfExecutionJob(family,
                job.getAppDataJob().getProduct().getProductName(),
                job.getAppDataJob().getProduct().getProcessMode(), workingDir,
                jobOrder, job.getGeneration().getReportingTaskUID());
        
        r.setCreationDate(new Date());
        r.setHostname(hostname);

        try {

            // Add jobOrder inputs to the DTO
            final List<JobOrderInput> distinctInputJobOrder = job.getJobOrder()
                    .getProcs().stream()
                    .filter(proc -> proc != null
                            && !CollectionUtils.isEmpty(proc.getInputs()))
                    .flatMap(proc -> proc.getInputs().stream()).distinct()
                    .collect(Collectors.toList());
            distinctInputJobOrder.forEach(input -> {
                for (final JobOrderInputFile file : input.getFilenames()) {
                    r.addInput(new LevelJobInputDto(input.getFamily().name(),
                            file.getFilename(), file.getKeyObjectStorage()));
                }
            });

            final String jobOrderXml = xmlConverter
            .convertFromObjectToXMLString(job.getJobOrder());
            
            LOGGER.trace("Adding input JobOrderXml '{}' for product '{}'",
            		jobOrderXml, job.getAppDataJob().getProduct().getProductName());
            
            // Add the jobOrder itself in inputs
            r.addInput(new LevelJobInputDto(ProductFamily.JOB_ORDER.name(),
                    jobOrder, jobOrderXml));

            // Add joborder output to the DTO
            final List<JobOrderOutput> distinctOutputJobOrder = job.getJobOrder().getProcs().stream()
                    .filter(proc -> proc != null && !CollectionUtils.isEmpty(proc.getOutputs()))
                    .flatMap(proc -> proc.getOutputs().stream())
                    .filter(output -> output.getFileNameType() == JobOrderFileNameType.REGEXP && output.getDestination() == JobOrderDestination.DB)
                    .distinct()
                    .collect(Collectors.toList());
            
            r.addOutputs(distinctOutputJobOrder.stream()
                    .map(output -> new LevelJobOutputDto(
                            output.getFamily().name(), output.getFileName()))
                    .collect(Collectors.toList()));
            
            final List<JobOrderOutput> distinctOutputJobOrderNotRegexp = job.getJobOrder().getProcs().stream()
                    .filter(proc -> proc != null && !CollectionUtils.isEmpty(proc.getOutputs()))
                    .flatMap(proc -> proc.getOutputs().stream())
                    .filter(output -> output.getFileNameType() == JobOrderFileNameType.DIRECTORY && output.getDestination() == JobOrderDestination.DB)
                    .distinct()
                    .collect(Collectors.toList());
            
            r.addOutputs(distinctOutputJobOrderNotRegexp
                    .stream().map(
                            output -> new LevelJobOutputDto(
                                    output.getFamily().name(),
                                    output.getFileName() + "^.*"
                                            + output.getFileType() + ".*$"))
                    .collect(Collectors.toList()));

            for (final LevelJobOutputDto output: r.getOutputs()) {
            	// Iterate over the outputs and identify if an OQC check is required
            	final ProductFamily outputFamily = ProductFamily.valueOf(output.getFamily());
            	if (this.ipfPreparationWorkerSettings.getOqcCheck().contains(outputFamily)) {
            		// Hit, we found a product family that had been configured as oqc check. Flag it.
            		LOGGER.info("Found output of family {}, flagging it as oqcCheck",outputFamily);
            		output.setOqcCheck(true);
            	} else {
            		// No hit
            		LOGGER.debug("Found output of family {}, no oqcCheck required",outputFamily);
            	}
            }
            
            // Add the tasks
            this.tasks.forEach(pool -> {
                final LevelJobPoolDto poolDto = new LevelJobPoolDto();
                pool.forEach(task -> {
                    poolDto.addTask(new LevelJobTaskDto(task));
                });
                r.addPool(poolDto);
            });

            // Apply implementation build job
            LOGGER.info("{} [productName {}] 3b - Building custom job",
                    this.prefixLogMonitor,
                    job.getAppDataJob().getProduct().getProductName());
            this.customJobDto(job, r);

        } catch (IOException | JAXBException e) {
            throw new InternalErrorException("Cannot send the job", e);
        }

        // Third, send the job
        LOGGER.info("{} [productName {}] 3c - Publishing job",
                this.prefixLogMonitor,
                job.getAppDataJob().getProduct().getProductName());
        
		@SuppressWarnings("unchecked")
		final AppDataJob<CatalogEvent> dto = job.getAppDataJob();

        this.outputFactory.sendJob(dto.getMessages().get(0), r);
        
        return joborderName;
    }

    protected abstract void customJobOrder(JobGeneration job);

    protected abstract void customJobDto(JobGeneration job, IpfExecutionJob dto);
    
    // S1PRO-707
    static final String getPolarisationFor(final AppDataJobProduct product) {
    	final String polarisation = product.getPolarisation().toUpperCase();
    	if (polarisation.equals("SV") || polarisation.equals("DV")) {
    		return "V";    		
    	}
    	else if (polarisation.equals("SH") || polarisation.equals("DH")) {
    		return "H";
    	}
    	return "NONE";    	
    }
    
    private final String getTypeForParameterName(final String name) {    			
    	for (final TaskTableDynProcParam param : taskTable.getDynProcParams()) {
    		if (param.getName().equals(name)) {
    			return param.getType();
    		}    		
    	}
    	return "String";
    }
    
    private final String mapTasktableTypeToReportingType(final String type) {
    	if ("number".equalsIgnoreCase(type)) {
    		return "_double";
    	} else if ("datenumber".equalsIgnoreCase(type)) {
    		return "_date";
    	}
    	return "_string";
    }
}
