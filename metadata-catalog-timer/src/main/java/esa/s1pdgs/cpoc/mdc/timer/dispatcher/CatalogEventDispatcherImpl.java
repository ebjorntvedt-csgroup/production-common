package esa.s1pdgs.cpoc.mdc.timer.dispatcher;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.mdc.timer.db.CatalogEventTimerEntry;
import esa.s1pdgs.cpoc.mdc.timer.db.CatalogEventTimerEntryRepository;
import esa.s1pdgs.cpoc.mdc.timer.publish.Publisher;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;

public class CatalogEventDispatcherImpl implements CatalogEventDispatcher {
	
	private static final LocalDateTime MIN_DATE = LocalDateTime.of(2000, 1, 1, 0, 0);
	
	private MetadataClient metadataClient;
	private CatalogEventTimerEntryRepository repository;
	private Publisher publisher;
	private String productType;
	private ProductFamily productFamily;

	public CatalogEventDispatcherImpl(final MetadataClient metadataClient,
			final CatalogEventTimerEntryRepository repository, final Publisher publisher, final String productType,
			final ProductFamily productFamily) {
		this.metadataClient = metadataClient;
		this.repository = repository;
		this.productType = productType;
		this.productFamily = productFamily;
		this.publisher = publisher;
	}

	@Override
	public void run() {
		LocalDateTime intervalStart;
		CatalogEventTimerEntry entry = getCatalogEventTimerEntry();

		// Get the intervalStart from the database entry
		if (entry == null) {
			intervalStart = MIN_DATE;
		} else {
			intervalStart = LocalDateTime.ofInstant(entry.getLastCheckDate().toInstant(), ZoneId.systemDefault());
		}
		
		LOGGER.debug("Retrieved last timestamp {}", intervalStart.toString());
		LocalDateTime intervalStop = LocalDateTime.now();

		try {
			LOGGER.debug("Retrieve new products from database");
			List<SearchMetadata> products = this.metadataClient.searchInterval(this.productFamily, this.productType,
					intervalStart, intervalStop);

			for (SearchMetadata product : products) {
				LOGGER.info("Publish CatalogEvent for product {}", product.getProductName());
				CatalogEvent event = toCatalogEvent(product);
				this.publisher.publish(event);
			}

			if (entry == null) {
				entry = new CatalogEventTimerEntry();
				entry.setProductFamily(this.productFamily);
				entry.setProductType(this.productType);
			}
			updateCatalogEventTimerEntry(entry, intervalStop);
		} catch (MetadataQueryException e) {
			LOGGER.warn("An exception occured while fetching new products: ", e);
		} catch (Exception e) {
			LOGGER.warn("An exception occured while publishing a CatalogEvent: ", e);
		}
	}

	/**
	 * Retrieve the corresponding entry from the database, to determine the last
	 * time this dispatcher ran
	 * 
	 * @return CatalogEventTimerEntry corresponding to this dispatcher
	 */
	private CatalogEventTimerEntry getCatalogEventTimerEntry() {
		List<CatalogEventTimerEntry> entries = this.repository.findByProductTypeAndProductFamily(this.productType,
				this.productFamily);

		if (entries.isEmpty()) {
			return null;
		}

		return entries.get(0);
	}

	/**
	 * Convert a product from the elastic search into an catalog event
	 */
	private CatalogEvent toCatalogEvent(SearchMetadata metadata) {
		CatalogEvent event = new CatalogEvent();
		event.setProductFamily(this.productFamily);
		event.setProductName(metadata.getProductName());
		event.setKeyObjectStorage(metadata.getKeyObjectStorage());
		event.setProductType(this.productType);
		event.setMetadata(new HashMap<String, Object>());

		return event;
	}

	/**
	 * Update entry in database to prohibit creation of the same CatalogEvent
	 * multiple times
	 */
	private void updateCatalogEventTimerEntry(CatalogEventTimerEntry currentEntry, LocalDateTime newTime) {
		currentEntry.setLastCheckDate(Date.from(newTime.atZone(ZoneId.systemDefault()).toInstant()));

		this.repository.save(currentEntry);

	}
}
