package fr.viveris.s1pdgs.mqi.server.distribution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.viveris.s1pdgs.common.ProductCategory;
import fr.viveris.s1pdgs.mqi.model.Ack;
import fr.viveris.s1pdgs.mqi.model.AuxiliaryFileDto;
import fr.viveris.s1pdgs.mqi.model.GenericMessageDto;
import fr.viveris.s1pdgs.mqi.model.GenericPublicationMessageDto;
import fr.viveris.s1pdgs.mqi.server.ApplicationProperties;
import fr.viveris.s1pdgs.mqi.server.consumption.MessageConsumptionController;
import fr.viveris.s1pdgs.mqi.server.publication.MessagePublicationController;

/**
 * Message distribution controller
 * 
 * @author Viveris Technologies
 */
@RestController
@RequestMapping(path = "/messages/auxiliary_files")
public class AuxiliaryFilesDistributionController
        extends GenericMessageDistribution<AuxiliaryFileDto> {

    /**
     * Constructor
     * 
     * @param messages
     */
    @Autowired
    public AuxiliaryFilesDistributionController(
            final MessageConsumptionController messages,
            final MessagePublicationController publication,
            final ApplicationProperties properties) {
        super(messages, publication, properties,
                ProductCategory.AUXILIARY_FILES);
    }

    /**
     * Get the next message to proceed for auxiliary files
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/next")
    public ResponseEntity<GenericMessageDto<AuxiliaryFileDto>> next() {
        return super.next();
    }

    /**
     * Get the next message to proceed for auxiliary files
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/ack")
    public ResponseEntity<Boolean> ack(
            @RequestParam("identifier") final long identifier,
            @RequestParam("ack") final Ack ack,
            @RequestParam(value = "message", defaultValue = "") final String message) {
        return super.ack(identifier, ack, message);
    }

    /**
     * Publish a message
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/publish")
    public ResponseEntity<Void> publish(
            @RequestBody() final GenericPublicationMessageDto<AuxiliaryFileDto> message) {
        String log = String.format("[productName: %s]",
                message.getMessageToPublish().getProductName());
        return super.publish(log, message);
    }

}
