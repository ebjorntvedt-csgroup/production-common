package esa.s1pdgs.cpoc.ipf.preparation.worker.config;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.ipf.preparation.worker.appcat.AppCatJobService;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings.CategoryConfig;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings.InputWaitingConfig;
import esa.s1pdgs.cpoc.ipf.preparation.worker.dispatch.JobDispatcherImpl;
import esa.s1pdgs.cpoc.ipf.preparation.worker.generator.JobGenerator;
import esa.s1pdgs.cpoc.ipf.preparation.worker.generator.JobGeneratorImpl;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.ProductMode;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.ElementMapper;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableFactory;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TasktableManager;
import esa.s1pdgs.cpoc.ipf.preparation.worker.publish.Publisher;
import esa.s1pdgs.cpoc.ipf.preparation.worker.query.AuxQueryHandler;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.IpfPreparationService;
import esa.s1pdgs.cpoc.ipf.preparation.worker.status.AppStatusImpl;
import esa.s1pdgs.cpoc.ipf.preparation.worker.timeout.InputTimeoutChecker;
import esa.s1pdgs.cpoc.ipf.preparation.worker.timeout.InputTimeoutCheckerImpl;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.ProductTypeAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.edrs.AiopPropertiesAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.edrs.EdrsSession;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.segment.L0Segment;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.slice.LevelProduct;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.mqi.client.MessageFilter;
import esa.s1pdgs.cpoc.mqi.client.MqiClient;
import esa.s1pdgs.cpoc.mqi.client.MqiConsumer;
import esa.s1pdgs.cpoc.mqi.client.MqiListener;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.xml.XmlConverter;
import esa.s1pdgs.cpoc.xml.model.tasktable.TaskTable;

@Configuration
public class IpfPreparationWorkerConfiguration {
	private static final Logger LOG = LogManager.getLogger(IpfPreparationWorkerConfiguration.class);
	

    private final AppStatusImpl appStatus;
    private final MqiClient mqiClient;
    private final List<MessageFilter> messageFilter;
    private final IpfPreparationWorkerSettings settings;
    private final ErrorRepoAppender errorAppender;
    private final ProcessConfiguration processConfiguration;
	private final ProcessSettings processSettings;
    private final MetadataClient metadataClient;
    private final AiopProperties aiopProperties;
	private final TaskTableFactory taskTableFactory;
	private final ElementMapper elementMapper;
    private final Publisher publisher;
    private final AppCatJobService appCatService;
    
	@Autowired
	public IpfPreparationWorkerConfiguration(
			final AppStatusImpl appStatus, 
			final MqiClient mqiClient,
			final List<MessageFilter> messageFilter,
			final IpfPreparationWorkerSettings settings, 
			final ErrorRepoAppender errorAppender,
			final ProcessConfiguration processConfiguration,
			final ProcessSettings processSettings,
		    final MetadataClient metadataClient,
		    final AiopProperties aiopProperties,
		    final XmlConverter xmlConverter,
			final TaskTableFactory taskTableFactory,
			final ElementMapper elementMapper,
		    final Publisher publisher,
		    final AppCatJobService appCatService
	) {
		this.appStatus = appStatus;
		this.mqiClient = mqiClient;
		this.messageFilter = messageFilter;
		this.settings = settings;
		this.errorAppender = errorAppender;
		this.processConfiguration = processConfiguration;
		this.processSettings = processSettings;
		this.metadataClient = metadataClient;
		this.aiopProperties = aiopProperties;
		this.taskTableFactory = taskTableFactory;
		this.elementMapper = elementMapper;
		this.publisher = publisher;
		this.appCatService = appCatService;
	}

	@Bean
	public Function<TaskTable, InputTimeoutChecker> timeoutCheckerFor() {
		return this::inputWaitTimeoutFor;
	}
	
	@Bean
	public TasktableManager tasktableManager() {
		return TasktableManager.of(new File(settings.getDiroftasktables()));
	}
		
	@Bean(name="jobGenerationTaskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(final TasktableManager ttManager) {
        final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(ttManager.size());
        threadPoolTaskScheduler.setThreadNamePrefix("JobGenerationTaskScheduler");
        return threadPoolTaskScheduler;
    }
	
//	@Bean
//	@Autowired
//	public AbstractJobsDispatcher jobsDispatcher(
//			final ProcessSettings processSettings,
//			final JobsGeneratorFactory factory, 
//			final ThreadPoolTaskScheduler taskScheduler,
//			final XmlConverter xmlConverter,
//			@Value("${level-products.pathroutingxmlfile}") final String pathRoutingXmlFile,
//		
//	) {
//		switch (processSettings.getLevel()) {
//			case L0:
//				return new L0AppJobDispatcher(settings, processSettings, factory, taskScheduler, appCatClient);
//			case L0_SEGMENT:
//				return new L0SegmentAppJobDispatcher(settings, processSettings, factory, taskScheduler, appCatClient);
//			case L1:
//			case L2:
//				return new LevelProductsJobDispatcher(settings, processSettings, factory, taskScheduler, xmlConverter, 
//						pathRoutingXmlFile, appCatClient);
//			default:
//				// fall through to throw exception
//		}
//		throw new IllegalArgumentException(
//				String.format(
//						"Unsupported Application Level '%s'. Available are: %s", 
//						processSettings.getLevel(),
//						Arrays.asList(ApplicationLevel.values())
//				)
//		);
//	}
	
