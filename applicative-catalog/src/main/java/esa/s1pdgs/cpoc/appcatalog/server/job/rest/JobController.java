package esa.s1pdgs.cpoc.appcatalog.server.job.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobDto;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobGenerationDto;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobGenerationDtoState;
import esa.s1pdgs.cpoc.appcatalog.server.job.converter.JobConverter;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobService;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobGenerationInvalidStateException;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobGenerationInvalidTransitionStateException;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobGenerationNotFoundException;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobGenerationTerminatedException;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobInvalidStateException;
import esa.s1pdgs.cpoc.appcatalog.server.job.exception.AppCatalogJobNotFoundException;
import esa.s1pdgs.cpoc.appcatalog.server.job.rest.JobControllerConfiguration.Generations;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.filter.FilterCriterion;
import esa.s1pdgs.cpoc.common.filter.FilterUtils;
import esa.s1pdgs.cpoc.common.utils.DateUtils;

/**
 * @author Viveris Technologies
 */
@RestController
@EnableConfigurationProperties(JobControllerConfiguration.class)
public class JobController {
    private static final Logger LOGGER = LogManager.getLogger(JobController.class);

    private final AppDataJobService appDataJobService;

    private final JobConverter jobConverter;

    private final static String PK_ORDER_BY_ASC = "[orderByAsc]";
    private final static String PK_ORDER_BY_DESC = "[orderByDesc]";

    private final static String PK_ID = "_id";
    private final static String PK_CREATION = "creationDate";
    private final static String PK_UPDATE = "lastUpdateDate";
    private final static String PK_MESSAGES_ID = "messages.identifier";
    private final static String PK_PRODUCT_START = "product.startTime";
    private final static String PK_PRODUCT_STOP = "product.stopTime";
    
    private final JobControllerConfiguration config;

    /**
     * Constructor
     * 
     * @param appDataJobService
     */
    public JobController(
    		final AppDataJobService appDataJobService,
            final JobConverter jobConverter,
            final JobControllerConfiguration config
    ) {
        this.appDataJobService = appDataJobService;
        this.jobConverter = jobConverter;
        this.config = config;
    }

    /**
     * Search for jobs
     * 
     * @param params
     * @return
     * @throws AppCatalogJobNotFoundException
     * @throws AppCatalogJobInvalidStateException
     * @throws AppCatalogJobGenerationInvalidStateException
     * @throws InternalErrorException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{category}/jobs/search")
    public List<AppDataJobDto> search(
    		@PathVariable(name = "category") final String categoryName,
            @RequestParam Map<String, String> params)
            throws AppCatalogJobNotFoundException,
            AppCatalogJobInvalidStateException,
            AppCatalogJobGenerationInvalidStateException,
            InternalErrorException {
        // Extract criterion
    	final ProductCategory category = ProductCategory.valueOf(categoryName.toUpperCase());
        List<FilterCriterion> filters = new ArrayList<>();
        Sort sort = null;
        for (String keyFilter : params.keySet()) {
            String valueFilter = params.get(keyFilter);
            switch (keyFilter) {
                case PK_ORDER_BY_ASC:
                    sort = new Sort(Direction.ASC, valueFilter);
                    break;
                case PK_ORDER_BY_DESC:
                    sort = new Sort(Direction.DESC, valueFilter);
                    break;
                default:
                    FilterCriterion criterion = FilterUtils
                            .extractCriterion(keyFilter, valueFilter);
                    switch (criterion.getKey()) {
                        case PK_ID:
                            criterion.setValue(Long.decode(valueFilter));
                            break;
                        case PK_MESSAGES_ID:
                            criterion.setValue(Long.decode(valueFilter));
                            break;
                        case PK_CREATION:
                        case PK_UPDATE:
                            criterion.setValue(
                                    DateUtils.convertDateIso(valueFilter));
                            break;
                        case PK_PRODUCT_START:
                        case PK_PRODUCT_STOP:
                            criterion.setValue(
                                    DateUtils.convertDateIso(valueFilter));
                            break;
                    }
                    filters.add(criterion);
                    break;
            }
        }
        // Search
        List<AppDataJob> jobsDb = appDataJobService.search(filters, category, sort);
        // Convert into DTO
        List<AppDataJobDto> jobsDto = new ArrayList<>();
        for (AppDataJob jobDb : jobsDb) {
            jobsDto.add(jobConverter.convertJobFromDbToDto(jobDb, category));
        }
        return jobsDto;
    }

    /**
     * Get one job
     * 
     * @param jobId
     * @return
     * @throws AppCatalogJobNotFoundException
     * @throws AppCatalogJobGenerationInvalidStateException
     * @throws AppCatalogJobInvalidStateException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{category}/jobs/{jobId}")
    public AppDataJobDto one(
    		@PathVariable(name = "category") final String categoryName,
    		@PathVariable(name = "jobId") final Long jobId)
            throws AppCatalogJobNotFoundException,
            AppCatalogJobInvalidStateException,
            AppCatalogJobGenerationInvalidStateException {
    	final ProductCategory category = ProductCategory.valueOf(categoryName.toUpperCase());
        return jobConverter
                .convertJobFromDbToDto(appDataJobService.getJob(jobId), category);
    }

    /**
     * Create a job
     * 
     * @param newJob
     * @return
     * @throws AppCatalogJobInvalidStateException
     * @throws AppCatalogJobGenerationInvalidStateException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{category}/jobs")
    public AppDataJobDto newJob(
    		@PathVariable(name = "category") final String categoryName,
    		@RequestBody final AppDataJobDto newJob)
            throws AppCatalogJobInvalidStateException,
            AppCatalogJobGenerationInvalidStateException {
    	final ProductCategory category = ProductCategory.valueOf(categoryName.toUpperCase());
        // Convert into database message
        AppDataJob newJobDb = jobConverter.convertJobFromDtoToDb(newJob, category);

        // Create it
        return jobConverter
                .convertJobFromDbToDto(appDataJobService.newJob(newJobDb),category);
    }

    /**
     * Delete a job
     * 
     * @param jobId
     */
    @DeleteMapping("/{jobId}")
    public void deleteJob(@PathVariable final Long jobId) {
        appDataJobService.deleteJob(jobId);
    }

