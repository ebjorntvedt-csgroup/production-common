package esa.s1pdgs.cpoc.mqi.server.consumption.kafka.listener;

import static java.util.Collections.singletonList;

import java.util.Collection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.mqi.server.service.MessagePersistence;

/**
 * Rebalance listener when messages are in memory
 * 
 * @author Viveris Technologies
 */
public class MemoryConsumerAwareRebalanceListener
        implements ConsumerAwareRebalanceListener {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(MemoryConsumerAwareRebalanceListener.class);

    /**
     * Service for checking if a message is processing or not by another
     */
    private final MessagePersistence messagePersistence;

    /**
     * Group name
     */
    private final String group;

    /**
     * Default mode
     */
    private final int defaultMode;

    /**
     * Default constructor
     */
    public MemoryConsumerAwareRebalanceListener(
            final MessagePersistence messagePersistence, final String group,
            final int defaultMode) {
        super();
        this.messagePersistence = messagePersistence;
        this.group = group;
        this.defaultMode = defaultMode;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the defaultMode
     */
    public int getDefaultMode() {
        return defaultMode;
    }

    /**
     * 
     */
    @Override
    public void onPartitionsRevokedBeforeCommit(final Consumer<?, ?> consumer,
            final Collection<TopicPartition> partitions) {
        LOGGER.info(
                "[MONITOR] [rebalance] onPartitionsRevokedBeforeCommit call");
    }

    /**
     * 
     */
    @Override
    public void onPartitionsRevokedAfterCommit(final Consumer<?, ?> consumer,
            final Collection<TopicPartition> partitions) {
        LOGGER.info("[MONITOR] [rebalance] onPartitionsRevokedAfterCommit call");
    }

    /**
     * 
     */
    @Override
    public void onPartitionsAssigned(final Consumer<?, ?> consumer,
            final Collection<TopicPartition> partitions) {
        LOGGER.info("[MONITOR] [rebalance] onPartitionsAssigned call");
        // We seek the consumer on the right offset
        for (TopicPartition topicPartition : partitions) {
            LOGGER.debug(
                    "[MONITOR] [rebalance] Current offset for topic {} in partition {} is {} committed offset is -> {}",
                    topicPartition.topic(), topicPartition.partition(),
                    consumer.position(topicPartition),
                    consumer.committed(topicPartition));
            long startingOffset = defaultMode;
            try {
                startingOffset =
                        messagePersistence.getEarliestOffset(topicPartition.topic(), topicPartition.partition(), group);
            } catch (AbstractCodedException ace) {
                LOGGER.error(
                        "[MONITOR] [rebalance] Exception occurred, set default mode {}: {}",
                        defaultMode, ace.getLogMessage());
            } catch (Exception e) {
                LOGGER.error(
                        "[MONITOR] [rebalance] Exception occurred, set default mode {}: {}",
                        defaultMode, LogUtils.toString(e));
            }
            if (startingOffset == -3) {
                LOGGER.info("[MONITOR] [rebalance] Leaving topic {} partition {} alone", topicPartition.topic(), topicPartition.partition());
            } else if (startingOffset == -2) {
                LOGGER.info("[MONITOR] [rebalance] Setting offset of topic {} partition {} to end", topicPartition.topic(), topicPartition.partition());
                consumer.seekToEnd(singletonList(topicPartition));
            } else if (startingOffset == -1) {
                LOGGER.info("[MONITOR] [rebalance] Setting offset of topic {} partition {} to beginning", topicPartition.topic(), topicPartition.partition());
                consumer.seekToBeginning(singletonList(topicPartition));
            } else {
                LOGGER.info("[MONITOR] [rebalance] Resetting offset of topic {} partition {} to {}", topicPartition.topic(), topicPartition.partition(),
                        startingOffset);
                consumer.seek(topicPartition, startingOffset);
            }
        }
    }
}
