package esa.s1pdgs.cpoc.ingestion.trigger.inbox;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.ingestion.trigger.entity.InboxEntry;
import esa.s1pdgs.cpoc.ingestion.trigger.filter.InboxFilter;
import esa.s1pdgs.cpoc.ingestion.trigger.kafka.producer.SubmissionClient;
import esa.s1pdgs.cpoc.ingestion.trigger.report.IngestionTriggerReportingInput;
import esa.s1pdgs.cpoc.ingestion.trigger.report.IngestionTriggerReportingOutput;
import esa.s1pdgs.cpoc.ingestion.trigger.service.IngestionTriggerServiceTransactional;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionJob;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class Inbox {
	private static final Logger LOG = LoggerFactory.getLogger(Inbox.class);
	
	private final InboxAdapter inboxAdapter;
	private final InboxFilter filter;
	private final IngestionTriggerServiceTransactional ingestionTriggerServiceTransactional;
	private final SubmissionClient client;
	private final ProductFamily family;

	Inbox(
			final InboxAdapter inboxAdapter, 
			final InboxFilter filter,
			final IngestionTriggerServiceTransactional ingestionTriggerServiceTransactional, 
			final SubmissionClient client,
			final ProductFamily family
	) {
		this.inboxAdapter = inboxAdapter;
		this.filter = filter;
		this.ingestionTriggerServiceTransactional = ingestionTriggerServiceTransactional;
		this.client = client;
		this.family = family;
	}
	
	public final void poll() {
		try {
			final Set<InboxEntry> persistedContent = ingestionTriggerServiceTransactional
					.getAllForPath(inboxAdapter.inboxURL());
						
			// read all entries that have not been persisted before
			final Set<InboxEntry> pickupContent = new HashSet<>(inboxAdapter.read(InboxFilter.ALLOW_ALL));
			
			// determine the entries that have been deleted from inbox to remove them from persistence
			final Set<InboxEntry> finishedElements = new HashSet<>(persistedContent);
			finishedElements.removeAll(pickupContent);
			
			final StringBuilder logMessage = new StringBuilder();
			if (!finishedElements.isEmpty()) {
				logMessage.append("Handled ")
					.append(finishedElements.size())
					.append(" finished elements (")
					.append(summarize(finishedElements))
					.append(")");
				// when a product has been removed from the inbox directory, it shall be removed
				// from the persistence so it will not be ignored if it occurs again on the inbox
				LOG.debug("Deleting all {} from persistence", summarize(finishedElements));
				ingestionTriggerServiceTransactional.removeFinished(finishedElements);
			}
			
			final Set<InboxEntry> newElements = new HashSet<>(pickupContent);
			newElements.removeAll(persistedContent);

			for (final InboxEntry newEntry : newElements) {
				if (filter.accept(newEntry)) {
					LOG.debug("adding {}", newEntry);	
					handleEntry(newEntry);	
				}
				else {
					LOG.info("{} is ignored by {}", newEntry, filter);
				}	
				persist(newEntry);
			}

			if (!newElements.isEmpty()) {
				if (logMessage.length() == 0) {
					logMessage.append("Handled ");
				} else {
					logMessage.append(" and ");
				}
				logMessage.append(newElements.size())
					.append(" new elements (")
					.append(summarize(newElements))
					.append(")");	
			}
			if (logMessage.length() != 0) {
				LOG.info(logMessage.toString());
			}
		} catch (final Exception e) {			
			// thrown on error reading the Inbox. No real retry here as it will be retried on next polling attempt anyway	
			LOG.error(String.format("Error on polling %s", description()), e);
		}
	}

	public final String description() {
		return inboxAdapter.description() + " for productFamily " + family + "";
	}

	@Override
	public final String toString() {
		return "Inbox [inboxAdapter=" + inboxAdapter + ", filter=" + filter + ", client=" + client + "]";
	}
	
	private void handleEntry(final InboxEntry entry) {
				
		final Reporting reporting = ReportingUtils.newReportingBuilder()
				.newReporting("IngestionTrigger");
			
		reporting.begin(
				new IngestionTriggerReportingInput(entry.getName(), new Date(), entry.getLastModified()),
				new ReportingMessage("New file detected %s", entry.getName())
		);
		
		// empty files are not accepted!
		if (entry.getSize() == 0) {	
			reporting.error(new ReportingMessage("File %s is empty, ignored.", entry.getName()));						
			return;
		}
		
		try {
			LOG.debug("Publishing new entry to kafka queue: {}", entry);					
			final IngestionJob dto = new IngestionJob(family, entry.getName());
		    dto.setRelativePath(entry.getRelativePath());
		    dto.setPickupBaseURL(entry.getPickupURL());
		    dto.setProductName(entry.getName());
		    dto.setUid(reporting.getUid());
			client.publish(dto);	
			reporting.end(
					new IngestionTriggerReportingOutput(entry.getPickupURL() + "/" + entry.getRelativePath()), 
					new ReportingMessage("File %s created IngestionJob", entry.getName())
			);
		} catch (final Exception e) {
			reporting.error(new ReportingMessage("File %s could not be handled: %s", entry.getName(), LogUtils.toString(e)));
			LOG.error(String.format("Error on handling %s in %s: %s", entry, description(), LogUtils.toString(e)));
		}		
	}
	
	private InboxEntry persist(final InboxEntry toBePersisted) {
		final InboxEntry persisted = ingestionTriggerServiceTransactional.add(toBePersisted);
		LOG.debug("Added {} to persistence", persisted);
		return persisted;
	}
	
	private String summarize(final Collection<InboxEntry> entries) {
		final String summary = entries.stream()
			.map(InboxEntry::getName)
			.collect(Collectors.joining(", "));
		
		if (StringUtils.isEmpty(summary)) {
			return "[none]";
		}
		return summary;
	}
}
