/**
 * 
 */
package esa.s1pdgs.cpoc.appcatalog.server.mqi.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.appcatalog.server.mqi.db.MqiMessageService;
import esa.s1pdgs.cpoc.appcatalog.server.status.AppStatus;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;

/**
 * REST server for managing MQI messages in DB for the product category
 * AUXILIARY_FILES
 * 
 * @author Viveris Technologies
 */
@RestController
@RequestMapping(path = "/mqi/auxiliary_files")
public class MqiAuxiliaryFileController
        extends GenericMqiController<ProductDto> {

    /**
     * @param mongoDBServices
     * @param maxRetries
     */
    @Autowired
    public MqiAuxiliaryFileController(final MqiMessageService mongoDBServices,
            @Value("${mqi.max-retries}") final int maxRetries,
            final AppStatus appStatus,
            @Value("${mqi.dft-offset}") final int dftOffset) {
        super(mongoDBServices, maxRetries, ProductCategory.AUXILIARY_FILES,
                appStatus, dftOffset);
    }

}
