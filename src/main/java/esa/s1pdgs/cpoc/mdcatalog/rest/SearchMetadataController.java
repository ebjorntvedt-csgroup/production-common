package esa.s1pdgs.cpoc.mdcatalog.rest;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.mdcatalog.es.EsServices;
import esa.s1pdgs.cpoc.mdcatalog.es.model.SearchMetadata;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException.ErrorCode;
import esa.s1pdgs.cpoc.mdcatalog.rest.dto.SearchMetadataDto;

@RestController
@RequestMapping(path = "/metadata")
public class SearchMetadataController {

	private static final Logger LOGGER = LogManager.getLogger(SearchMetadataController.class);

	private final EsServices esServices;

	@Autowired
	public SearchMetadataController(final EsServices esServices) {
		this.esServices = esServices;
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{productFamily}/search")
	public ResponseEntity<SearchMetadataDto> search(@PathVariable(name = "productFamily") String productFamily,
	        @RequestParam(name = "productType") String productType,
			@RequestParam(name = "mode") String mode, @RequestParam(name = "satellite") String satellite,
			@RequestParam(name = "t0") String startDate, @RequestParam(name = "t1") String stopDate,
			@RequestParam(name = "insConfId", defaultValue = "-1") int insConfId,
			@RequestParam(value = "dt0", defaultValue = "0.0") double dt0,
			@RequestParam(value = "dt1", defaultValue = "0.0") double dt1) {
		try {
			if (mode.equals("LatestValCover")) {
				SearchMetadata f = esServices.lastValCover(productType, ProductFamily.fromValue(productFamily),
						convertDateForSearch(startDate, -dt0,
								DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.999999")),
						convertDateForSearch(stopDate, dt1,
								DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.000000")),
						satellite, insConfId);
				SearchMetadataDto response = null;
				if (f != null) {
					response = new SearchMetadataDto(f.getProductName(), f.getProductType(), f.getKeyObjectStorage(),
							f.getValidityStart(), f.getValidityStop());
				}
				return new ResponseEntity<SearchMetadataDto>(response, HttpStatus.OK);
			} else {
				LOGGER.error("[productType {}] [code {}] [mode {}] [msg Unknown mode]", productType,
						ErrorCode.ES_INVALID_SEARCH_MODE.getCode(), mode);
				return new ResponseEntity<SearchMetadataDto>(HttpStatus.BAD_REQUEST);
			}
		} catch (AbstractCodedException e) {
			LOGGER.error("[productType {}] [code {}] [mode {}] {}", productType, e.getCode().getCode(), mode,
					e.getLogMessage());
			return new ResponseEntity<SearchMetadataDto>(HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			LOGGER.error("[productType {}] [code {}] [mode {}] [msg {}]", productType,
					ErrorCode.INTERNAL_ERROR.getCode(), mode, e.getMessage());
			return new ResponseEntity<SearchMetadataDto>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	private String convertDateForSearch(String dateStr, double delta, DateTimeFormatter outFormatter)
			throws ParseException {

		LocalDateTime time = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDateTime timePlus = time.plusSeconds(Math.round(delta));
		return timePlus.format(outFormatter);
	}
}