	@Bean
	@Autowired
	public ProductTypeAdapter typeAdapter(
			@Value("${level-products.pathroutingxmlfile}") final String pathRoutingXmlFile
	) {
		if (processSettings.getLevel() == ApplicationLevel.L0) {
			return new EdrsSession(
					metadataClient, 
					AiopPropertiesAdapter.of(aiopProperties)
			);
		}
		else if (processSettings.getLevel() == ApplicationLevel.L0_SEGMENT) {
			final long timeoutInputSearchMs = settings.getWaitprimarycheck().getMaxTimelifeS() * 1000L;
			
			return new L0Segment(
					metadataClient, 
					timeoutInputSearchMs
			);			
		}
		else if (EnumSet.of(ApplicationLevel.L1, ApplicationLevel.L2).contains(processSettings.getLevel())) {
			final Map<String, Float> sliceOverlap = settings.getTypeOverlap();
			final Map<String, Float> sliceLength = settings.getTypeSliceLength();
	
			return new LevelProduct(
					metadataClient, 
					sliceOverlap, 
					sliceLength
			);			
		}
		throw new IllegalArgumentException(
				String.format(
						"Unsupported Application Level '%s'. Available are: %s", 
						processSettings.getLevel(),
						Arrays.asList(
								ApplicationLevel.L0, 
								ApplicationLevel.L0_SEGMENT, 
								ApplicationLevel.L1, 
								ApplicationLevel.L2
						)
				)
		);
		
	}
	
	@Bean
	@Autowired
	public IpfPreparationService service(
			final TasktableManager ttManager, 
			final ThreadPoolTaskScheduler taskScheduler,
			final ProductTypeAdapter typeAdapter,
			final Function<TaskTable, InputTimeoutChecker> timeoutCheckerFactory
	) {		
		final Map<String, JobGenerator> generators = new HashMap<>(ttManager.size());	
		//final AppCatAdapter appCat = new AppCatAdapter(appCatClient, gracePeriodHandler);
	
		for (final File taskTableFile : ttManager.tasktables()) {				
			final JobGenerator jobGenerator = newJobGenerator(taskTableFile, typeAdapter, timeoutCheckerFactory);
			generators.put(taskTableFile.getName(), jobGenerator);
		    // --> Launch generators
			taskScheduler.scheduleWithFixedDelay(jobGenerator, settings.getJobgenfixedrate());
		}
		
		final IpfPreparationService service = new IpfPreparationService(
				new JobDispatcherImpl(
						typeAdapter, 
						processSettings, 
						appCatService, 
						generators.keySet()
				), 
				errorAppender, 
				processConfiguration
		);
		
		// TODO add ThreadFactory to give a descriptive name for the consumer
		final ExecutorService executor = Executors.newFixedThreadPool(
				settings.getProductCategories().size()
		);
		
		for (final Map.Entry<ProductCategory, CategoryConfig> entry : settings.getProductCategories().entrySet()) {				
			executor.execute(newConsumerFor(entry.getKey(), entry.getValue(), service));
		}		
		return service;
	}
	
	private JobGenerator newJobGenerator(
			final File taskTableFile, 
			final ProductTypeAdapter typeAdapter,
			final Function<TaskTable, InputTimeoutChecker> timeoutCheckerFactory
	) {		
		final TaskTableAdapter tasktableAdapter = new TaskTableAdapter(
				taskTableFile, 
				taskTableFactory.buildTaskTable(taskTableFile, processSettings.getLevel()), 
				elementMapper
		);			    
	    final List<List<String>> tasks = tasktableAdapter.buildTasks();
		final AuxQueryHandler auxQueryHandler = new AuxQueryHandler(
				metadataClient, 
				ProductMode.SLICING, //TODO clarify why mode is always slicing
				timeoutCheckerFactory.apply(tasktableAdapter.taskTable()),
				elementMapper);
		return new JobGeneratorImpl(
				tasktableAdapter,
				typeAdapter, 
				appCatService, 
				processSettings, 
				errorAppender, 
				publisher, 
				tasks,
				auxQueryHandler
		);
	}
		
	private MqiConsumer<IpfPreparationJob> newConsumerFor(
			final ProductCategory category, 
			final CategoryConfig config,
			final MqiListener<IpfPreparationJob> listener
			
	) {
		LOG.debug("Creating MQI consumer for category {} using {}", category, config);
		return new MqiConsumer<>(
				mqiClient,
				category,
				listener,
				messageFilter,
				config.getFixedDelayMs(),
				config.getInitDelayPollMs(),
				appStatus
		);
	}	

	private InputTimeoutChecker inputWaitTimeoutFor(final TaskTable taskTable) {
		final List<InputWaitingConfig> configsForTaskTable = new ArrayList<>();
		for (final InputWaitingConfig config : settings.getInputWaiting()) {
			if (taskTable.getProcessorName().equals(config.getProcessorNameRegexp()) &&
				taskTable.getVersion().matches(config.getProcessorVersionRegexp())) 
			{			
				configsForTaskTable.add(config);
			}					
		}
		// default: always time out
		return new InputTimeoutCheckerImpl(configsForTaskTable, LocalDateTime::now);
	}
}