    /**
     * Patch a job
     * 
     * @param jobId
     * @param patchJob
     * @return
     * @throws AppCatalogJobInvalidStateException
     * @throws AppCatalogJobNotFoundException
     * @throws AppCatalogJobGenerationInvalidStateException
     */
    @RequestMapping(method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{category}/jobs/{jobId}")
    public AppDataJobDto patchJob(
    		@PathVariable(name = "category") final String categoryName,
            @PathVariable(name = "jobId") final Long jobId,
            @RequestBody final AppDataJobDto patchJob)
            throws AppCatalogJobInvalidStateException,
            AppCatalogJobNotFoundException,
            AppCatalogJobGenerationInvalidStateException {
    	final ProductCategory category = ProductCategory.valueOf(categoryName.toUpperCase());
        return jobConverter.convertJobFromDbToDto(appDataJobService
                .patchJob(jobId, jobConverter.convertJobFromDtoToDb(patchJob, category)), category);
    }

    /**
     * Set messages of a given job
     * 
     * @param jobId
     * @param messages
     * @return
     * @throws AppCatalogJobInvalidStateException
     * @throws AppCatalogJobGenerationInvalidStateException
     * @throws AppCatalogJobNotFoundException
     * @throws AppCatalogJobGenerationNotFoundException
     * @throws AppCatalogJobGenerationInvalidTransitionStateException
     */
    @RequestMapping(method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{category}/jobs/{jobId}/generations/{taskTable}")
    public AppDataJobDto patchGenerationOfJob(
    		@PathVariable(name = "category") final String categoryName,
            @PathVariable(name = "jobId") final Long jobId,
            @PathVariable(name = "taskTable") final String taskTable,
            @RequestBody final AppDataJobGenerationDto generation)
            throws AppCatalogJobInvalidStateException,
            AppCatalogJobGenerationInvalidStateException,
            AppCatalogJobNotFoundException,
            AppCatalogJobGenerationInvalidTransitionStateException,
            AppCatalogJobGenerationNotFoundException {
    	final ProductCategory category = ProductCategory.valueOf(categoryName.toUpperCase());
        AppDataJobDto ret = null;
        try {
            ret = jobConverter.convertJobFromDbToDto(
                    appDataJobService.patchGenerationToJob(jobId, taskTable,
                            jobConverter.convertJobGenerationFromDtoToDb(
                                    generation),
                            getFor(category, generation.getState())), category);
        } catch (AppCatalogJobGenerationTerminatedException e) {
            LOGGER.error("[jobId {}] [taskTable {}] [code {}] {}", jobId,
                    taskTable, e.getCode().getCode(), e.getLogMessage());
            // TODO publish error message in kafka
        }
        return ret;
    }
    
    private final int getFor(ProductCategory category, AppDataJobGenerationDtoState state)
    {
    	try {
			final Generations gen = config.getFor(category);
			if (state == AppDataJobGenerationDtoState.INITIAL)
			{
				return gen.getMaxErrorsInitial();
			}
			return gen.getMaxErrorsPrimaryCheck();
		} catch (Exception e) {
			// don't fail here but return some sane default.
	    	return 300;
		}
    }
}
