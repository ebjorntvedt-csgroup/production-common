package esa.s1pdgs.cpoc.mdcatalog.extraction.mqi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.client.MqiClientFactory;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;

/**
 * Configuration of MQI client.<br/>
 * Creation of 3 services: LEVEL_PRODUCTS, LEVEL_REPORTS, LEVEL_JOBS
 * 
 * @author Viveris Technologies
 */
@Configuration
public class MqiConfiguration {
	private final MqiClientFactory mqiClientFactory;

    /**
     * Constructor
     * 
     * @param hostUri
     * @param maxRetries
     * @param tempoRetryMs
     */
    @Autowired
    public MqiConfiguration(@Value("${file.mqi.host-uri}") final String hostUri,
            @Value("${file.mqi.max-retries}") final int maxRetries,
            @Value("${file.mqi.tempo-retry-ms}") final int tempoRetryMs,
            final RestTemplateBuilder builder
    ) {
    	mqiClientFactory = new MqiClientFactory(hostUri, maxRetries, tempoRetryMs)
    			.restTemplateSupplier(builder::build);
    }

    /**
     * Service for querying MQI for LEVEL_SEGMENT category
     * 
     * @param builder
     * @return
     */
    @Bean(name = "mqiServiceForLevelSegments")
    public GenericMqiService<ProductDto> mqiServiceForLevelSegments() {    	    	
    	return mqiClientFactory.newProductServiceFor(ProductCategory.LEVEL_SEGMENTS);
    }

    /**
     * Service for querying MQI for LEVEL_PRODUCT category
     * 
     * @param builder
     * @return
     */
    @Bean(name = "mqiServiceForLevelProducts")
    public GenericMqiService<ProductDto> mqiServiceForLevelProducts() {
    	return mqiClientFactory.newProductServiceFor(ProductCategory.LEVEL_PRODUCTS);
    }

    /**
     * Service for querying MQI for LEVEL_REPORTS category
     * 
     * @param builder
     * @return
     */
    @Bean(name = "mqiServiceForAuxiliaryFiles")
    public GenericMqiService<ProductDto> mqiServiceForAuxiliaryFiles() {    
    	return mqiClientFactory.newProductServiceFor(ProductCategory.AUXILIARY_FILES);
    }

    /**
     * Service for querying MQI for LEVEL_JOBS category
     * 
     * @param builder
     * @return
     */
    @Bean(name = "mqiServiceForEdrsSessions")
    public GenericMqiService<EdrsSessionDto> mqiServiceForEdrsSessions() {
     	return mqiClientFactory.newErdsSessionService();
    }
}
