package esa.s1pdgs.cpoc.ipf.preparation.worker.type.spp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerInputsMissingException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.common.utils.Exceptions;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTableAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.AbstractProductTypeAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.Product;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.ProductTypeAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.slice.LevelSliceProduct;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.L0AcnMetadata;
import esa.s1pdgs.cpoc.metadata.model.L0SliceMetadata;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.queue.util.CatalogEventAdapter;
import esa.s1pdgs.cpoc.xml.model.joborder.JobOrder;
import esa.s1pdgs.cpoc.xml.model.joborder.JobOrderSensingTime;

public class SppMbuTypeAdapter extends AbstractProductTypeAdapter implements ProductTypeAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SppMbuTypeAdapter.class);

    private final MetadataClient metadataClient;
    private final Map<String, Float> sliceOverlap;
	private final Map<String, Float> sliceLength;
	private final Map<String,String> timelinessMapping;

    public SppMbuTypeAdapter(
			final MetadataClient metadataClient,
			final Map<String, Float> sliceOverlap,
			final Map<String, Float> sliceLength,
			final Map<String,String> timelinessMapping
	) {
		this.metadataClient = metadataClient;
		this.sliceOverlap = sliceOverlap;
		this.sliceLength = sliceLength;
		this.timelinessMapping = timelinessMapping;
	}

    @Override
    public Product mainInputSearch(final AppDataJob job, final TaskTableAdapter tasktableAdpter) {
    	final LevelSliceProduct product = LevelSliceProduct.of(job);
		
		// Retrieve instrument configuration id and slice number
		try {
			final L0SliceMetadata file = metadataClient.getL0Slice(product.getProductName());
			product.setInsConfId(file.getInstrumentConfigurationId());
			product.setNumberSlice(file.getNumberSlice());
			product.setDataTakeId(file.getDatatakeId());
			product.addSlice(file);
			
		} catch (final MetadataQueryException e) {
			LOGGER.debug("L0 slice for {} not found in MDC (error was {}). Trying next time...", product.getProductName(), 
					Exceptions.messageOf(e));
		}
		// Retrieve Total_Number_Of_Slices
		try {
			final L0AcnMetadata acn = metadataClient.getFirstACN(
					product.getProductName(),
					product.getProcessMode()
			);
			product.setTotalNbOfSlice(acn.getNumberOfSlices());
			product.setSegmentStartDate(acn.getValidityStart());
			product.setSegmentStopDate(acn.getValidityStop());
			product.addAcn(acn);
		} catch (final MetadataQueryException e) {
			LOGGER.debug("L0 acn for {} not found in MDC (error was {}). Trying next time...", product.getProductName(), 
					Exceptions.messageOf(e));
		}
		return product;
    }

    @Override
	public void validateInputSearch(final AppDataJob job, final TaskTableAdapter tasktableAdpter) throws IpfPrepWorkerInputsMissingException {
		final LevelSliceProduct product = LevelSliceProduct.of(job);
		
		// there needs to be a slice
		if (product.getSlices().isEmpty()) {
			throw new IpfPrepWorkerInputsMissingException(
					Collections.singletonMap(
							product.getProductName(), 
							"No Slice: " + 	product.getProductName()
					)
			);
		}
		
		// if it's there, job creation can proceed
		LOGGER.info("Found slice {}", product.getSlices());
	}

	@Override
	public List<AppDataJob> createAppDataJobs(final IpfPreparationJob job) {
		final AppDataJob appDataJob = AppDataJob.fromPreparationJob(job);
		
		final CatalogEventAdapter eventAdapter = CatalogEventAdapter.of(appDataJob);
		final LevelSliceProduct product = LevelSliceProduct.of(appDataJob);		
		product.setAcquisition(eventAdapter.swathType()); 
        product.setPolarisation(eventAdapter.polarisation());
        
        return Collections.singletonList(appDataJob);
	}

	@Override
	public final void customJobOrder(final AppDataJob job, final JobOrder jobOrder) {
		final LevelSliceProduct product = LevelSliceProduct.of(job);
		
		// Rewrite job order sensing time
		final String jobOrderStart = DateUtils.convertToAnotherFormat(product.getSegmentStartDate(),
				AppDataJobProduct.TIME_FORMATTER, JobOrderSensingTime.DATETIME_FORMATTER);
		final String jobOrderStop = DateUtils.convertToAnotherFormat(product.getSegmentStopDate(),
				AppDataJobProduct.TIME_FORMATTER, JobOrderSensingTime.DATETIME_FORMATTER);
		
		jobOrder.getConf().setSensingTime(new JobOrderSensingTime(jobOrderStart, jobOrderStop));

		updateProcParam(
				jobOrder, 
				"Mission_Id",
				product.getMissionId() + product.getSatelliteId());
		updateProcParam(
				jobOrder, 
				"Slice_Number", 
				String.valueOf(product.getNumberSlice()));
		updateProcParam(
				jobOrder, 
				"Total_Number_Of_Slices",
				String.valueOf(product.getTotalNbOfSlice()));
		updateProcParam(
				jobOrder, 
				"Slice_Overlap",
				String.valueOf(sliceOverlap.get(product.getAcquisition())));
		updateProcParam(
				jobOrder, 
				"Slice_Length",
				String.valueOf(sliceLength.get(product.getAcquisition())));
		updateProcParam(
				jobOrder, 
				"Slicing_Flag", 
				"TRUE"
		);
		
		// S1PRO-2194: evaluate mapped timeliness value. Keep default, if there is no
		// corresponding mapping configured;
		final String inputTimeliness = product.getTimeliness(); // is an empty string if not defined
		
		final String timelinessInJoborder = timelinessMapping.get(inputTimeliness);
		if (timelinessInJoborder != null) {
			LOGGER.debug("Adding 'Timeliness_Category' value '{}' to joborder of job {} (input was: {})", 
					timelinessInJoborder, job.getId(), inputTimeliness);
			updateProcParamIfDefined(
					jobOrder, 
					"Timeliness_Category", 
					timelinessInJoborder
			);
		}
		else {
			LOGGER.trace("Omitting provision of timeliness value {} (no mapping defined in {})",  
					inputTimeliness, timelinessMapping);
		}
	}

	@Override
	public final void customJobDto(final AppDataJob job, final IpfExecutionJob dto) {
		// NOTHING TO DO
	}
}
