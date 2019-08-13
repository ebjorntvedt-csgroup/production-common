package esa.s1pdgs.cpoc.disseminator.outbox;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.disseminator.config.DisseminationProperties.OutboxConfiguration;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;

public interface OutboxClient {		
	public static interface Factory {
		public static final Factory NOT_DEFINED_ERROR = new Factory() {			
			@Override public OutboxClient newClient(ObsClient obsClient, OutboxConfiguration config) {
				throw new RuntimeException(String.format("No OutboxClient.Factory exists for protocol %s", config.getProtocol()));
			}
		}; 
		
		OutboxClient newClient(final ObsClient obsClient, final OutboxConfiguration config);
	}	
	
	public static final OutboxClient NULL = new OutboxClient() {
		@Override
		public final void transfer(ProductFamily family, String keyObjectStorage) throws Exception {
			// do nothing
		}		
	};
	
	void transfer(ProductFamily family, String keyObjectStorage) throws Exception;
}
