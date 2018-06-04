package fr.viveris.s1pdgs.ingestor.kafka;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import fr.viveris.s1pdgs.ingestor.files.model.dto.KafkaConfigFileDto;

public class KafkaConfigFileProducerTest {

	/**
	 * Topic name
	 */
	private static final String TOPIC_NAME = "topic-config";

	/**
	 * KAFKA client
	 */
	@Mock
	private KafkaTemplate<String, KafkaConfigFileDto> kafkaTemplate;

	/**
	 * Service to test
	 */
	private KafkaConfigFileProducer service;

	/**
	 * Test initialization
	 */
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		service = new KafkaConfigFileProducer(kafkaTemplate, TOPIC_NAME);
	}

	/**
	 * Test constructor
	 */
	@Test
	public void testConstructor() {
		assertEquals(TOPIC_NAME, service.getKafkaTopic());
	}

	/**
	 * Test the function get product name
	 */
	@Test
	public void testGetProductName() {
		KafkaConfigFileDto dto = new KafkaConfigFileDto("product-name", "key-obs");
		assertEquals("getProductName should return the product name", "product-name", service.extractProductName(dto));
	}

}
