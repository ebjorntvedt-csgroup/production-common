package esa.s1pdgs.cpoc.ingestion.trigger.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.ingestion.trigger.Inbox;
import esa.s1pdgs.cpoc.ingestion.trigger.InboxFactory;
import esa.s1pdgs.cpoc.ingestion.trigger.IngestionTriggerService;

@Configuration
public class IngestionTriggerServiceConfiguration {
	private static final Logger LOG = LoggerFactory.getLogger(IngestionTriggerServiceConfiguration.class);

	private final IngestionTriggerConfigurationProperties properties;
	private final InboxFactory inboxFactory;

	@Autowired
	public IngestionTriggerServiceConfiguration(
			final IngestionTriggerConfigurationProperties properties,
			final InboxFactory inboxFactory
	) {
		this.properties = properties;
		// InboxFactory is autowired here without a qualifier because there is only one
		// implementation of it in the classpath. This needs to be changed in the future
		// when there are other types of inboxes available
		this.inboxFactory = inboxFactory;
	}

	@Bean
	public IngestionTriggerService newInboxService() {
		final List<Inbox> inboxes = new ArrayList<>();

		for (final InboxConfiguration config : properties.getPolling()) {
			try {
				final Path inboxPath = Paths.get(config.getDirectory());
				inboxes.add(inboxFactory.newInbox(config));
				Files.createDirectories(inboxPath);
			} catch (IllegalArgumentException | IOException e) {
				LOG.error(e.getMessage());
			}
		}
		return new IngestionTriggerService(inboxes);
	}	
}
