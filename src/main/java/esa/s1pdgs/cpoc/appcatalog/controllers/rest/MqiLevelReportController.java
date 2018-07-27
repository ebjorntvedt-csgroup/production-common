package esa.s1pdgs.cpoc.appcatalog.controllers.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.appcatalog.services.mongodb.MongoDBServices;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelReportDto;

/**
 * REST server for managing MQI messages in DB for the product category
 * LEVEL_REPORTS
 * 
 * @author Viveris Technologies
 */
@RestController
@RequestMapping(path = "/mqi/level_reports")
public class MqiLevelReportController
        extends GenericMqiController<LevelReportDto> {

    /**
     * Constructor
     * 
     * @param mongoDBServices
     * @param maxRetries
     */
    @Autowired
    public MqiLevelReportController(final MongoDBServices mongoDBServices,
            @Value("${mqi.max-retries}") final int maxRetries) {
        super(mongoDBServices, maxRetries, ProductCategory.LEVEL_REPORTS);
    }

}
