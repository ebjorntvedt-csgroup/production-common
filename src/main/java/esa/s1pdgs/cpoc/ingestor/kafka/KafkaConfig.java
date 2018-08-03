package esa.s1pdgs.cpoc.ingestor.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import esa.s1pdgs.cpoc.mqi.model.queue.AuxiliaryFileDto;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;

/**
 * KAFKA configuration
 * 
 * @author Cyrielle Gailliard
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * URI of KAFKA cluster
     */
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
    /**
     * Ingestor group identifier for KAFKA
     */
    @Value("${kafka.group-id}")
    private String kafkaGroupId;
    /**
     * Pool timeout for consumption
     */
    @Value("${kafka.poll-timeout}")
    private long kafkaPooltimeout;

    @Value("${kafka.producer-retries}")
    protected int kafkaRetriesConfig;

    /**
     * Consumer configuration
     * 
     * @return
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        return props;
    }

    /**
     * Consumer factory
     * 
     * @return
     */
    @Bean
    public ConsumerFactory<String, AuxiliaryFileDto> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(),
                new StringDeserializer(),
                new JsonDeserializer<>(AuxiliaryFileDto.class));
    }

    /**
     * Listener containers factory
     * 
     * @return
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, AuxiliaryFileDto>> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, AuxiliaryFileDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties();
        factory.getContainerProperties().setPollTimeout(kafkaPooltimeout);
        return factory;
    }

    /**
     * Producer configuration
     * 
     * @return
     */
    @Bean
    public Map<String, Object> producerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaRetriesConfig);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    /**
     * Producer factory
     * 
     * @return
     */
    @Bean(name = "producerSessionFactory")
    public ProducerFactory<String, EdrsSessionDto> producerSessionFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    /**
     * KAFKA template, the producer wrapper (the producer factory is provided in
     * it)
     * 
     * @return
     */
    @Bean(name = "kafkaSessionTemplate")
    public KafkaTemplate<String, EdrsSessionDto> kafkaSessionTemplate() {
        return new KafkaTemplate<>(producerSessionFactory());
    }

    /**
     * Producer factory
     * 
     * @return
     */
    @Bean(name = "producerConfigFileFactory")
    public ProducerFactory<String, AuxiliaryFileDto> producerConfigFileFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    /**
     * KAFKA template, the producer wrapper (the producer factory is provided in
     * it)
     * 
     * @return
     */
    @Bean(name = "kafkaConfigFileTemplate")
    public KafkaTemplate<String, AuxiliaryFileDto> kafkaConfigFileTemplate() {
        return new KafkaTemplate<>(producerConfigFileFactory());
    }

}
