package esa.s1pdgs.cpoc.validation.service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;
import esa.s1pdgs.cpoc.validation.service.metadata.MetadataService;
import esa.s1pdgs.cpoc.validation.service.obs.ObsService;

@Service
public class ValidationService {
	private static final Logger LOGGER = LogManager.getLogger(ValidationService.class);

	private final MetadataService metadataService;
	
	private final ObsService obsService;

	@Autowired
	public ValidationService(MetadataService metadataService, ObsService obsService) {
		this.metadataService = metadataService;
		this.obsService = obsService;
	}

	public void process(ProductFamily family, LocalDateTime intervalStart, LocalDateTime intervalStop) {
		LOGGER.info("Validating for inconsistancy between time interval from {} and {}", intervalStart, intervalStop);
		
		//TODO hardcoded!
		family = ProductFamily.L0_ACN;
		
		List<SearchMetadata> metadataResults = null;
		try {
			metadataResults = metadataService.query(family, null, "2000-01-01T00:00:00.123456Z",
					"2019-12-01T00:00:00.123456Z");
			if (metadataResults == null) {
				// TODO: we might handle this differently.
				return;
			}
			
			LOGGER.info("Metadata query for family '{}' returned {} results",family,metadataResults.size());
			
		} catch (AbstractCodedException ex) {
			String errorMessage = String.format(
					"[ValidationTask] [subTask retrieveMetadata] [STOP KO] %s [code %d] %s",
					"LOG_ERROR", ex.getCode().getCode(), ex.getLogMessage());
			LOGGER.error(errorMessage);
			return;
		}
		
		List<ObsObject> filesResult = null; 
		try {
			Date startDate = Date.from(intervalStart.atZone(ZoneId.systemDefault()).toInstant());
			Date stopDate = Date.from(intervalStop.atZone(ZoneId.systemDefault()).toInstant());
			
			filesResult = obsService.listInterval(family, startDate, stopDate);
			LOGGER.info("obs query for family '{}' returned {} results",family,filesResult.size());
		} catch (SdkClientException ex) {
			String errorMessage = String.format(
					"[ValidationTask] [subTask retrieveObs] [STOP KO] %s [step %d] %s %s",
					"LOG_ERROR",  ex.getMessage());
			LOGGER.error(errorMessage);	
		}
		
	}

}
