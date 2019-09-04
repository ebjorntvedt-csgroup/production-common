package esa.s1pdgs.cpoc.disseminator.outbox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import esa.s1pdgs.cpoc.disseminator.config.DisseminationProperties.OutboxConfiguration;
import esa.s1pdgs.cpoc.disseminator.path.PathEvaluater;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;

public final class LocalOutboxClient extends AbstractOutboxClient {	
	public static final class Factory implements OutboxClient.Factory {
		@Override
		public OutboxClient newClient(ObsClient obsClient, OutboxConfiguration config, final PathEvaluater eval) {
			return new LocalOutboxClient(obsClient, config, eval);
		}
	}
	
	public LocalOutboxClient(ObsClient obsClient, OutboxConfiguration config, final PathEvaluater eval) {
		super(obsClient, config, eval);
	}

	@Override
	public final void transfer(final ObsObject obsObject) throws Exception {		
		final Path path = evaluatePathFor(obsObject);	
		for (final Map.Entry<String, InputStream> entry : entries(obsObject)) {
			
			final File destination = path.resolve(entry.getKey()).toFile();
			createParentIfRequired(destination);
			
			try (final InputStream in = entry.getValue();
				 final OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))
			) {				
				logger.info("Transferring {} to {}", entry.getKey(), destination);
				IOUtils.copyLarge(in, out, new byte[config.getBufferSize()]);    				
			}
		}
	}
}