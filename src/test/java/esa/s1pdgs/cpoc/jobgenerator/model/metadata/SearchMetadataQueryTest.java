package esa.s1pdgs.cpoc.jobgenerator.model.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.jobgenerator.model.metadata.SearchMetadataQuery;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * 
 */
public class SearchMetadataQueryTest {
	
	/**
	 * Test toString
	 */
	@Test
	public void testToString() {
		SearchMetadataQuery obj = new SearchMetadataQuery(12, "retrievalMode", 0.0, 1.5, "productType", ProductFamily.L0_SLICE, "NRT");
		
		String str = obj.toString();
		assertTrue(str.contains("identifier: 12"));
		assertTrue(str.contains("retrievalMode: retrievalMode"));
		assertTrue(str.contains("deltaTime0: 0.0"));
		assertTrue(str.contains("deltaTime1: 1.5"));
		assertTrue(str.contains("productType: productType"));
        assertTrue(str.contains("productFamily: L0_SLICE"));
        assertTrue(str.contains("mode: NRT"));
		
		obj.setIdentifier(1);
		obj.setRetrievalMode("retrievalode");
		obj.setDeltaTime0(2.0);
		obj.setDeltaTime1(1.2);
		obj.setProductType("productype");
		obj.setProductFamily(ProductFamily.L1_SLICE);
		obj.setMode("FAST");
		
		str = obj.toString();
		assertTrue(str.contains("identifier: 1"));
		assertTrue(str.contains("retrievalMode: retrievalode"));
		assertTrue(str.contains("deltaTime0: 2.0"));
		assertTrue(str.contains("deltaTime1: 1.2"));
		assertTrue(str.contains("productType: productype"));
        assertTrue(str.contains("productFamily: L1_SLICE"));
        assertTrue(str.contains("mode: FAST"));
		
		String log = obj.toLogMessage();
		assertEquals("1|retrievalode|2.0|1.2|productype|L1_SLICE|FAST", log);
	}

	/**
	 * Check equals and hascode methods
	 */
	@Test
	public void testEquals() {
		EqualsVerifier.forClass(SearchMetadataQuery.class).usingGetClass().suppress(Warning.NONFINAL_FIELDS).verify();
	}

}
