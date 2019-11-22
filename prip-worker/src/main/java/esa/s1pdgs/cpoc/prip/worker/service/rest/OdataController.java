package esa.s1pdgs.cpoc.prip.worker.service.rest;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.prip.metadata.PripMetadataRepository;
import esa.s1pdgs.cpoc.prip.worker.service.edm.EdmProvider;
import esa.s1pdgs.cpoc.prip.worker.service.processor.ProductEntityCollectionProcessor;
import esa.s1pdgs.cpoc.prip.worker.service.processor.ProductEntityProcessor;

@RestController
@RequestMapping(value = "/odata")
public class OdataController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OdataController.class);

	@Autowired
	private EdmProvider edmProvider;
	
	@Autowired
	private PripMetadataRepository pripMetadataRepository;
	
	@Autowired
	private ObsClient obsClient;
	
	@Value("${prip-worker.download-url-expiration-time-in-seconds:600}")
	private long downloadUrlExpirationTimeInSeconds;
	
	@RequestMapping(value = "/v1/**")
	public void process(HttpServletRequest request, HttpServletResponse response) {
		String queryParams = request.getQueryString() == null ? "" : "?" + request.getQueryString();
		LOGGER.info("Received HTTP request for URL: {}{}", request.getRequestURL().toString(), queryParams);		
		OData odata = OData.newInstance();
		ServiceMetadata serviceMetadata = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());
		ODataHttpHandler handler = odata.createHandler(serviceMetadata);
		handler.register(new ProductEntityProcessor(pripMetadataRepository, obsClient, downloadUrlExpirationTimeInSeconds));
		handler.register(new ProductEntityCollectionProcessor(pripMetadataRepository));

		handler.process(new HttpServletRequestWrapper(request) {
	         @Override
	         public String getServletPath() {
	            return "odata/v1"; // just the prefix up to /odata/v1, the rest is used as parameters by Olingo
	         }
	      }, response);
	}
}
