package esa.s1pdgs.cpoc.ipf.preparation.worker.service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.generator.JobsGeneratorFactory;
import esa.s1pdgs.cpoc.ipf.preparation.worker.generator.JobsGeneratorFactory.JobGenType;

/**
 * Dispatcher of EdrsSession product<br/>
 * Only one task table
 * 
 * @author Cyrielle Gailliard
 */
public class L0AppJobDispatcher extends AbstractJobsDispatcher {
    private static final String TASK_TABLE_NAME = "TaskTable.AIOP.xml";

    public L0AppJobDispatcher(
    		final IpfPreparationWorkerSettings settings,
            final ProcessSettings processSettings,
            final JobsGeneratorFactory factory,
            final ThreadPoolTaskScheduler taskScheduler,
            final AppCatalogJobClient appDataService
            ) {
        super(settings, processSettings, factory, taskScheduler, appDataService);
    }

    @PostConstruct
    public void initialize() throws Exception {
        // Init job generators from task tables
        super.initTaskTables();
    }

    @Override
    protected AbstractJobsGenerator createJobGenerator(
            final File xmlFile
    ) throws AbstractCodedException {
        return factory.newJobGenerator(xmlFile, appDataService, JobGenType.LEVEL_0);
    }

    @Override
    protected List<String> getTaskTables(final AppDataJob job) {
        return Arrays.asList(TASK_TABLE_NAME);
    }
}
