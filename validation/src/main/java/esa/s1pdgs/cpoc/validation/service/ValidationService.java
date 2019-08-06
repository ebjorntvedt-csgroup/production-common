package esa.s1pdgs.cpoc.validation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.ProductFamilyValidation;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;
import esa.s1pdgs.cpoc.report.LoggerReporting;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.validation.service.metadata.MetadataService;

@Service
public class ValidationService {
	private static final Logger LOGGER = LogManager.getLogger(ValidationService.class);

	private final MetadataService metadataService;

	private final ObsClient obsClient;

	@Autowired
	public ValidationService(MetadataService metadataService, ObsClient obsClient) {
		this.metadataService = metadataService;
		this.obsClient = obsClient;
	}

	public int checkConsistencyForInterval(LocalDateTime startInterval, LocalDateTime endInterval) {
		final Reporting.Factory reportingFactory = new LoggerReporting.Factory(LOGGER, "ValidationService");
		int discrepancies = 0;
		for (ProductFamilyValidation family : ProductFamilyValidation.values()) {
			discrepancies += validateProductFamily(reportingFactory, family, startInterval, endInterval);
		}
		return discrepancies;
	}

	int validateProductFamily(Reporting.Factory reportingFactory, ProductFamilyValidation family,
			LocalDateTime startInterval, LocalDateTime endInterval) {

		final Reporting reportingValidation = reportingFactory.newReporting(0);
		reportingValidation.reportStart(String.format("Starting validation task from %s to %s for family %s",
				startInterval, endInterval, family));

		final Reporting reportingMetadata = reportingFactory.newReporting(1);

		int discrepancies = 0;

		try {

			List<SearchMetadata> metadataResults = null;
			try {
				reportingMetadata.reportStart("Gathering discrepancies in metadata catalog");
				metadataResults = metadataService.query(ProductFamily.valueOf(family.name()), startInterval,
						endInterval);
				if (metadataResults == null) {
					// set to empty list
					metadataResults = new ArrayList<>();
				}
			} catch (MetadataQueryException e) {
				reportingMetadata.reportError("Error occured while performing metadata catalog query task [code {}] {}",
						e.getCode().getCode(), e.getLogMessage());
				throw e;
			}

			final Reporting reportingObs = reportingFactory.newReporting(2);
			Map<String, ObsObject> obsResults = null;
			try {
				reportingObs.reportStart("Gathering discrepancies in OBS");

				Date startDate = Date.from(startInterval.atZone(ZoneId.of("UTC")).toInstant());
				Date endDate = Date.from(endInterval.atZone(ZoneId.of("UTC")).toInstant());

				obsResults = obsClient.listInterval(ProductFamily.valueOf(family.name()), startDate, endDate);
				LOGGER.info("OBS query for family '{}' returned {} results", family, obsResults.size());

			} catch (SdkClientException | DateTimeParseException e) {
				reportingObs.reportError("Error occured while performing obs query task: {}", e.getMessage());
				throw e;
			}

			List<String> metadataDiscrepancies = new ArrayList<>();
//			List<String> obsDiscrepancies = new ArrayList<>();

			for (SearchMetadata smd : metadataResults) {
				//if (obsResults.get(smd.getKeyObjectStorage()) == null) {
				if (!verifyMetadataForObject(smd, obsResults.values())) {
					LOGGER.info("Product {} does exist in metadata catalog, but not in OBS", smd.getKeyObjectStorage());
					metadataDiscrepancies.add(smd.getKeyObjectStorage());
				} else {
					LOGGER.debug("Product {} does exist in metadata catalog and OBS", smd.getKeyObjectStorage());
				}
			}

			/*if (obsResults.size() > 0) {
				LOGGER.info("Found {} products that exist in OBS, but not in MetadataCatalog", obsResults.size());
				for (ObsObject product : obsResults.values()) {
					metadataDiscrepancies.add(product.getKey());
					LOGGER.info("Product {} does exist in OBS, but not in MetadataCatalog", product.getKey());
				}
			}*/

			if (metadataDiscrepancies.isEmpty()) {
				reportingMetadata.reportStop("No discrepancies found in MetadataCatalog");
				reportingValidation.reportStop("No discrepancy found");
			} else {
				reportingMetadata.reportError("Products not present in MetadataCatalog: {}",
						buildProductList(metadataDiscrepancies));
				reportingValidation.reportError("Discrepancy found for {} product(s)", discrepancies);
			}
/*
			if (obsDiscrepancies.isEmpty()) {
				reportingObs.reportStop("No discepancies found in OBS");
			} else {
				reportingObs.reportError("Product(s) not present in OBS: {}", buildProductList(obsDiscrepancies));
			}

			if (metadataDiscrepancies.isEmpty() && obsDiscrepancies.isEmpty()) {
				reportingValidation.reportStop("No discrepancy found");

			} else {
				discrepancies = metadataDiscrepancies.size() + obsDiscrepancies.size();
				reportingValidation.reportError("Discrepancy found for {} product(s)", discrepancies);
			}*/
			return metadataDiscrepancies.size();
		} catch (Exception ex) {
			reportingValidation.reportError("Error occured while performing validation task: {}", ex.getMessage());
		}

		return 0;
	}
	
	private boolean verifyMetadataForObject(SearchMetadata metadata, Collection<ObsObject> objects) {
		LOGGER.debug("Verifying if metadata entry for product {} exist in OBS", metadata.getKeyObjectStorage());
		if (metadata.getKeyObjectStorage().contains("AUX") || metadata.getKeyObjectStorage().contains("MPL")) {
			return verifyAuxMetadataForObject(metadata, objects);
		} else if (metadata.getKeyObjectStorage().startsWith("S1A/") || metadata.getKeyObjectStorage().startsWith("S1B/")) {
			return verifySessionForObject(metadata, objects);
		} else {
			return verifySliceForObject(metadata,objects);
		}
	}
	
	private boolean verifyAuxMetadataForObject(SearchMetadata metadata, Collection<ObsObject> objects) {
		String key = metadata.getKeyObjectStorage();
		
		for (ObsObject obj: objects) {
			String auxname = obj.getKey().substring(0,obj.getKey().length());			
			if (key.equals(auxname)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean verifySessionForObject(SearchMetadata metadata, Collection<ObsObject> objects ) {
		String key = metadata.getKeyObjectStorage();
		
		for (ObsObject obj: objects) {
			String obsKey = obj.getKey();
			if (key.equals(obsKey)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean verifySliceForObject(SearchMetadata metadata, Collection<ObsObject> objects) {
		String key = metadata.getKeyObjectStorage();
		
		for (ObsObject obj: objects) {
			String obsKey = obj.getKey().substring(0,obj.getKey().indexOf("/"));
			if (key.equals(obsKey)) {
				return true;
			}
		}
		return false;
	}


	private String buildProductList(List<String> products) {
		if (products.size() == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i = 0; i < products.size(); i++) {
			builder.append(products.get(i));
			if (i < products.size() - 1) {
				builder.append(",");
			}
		}
		builder.append("]");
		return builder.toString();
	}

}
