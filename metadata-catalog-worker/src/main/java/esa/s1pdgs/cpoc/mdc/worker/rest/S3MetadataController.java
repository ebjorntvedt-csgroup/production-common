package esa.s1pdgs.cpoc.mdc.worker.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.mdc.worker.service.EsServices;
import esa.s1pdgs.cpoc.metadata.model.S3Metadata;

@RestController
@RequestMapping(path = "/s3metadata")
public class S3MetadataController extends AbstractMetadataController<S3Metadata> {

	private static final Logger LOGGER = LogManager.getLogger(S3MetadataController.class);

	@Autowired
	public S3MetadataController(EsServices esServices) {
		super(S3Metadata.class, esServices);
	}

	/**
	 * Queries the elastic search for products matching the given parameters. Query
	 * build is based on the marginTT workflow extension.
	 * 
	 * @return list of matching products
	 */
	@RequestMapping(path = "/{productType}/marginTT")
	public ResponseEntity<List<S3Metadata>> getMarginTTProducts(@PathVariable(name = "productType") String productType,
			@RequestParam(name = "productFamily") final String productFamily,
			@RequestParam(name = "satellite") final String satellite, @RequestParam(name = "t0") final String startDate,
			@RequestParam(name = "t1") final String stopDate,
			@RequestParam(value = "dt0", defaultValue = "0.0") final double dt0,
			@RequestParam(value = "dt1", defaultValue = "0.0") final double dt1,
			@RequestParam(value = "timeliness") final String timeliness) {

		try {
			List<S3Metadata> response = new ArrayList<>();

			LOGGER.info("Received S3 MarginTT search query for family '{}', product type '{}', timeliness '{}'",
					productFamily.toString(), productType, timeliness);

			List<S3Metadata> result = esServices.marginTTQuery(
					convertDateForSearch(startDate, -dt0,
							DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")),
					convertDateForSearch(stopDate, dt1, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")),
					productType, satellite, timeliness, ProductFamily.fromValue(productFamily));

			if (result != null) {
				LOGGER.debug("Query returned {} results", result.size());

				for (S3Metadata s : result) {
					response.add(s);
				}
			}

			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (final AbstractCodedException e) {
			LOGGER.error("Error on performing marginTT search for product type {}: [code {}] {}", productType,
					e.getCode().getCode(), e.getLogMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (final Exception e) {
			LOGGER.error("Error on performing marginTT for product type {}: {}", productType, LogUtils.toString(e));
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieve the L1Triggering information for the given productName from the
	 * elasticsearch
	 * 
	 * @param productFamily product family of the product, used to determine index
	 * @param productName   product name, which L1Triggering should be extracted
	 * @return L1Triggering, "NONE" as default
	 */
	@RequestMapping(path = "/l1triggering")
	public ResponseEntity<String> getL1Triggering(@RequestParam(name = "productFamily") final String productFamily,
			@RequestParam(name = "productName") final String productName) {
		try {
			LOGGER.info("Received L1Triggering query for productFamily '{}' and productName '{}'",
					productFamily.toString(), productName);

			String response = esServices.getL1Triggering(ProductFamily.fromValue(productFamily), productName);

			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (final AbstractCodedException e) {
			LOGGER.error("Error on performing L1Triggering search for product name {}: [code {}] {}", productName,
					e.getCode().getCode(), e.getLogMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (final Exception e) {
			LOGGER.error("Error on performing L1Triggering search for product name {}: {}", productName,
					LogUtils.toString(e));
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String convertDateForSearch(final String dateStr, final double delta,
			final DateTimeFormatter outFormatter) {
		final LocalDateTime time = LocalDateTime.parse(dateStr,
				DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
		final LocalDateTime timePlus = time.plusSeconds(Math.round(delta));
		return timePlus.format(outFormatter);
	}

}
