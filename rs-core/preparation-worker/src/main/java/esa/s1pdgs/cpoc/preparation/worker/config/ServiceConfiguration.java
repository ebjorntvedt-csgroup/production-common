package esa.s1pdgs.cpoc.preparation.worker.config;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import esa.s1pdgs.cpoc.common.CommonConfigurationProperties;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.preparation.worker.config.PreparationWorkerProperties.InputWaitingConfig;
import esa.s1pdgs.cpoc.preparation.worker.db.AppDataJobRepository;
import esa.s1pdgs.cpoc.preparation.worker.db.SequenceDao;
import esa.s1pdgs.cpoc.preparation.worker.model.joborder.JobOrderAdapter;
import esa.s1pdgs.cpoc.preparation.worker.query.AuxQueryHandler;
import esa.s1pdgs.cpoc.preparation.worker.service.AppCatJobService;
import esa.s1pdgs.cpoc.preparation.worker.service.InputSearchService;
import esa.s1pdgs.cpoc.preparation.worker.service.JobCreationService;
import esa.s1pdgs.cpoc.preparation.worker.service.TaskTableMapperService;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.adapter.ElementMapper;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.adapter.TaskTableAdapter;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.adapter.TaskTableFactory;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.adapter.TasktableManager;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.mapper.ConfigurableKeyEvaluator;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.mapper.RoutingBasedTasktableMapper;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.mapper.TasktableMapper;
import esa.s1pdgs.cpoc.preparation.worker.timeout.InputTimeoutChecker;
import esa.s1pdgs.cpoc.preparation.worker.timeout.InputTimeoutCheckerImpl;
import esa.s1pdgs.cpoc.preparation.worker.type.ProductTypeAdapter;
import esa.s1pdgs.cpoc.xml.XmlConverter;
import esa.s1pdgs.cpoc.xml.model.tasktable.TaskTable;

@Configuration
public class ServiceConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceConfiguration.class);

	@Bean
	@Autowired
	public TasktableMapper newTastTableMapper(final TaskTableMappingProperties properties) {
		return new RoutingBasedTasktableMapper.Factory(properties.getRouting(),
				new ConfigurableKeyEvaluator(properties.getRoutingKeyTemplate())).newMapper();
	}

	@Bean
	@Autowired
	public TaskTableMapperService taskTableMapperService(final TasktableMapper ttMapper,
			final ProcessProperties processProperties, final Map<String, TaskTableAdapter> ttAdapters) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
		return new TaskTableMapperService(ttMapper, processProperties, ttAdapters);
	}

	@Bean
	@Autowired
	public AppCatJobService appCatJobService(final AppDataJobRepository repository, final SequenceDao sequenceDao,
			final ProcessProperties processSettings) {
		LOG.info("Create new AppCatJobService with {} and {}", repository.toString(), sequenceDao.toString());
		return new AppCatJobService(repository, sequenceDao, processSettings);
	}

	@Bean
	@Autowired
	public TasktableManager tasktableManager(final PreparationWorkerProperties settings) {
		return TasktableManager.of(new File(settings.getDiroftasktables()));
	}

	@Bean
	@Autowired
	public Map<String, TaskTableAdapter> taskTableAdapters(final ProcessProperties processSettings,
			final ElementMapper elementMapper, final TaskTableFactory taskTableFactory,
			final PreparationWorkerProperties settings, final TasktableManager ttManager) {
		Map<String, TaskTableAdapter> ttAdapters = new HashMap<>();

		for (File taskTableFile : ttManager.tasktables()) {
			LOG.debug("Loading tasktable {}", taskTableFile.getAbsolutePath());
			ttAdapters.put(taskTableFile.getName(),
					new TaskTableAdapter(taskTableFile,
							taskTableFactory.buildTaskTable(taskTableFile, processSettings.getLevel()), elementMapper,
							settings.getProductMode()));
		}

		return ttAdapters;
	}

	@Bean
	@Autowired
	public AuxQueryHandler auxQueryHandler(final MetadataClient metadataClient,
			final PreparationWorkerProperties settings, final Function<TaskTable, InputTimeoutChecker> timeoutChecker) {
		return new AuxQueryHandler(metadataClient, settings.getProductMode(), timeoutChecker);
	}

	@Bean
	@Autowired
	public InputSearchService inputSearchService(final ProductTypeAdapter typeAdapter,
			final AuxQueryHandler auxQueryHandler, final Map<String, TaskTableAdapter> taskTableAdapters,
			final AppCatJobService appCatJobService, final JobCreationService jobCreationService) {
		return new InputSearchService(typeAdapter, auxQueryHandler, taskTableAdapters, appCatJobService,
				jobCreationService);
	}

	@Bean
	@Autowired
	public JobCreationService publisher(final CommonConfigurationProperties commonProperties,
			final PreparationWorkerProperties settings, final ProcessProperties processSettings,
			final ProductTypeAdapter typeAdapter, final ElementMapper elementMapper, final XmlConverter xmlConverter) {
		final JobOrderAdapter.Factory jobOrderFactory = new JobOrderAdapter.Factory(
				(tasktableAdapter) -> tasktableAdapter.newJobOrder(processSettings, settings.getProductMode()),
				typeAdapter, elementMapper, xmlConverter);

		return new JobCreationService(commonProperties, settings, processSettings, jobOrderFactory, typeAdapter);
	}
	
	@Bean
	@Autowired
	public Function<TaskTable, InputTimeoutChecker> timeoutCheckerFor(final PreparationWorkerProperties workerProperties) {
		return (taskTable) -> {
			final List<InputWaitingConfig> configsForTaskTable = new ArrayList<>();
			for (final InputWaitingConfig config : workerProperties.getInputWaiting().values()) {
				if (taskTable.getProcessorName().matches(config.getProcessorNameRegexp()) &&
					taskTable.getVersion().matches(config.getProcessorVersionRegexp())) 
				{			
					configsForTaskTable.add(config);
				}					
			}
			// default: always time out
			return new InputTimeoutCheckerImpl(configsForTaskTable, LocalDateTime::now);
		};
	}
}
