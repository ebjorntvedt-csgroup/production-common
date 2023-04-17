package de.werum.coprs.nativeapi.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.werum.coprs.nativeapi.rest.model.stac.StacCatalog;
import de.werum.coprs.nativeapi.rest.model.stac.StacCollection;
import de.werum.coprs.nativeapi.rest.model.stac.StacItemCollection;
import de.werum.coprs.nativeapi.rest.model.stac.StacRootCatalog;
import de.werum.coprs.nativeapi.service.NativeAPIServiceImpl;

@CrossOrigin
@RestController
@RequestMapping("/stac")
public class StacRestController {
	private static final Logger LOG = LogManager.getLogger(StacRestController.class);

	@Autowired
	private NativeAPIServiceImpl nativeAPI;

	@RequestMapping(path = "", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<StacRootCatalog> handleStacLandingPage() {
		LOG.info("Received external stac landing page request");

		return ResponseEntity.ok(nativeAPI.getLandingPage());
	}

	@RequestMapping(path = "/{catalog}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<StacCatalog> handleStacSubCatalogPage(@PathVariable("catalog") final String catalog) {
		LOG.info("Received external request for sub catalog page: {}", catalog);

		StacCatalog result = nativeAPI.getSubCatalogPage(catalog);

		if (result != null) {
			return ResponseEntity.ok(result);
		}

		throw new StacRestControllerException("SubCatalog " + catalog + " not found", HttpStatus.NOT_FOUND);
	}

	@RequestMapping(path = "/{catalog}/collections", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<StacCatalog> handleStacCollectionsPage(@PathVariable("catalog") final String catalog) {
		LOG.info("Received external request for stac collections page");

		StacCatalog result = nativeAPI.getCollectionsPage(catalog);

		if (result != null) {
			return ResponseEntity.ok(result);
		}

		throw new StacRestControllerException("Collections for SubCatalog " + catalog + " not found",
				HttpStatus.NOT_FOUND);
	}

	@RequestMapping(path = "/{catalog}/collections/{name}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<StacCollection> handleStacCollectionPage(@PathVariable("catalog") final String catalog,
			@PathVariable("name") final String name) {
		LOG.info("Received external request for stac collection page: {}", name);

		StacCollection result = nativeAPI.getCollectionPage(catalog, name);
		if (result != null) {
			return ResponseEntity.ok(result);
		}

		throw new StacRestControllerException("Collection " + name + " not found", HttpStatus.NOT_FOUND);
	}

	@RequestMapping(path = "/{catalog}/collections/{name}/items", method = RequestMethod.GET, produces = "application/geo+json")
	public ResponseEntity<StacItemCollection> handleStacCollectionItemsPage(
			@PathVariable("catalog") final String catalog, @PathVariable("name") final String name,
			final HttpServletRequest request) {
		LOG.info("Received external request for stac collection items page: {}", name);
		
		Map<String, String> parameters = new HashMap<>();
		parameters.put("collections", name);
		
		StacItemCollection result = nativeAPI.processSearchRequest(parameters);
		if (result != null) {
			return ResponseEntity.ok(result);
		}

		throw new StacRestControllerException("No items found for collection " + name, HttpStatus.NOT_FOUND);
	}

	@RequestMapping(path = "/search", method = RequestMethod.GET, produces = "application/geo+json")
	public ResponseEntity<StacItemCollection> handleStacItemSearch(final HttpServletRequest request) {
		LOG.info("Received external query request: {}", request.toString());
		
		LOG.info(request.getAttributeNames());
		
		StacItemCollection result = nativeAPI.processSearchRequest(request);
		if (result != null) {
			return ResponseEntity.ok(result);
		}

		return ResponseEntity.notFound().build();
	}
}