package esa.s1pdgs.cpoc.preparation.worker.config;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import esa.s1pdgs.cpoc.common.CommonConfigurationProperties;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.preparation.worker.service.AppCatJobService;
import esa.s1pdgs.cpoc.preparation.worker.service.HousekeepingService;
import esa.s1pdgs.cpoc.preparation.worker.service.InputSearchService;
import esa.s1pdgs.cpoc.preparation.worker.service.PreparationWorkerService;
import esa.s1pdgs.cpoc.preparation.worker.service.TaskTableMapperService;
import esa.s1pdgs.cpoc.preparation.worker.type.ProductTypeAdapter;

/**
 * Configuration class containing the interface for Spring Cloud Dataflow.
 */
@Configuration
public class PreparationWorkerServiceConfiguration {

	@Autowired
	private CommonConfigurationProperties commonProperties;

	@Autowired
	private ProcessProperties processProperties;

	@Autowired
	private TaskTableMapperService taskTableMapperService;

	@Autowired
	private AppCatJobService appCatJobService;

	@Autowired
	private ProductTypeAdapter typeAdapter;

	@Autowired
	private InputSearchService inputSearchService;

	@Autowired
	private PreparationWorkerProperties preparationWorkerProperties;

	@Bean
	public Function<CatalogEvent, List<Message<IpfExecutionJob>>> prepareExecutionJobs() {
		return new PreparationWorkerService(taskTableMapperService, typeAdapter, processProperties, appCatJobService,
				inputSearchService, commonProperties, preparationWorkerProperties);
	}

	@Bean
	public Function<Message<?>, List<Message<IpfExecutionJob>>> houseKeepAppDataJobs() {
		return new HousekeepingService(appCatJobService, inputSearchService, preparationWorkerProperties,
				commonProperties);
	}
}
