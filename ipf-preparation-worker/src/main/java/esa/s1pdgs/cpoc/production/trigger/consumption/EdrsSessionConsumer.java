package esa.s1pdgs.cpoc.production.trigger.consumption;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJobFile;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.CatalogEventAdapter;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public final class EdrsSessionConsumer implements ProductTypeConsumptionHandler {	
	private static final String TYPE = "EdrsSession";
	private static final Logger LOGGER = LogManager.getLogger(EdrsSessionConsumer.class);
	
    @Override
	public final String type() {
		return TYPE;
	}
	
	@Override
	public final AppDataJobProduct newProductFor(final GenericMessageDto<CatalogEvent> mqiMessage) {
		final CatalogEvent event = mqiMessage.getBody();
        final AppDataJobProduct productDto = new AppDataJobProduct();
        productDto.getMetadata().putAll(event.getMetadata());
		
		
		final CatalogEventAdapter eventAdapter = new CatalogEventAdapter(event);
		
        final AppDataJobProduct productDto = new AppDataJobProduct();
        productDto.setProductType(event.getProductType());
        productDto.setSessionId(eventAdapter.sessionId());
        productDto.setMissionId(eventAdapter.missionId());
        productDto.setStationCode(eventAdapter.stationCode());
        
        productDto.setProductName(eventAdapter.sessionId()); 
        
        productDto.setSatelliteId(eventAdapter.satelliteId());
        productDto.setStartTime(eventAdapter.startTime());
        productDto.setStopTime(eventAdapter.stopTime());
        
        
        addRawsFor(productDto, eventAdapter);
        return productDto;
	}
     
	private final void addRawsFor(final AppDataJobProduct product, final CatalogEventAdapter eventAdapter) {
        if (eventAdapter.channelId() == 1) {
            LOGGER.debug ("== ch1 ");    
            product.setRaws1(raws(eventAdapter));
        } else {
        	LOGGER.debug ("== ch2 ");
        	product.setRaws2(raws(eventAdapter));
        }
	}

	private final List<AppDataJobFile> raws(final CatalogEventAdapter eventAdapter) {
    	return eventAdapter.listValues("rawNames").stream()
    			.map(s -> new AppDataJobFile(s))
                .collect(Collectors.toList());
    }
}
