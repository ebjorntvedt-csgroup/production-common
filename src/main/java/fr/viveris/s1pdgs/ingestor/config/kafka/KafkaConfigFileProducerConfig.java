package fr.viveris.s1pdgs.ingestor.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import fr.viveris.s1pdgs.ingestor.model.dto.KafkaConfigFileDto;
import fr.viveris.s1pdgs.ingestor.services.kafka.KafkaConfigFileProducer;

/**
 * Kafka producer dedicated to the topic "metadata"
 * @author Cyrielle Gailliard
 *
 */
@Configuration
@EnableKafka
public class KafkaConfigFileProducerConfig {

	/**
	 * URI of KAFKA cluster
	 */
	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	/**
	 * Producer configuration
	 * @return
	 */
	@Bean
	public Map<String, Object> producerConfigFileConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
		props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
		return props;
	}

	/**
	 * Producer factory
	 * @return
	 */
	@Bean
	public ProducerFactory<String, KafkaConfigFileDto> producerConfigFileFactory() {
		return new DefaultKafkaProducerFactory<>(producerConfigFileConfigs());
	}

	/**
	 * KAFKA template, the producer wrapper (the producer factory is provided in it)
	 * @return
	 */
	@Bean
	public KafkaTemplate<String, KafkaConfigFileDto> kafkaConfigFileTemplate() {
		return new KafkaTemplate<>(producerConfigFileFactory());
	}

	/**
	 * KAFKA producer
	 * @return
	 */
	@Bean
	public KafkaConfigFileProducer senderMetadata() {
		return new KafkaConfigFileProducer();
	}
}
