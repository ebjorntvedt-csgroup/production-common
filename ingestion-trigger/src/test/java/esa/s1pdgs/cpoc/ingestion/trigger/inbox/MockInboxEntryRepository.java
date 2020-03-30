package esa.s1pdgs.cpoc.ingestion.trigger.inbox;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import esa.s1pdgs.cpoc.ingestion.trigger.entity.InboxEntry;

class MockInboxEntryRepository extends AbstractInboxEntryRepository
{
	private final List<InboxEntry> saved = new ArrayList<>();
	private final int expectedSaves;
	
	public MockInboxEntryRepository(int expectedSaves) {
		this.expectedSaves = expectedSaves;
	}
	
	@Override
	public <S extends InboxEntry> S save(S entity) {
		saved.add(entity);
		return entity;
	}
	
	final void verify() throws AssertionError {
		assertEquals(expectedSaves, saved.size());
	}

	@Override
	public List<InboxEntry> findByPickupURL(String pickupURL) {
		return Collections.emptyList();
	}
}