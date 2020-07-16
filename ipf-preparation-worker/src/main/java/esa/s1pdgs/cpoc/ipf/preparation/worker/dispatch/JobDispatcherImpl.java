package esa.s1pdgs.cpoc.ipf.preparation.worker.dispatch;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGeneration;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProductAdapter;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.ipf.preparation.worker.appcat.AppCatAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.appcat.CatalogEventAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.report.TaskTableLookupReportingOutput;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.ProductTypeAdapter;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

/**
 * Job dispatcher<br/>
 * 
 * When a message is read and can be processing, it will be dispatch to one or
 * several task tables according the product category.
 * 
 * @param <T>
 */
public class JobDispatcherImpl implements JobDispatcher {
    private final AppCatAdapter appCat;    
    private final ProcessSettings settings;
    private final ProductTypeAdapter typeAdapter;
    private final Collection<String> generatorAvailableForTasktableNames;

    public JobDispatcherImpl(
    		final ProductTypeAdapter typeAdapter,
    		final ProcessSettings settings,
            final AppCatAdapter appCat,         
            final Collection<String> generatorAvailableForTasktableNames
    ) {
        this.appCat = appCat;
        this.generatorAvailableForTasktableNames = generatorAvailableForTasktableNames;
        this.settings = settings;
        this.typeAdapter = typeAdapter;
    }

	@Override
	public final void dispatch(final GenericMessageDto<IpfPreparationJob> message) throws Exception {
    	final IpfPreparationJob prepJob = message.getBody();
    	final AppDataJob jobFromMessage = prepJob.getAppDataJob();    	
    	final GenericMessageDto<CatalogEvent> firstMessage = jobFromMessage.getMessages().get(0);
    	
    	LOGGER.trace("== dispatch job {}", jobFromMessage.toString());
        
        final Reporting reporting = ReportingUtils.newReportingBuilder()
        		.predecessor(prepJob.getUid())
        		.newReporting("TaskTableLookup");
        
        typeAdapter.customAppDataJobProduct(jobFromMessage.getProduct());        
        final AppDataJobProductAdapter productAdapter = new AppDataJobProductAdapter(jobFromMessage.getProduct());
        
        
    	reporting.begin(
    			ReportingUtils.newFilenameReportingInputFor(prepJob.getProductFamily(), jobFromMessage.getProduct().getProductName()),
    			new ReportingMessage("Start associating TaskTables to AppDataJob", jobFromMessage.getId())
    	);    	
        try {        	
            final String tasktableFilename = 
            		
            		
            		typeAdapter.taskTableMapper().tasktableFor(jobFromMessage);
            LOGGER.trace("Got TaskTable {}", tasktableFilename);
            
            // assert that there is a job generator for the assigned tasktable
            if (!generatorAvailableForTasktableNames.contains(tasktableFilename))  {
            	throw new IllegalStateException(
            			String.format(
            					"No job generator found for tasktable %s. Available are: %s", 
            					tasktableFilename,
            					generatorAvailableForTasktableNames
            			)
            	);
            }       
            
    		final Optional<AppDataJob> jobForMess = appCat.findJobFor(firstMessage); 
         	final CatalogEventAdapter eventAdapter = CatalogEventAdapter.of(firstMessage);
         	final Optional<AppDataJob> specificJob = typeAdapter.findAssociatedJobFor(appCat, eventAdapter);
    		
    		// there is already a job for this message --> possible restart scenario --> just update the pod name 
    		if (jobForMess.isPresent()) {		
    			final AppDataJob job = jobForMess.get();
    			LOGGER.info("Found job {} already associated to mqiMessage {}. Ignoring new message ...",
    					job.getId(), firstMessage.getId());		
    		}
    		else if (specificJob.isPresent()) {
        		final AppDataJob existingJob = specificJob.get(); 
        		LOGGER.info("Found job {} already being handled. Appending new message ...",
        				existingJob.getId(), firstMessage.getId());
        		appCat.appendMessage(existingJob, firstMessage);
    		}
    		else {
        		LOGGER.info("Persisting new job for message {} (catalog event message {}) ...",
        				message.getId(), firstMessage.getId());
        		
            	// no job yet associated to this message --> create job and persist
                final Date now = new Date();         
                final AppDataJobGeneration gen = new AppDataJobGeneration();
                gen.setState(AppDataJobGenerationState.INITIAL);
                gen.setTaskTable(tasktableFilename);
                gen.setNbErrors(0);
                gen.setCreationDate(now);
                gen.setLastUpdateDate(now);
                
                jobFromMessage.setGeneration(gen);
                jobFromMessage.setPrepJobMessageId(message.getId());
                jobFromMessage.setPrepJobInputQueue(message.getInputKey());
                jobFromMessage.setReportingId(reporting.getUid());
                jobFromMessage.setState(AppDataJobState.GENERATING); // will activate that this request can be polled
                jobFromMessage.setPod(settings.getHostname()); 
                
                final AppDataJob newlyCreatedJob = appCat.create(jobFromMessage);
                LOGGER.info("dispatched job {}", newlyCreatedJob.getId());                
    		}
    		reporting.end(
            		new TaskTableLookupReportingOutput(Collections.singletonList(tasktableFilename)),
            		new ReportingMessage("End associating TaskTables to AppDataJob")
            );
        } catch (final Exception e) {        	
        	reporting.error(new ReportingMessage(
        			"Error associating TaskTables to AppDataJob: %s", 
        			LogUtils.toString(e)
        	));
            throw e;
        }
    }

}
