package esa.s1pdgs.cpoc.dlq.manager.service;

import static org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder.X_EXCEPTION_MESSAGE;
import static org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder.X_EXCEPTION_STACKTRACE;
import static org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder.X_ORIGINAL_TOPIC;
import static org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder.X_ORIGINAL_TIMESTAMP;
import static org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder.X_ORIGINAL_TIMESTAMP_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import esa.s1pdgs.cpoc.appcatalog.common.FailedProcessing;
import esa.s1pdgs.cpoc.dlq.manager.configuration.DlqManagerConfigurationProperties;
import esa.s1pdgs.cpoc.dlq.manager.model.routing.RoutingTable;
import esa.s1pdgs.cpoc.dlq.manager.model.routing.Rule;

public class DlqManagerService implements Function<Message<byte[]>, List<Message<byte[]>>> {

	private static final Logger LOGGER = LogManager.getLogger(DlqManagerService.class);
	
	public static String X_ROUTE_TO = "x-route-to";
	
	private final RoutingTable routingTable;
	private final String parkingLotTopic;
	
	public DlqManagerService(final RoutingTable routingTable,
			final DlqManagerConfigurationProperties properties) {
		this.routingTable = routingTable;
		this.parkingLotTopic = properties.getParkingLotTopic();
	}
	
	@Override
	public List<Message<byte[]>> apply(Message<byte[]> message) {
		final List<Message<byte[]>> result = new ArrayList<>();
		LOGGER.debug("Receiving new message: {}", message);
		final String originalTopic = new String(message.getHeaders().get(X_ORIGINAL_TOPIC, byte[].class),
				StandardCharsets.UTF_8);
		LOGGER.debug("DLQ message topic: {}", originalTopic);
		final String exceptionMessage = new String(message.getHeaders().get(X_EXCEPTION_MESSAGE, byte[].class),
				StandardCharsets.UTF_8);
		final String payload = new String(message.getPayload());
		final JSONObject json = new JSONObject(payload);
		
		LOGGER.info("Receiving DLQ message on topic {} with error: {}", originalTopic, exceptionMessage);
		LOGGER.info("Payload: {}", json);
		
		Optional<Rule> optRule = routingTable.findRule(exceptionMessage);
		if (optRule.isEmpty()) {
			LOGGER.info("No matching rule found");
			LOGGER.info("Route to {}", parkingLotTopic);
			result.add(newParkingLotMessage(json, message.getHeaders()));
		} else {
			Rule rule = optRule.get();
			LOGGER.info("Found rule {}: {}", rule.getActionType().name(), rule.getErrorTitle());
			switch(rule.getActionType()) {
				case DELETE:
					LOGGER.info("Ignoring message");
					break;
				case RESTART:
					final String targetTopic = "".equals(rule.getTargetTopic()) ? originalTopic : rule.getTargetTopic();
					int retryCounter = json.getInt("retryCounter");
					if (retryCounter < rule.getMaxRetry()) {
						json.put("retryCounter", ++retryCounter);
						LOGGER.info("Route to {} (retry {}/{})", targetTopic, retryCounter, rule.getMaxRetry());
						result.add(MessageBuilder.withPayload(json.toString().getBytes(StandardCharsets.UTF_8))
								.setHeader(X_ROUTE_TO, targetTopic).build());
					} else {
						LOGGER.info("Route to {} ({}/{} retries used)", parkingLotTopic, retryCounter, rule.getMaxRetry());
						result.add(newParkingLotMessage(json, message.getHeaders()));
					}
					break;
				default:
					LOGGER.error("Invalid configuration");
					throw new IllegalStateException("Invalid configuration");
			}
		}
		return result;
	}
	
	private Message<byte[]> newParkingLotMessage(JSONObject payload, MessageHeaders originalMessageHeader) {
		// TODO: In future add a FailedProcessing here containing additional attributes from the originalMessageHeader, e.g. stacktrace
		final String originalTopic = new String(originalMessageHeader.get(X_ORIGINAL_TOPIC, byte[].class), StandardCharsets.UTF_8);
		final String exceptionMessage = new String(originalMessageHeader.get(X_EXCEPTION_MESSAGE, byte[].class), StandardCharsets.UTF_8);
		final String exceptionStracktrace = new String(originalMessageHeader.get(X_EXCEPTION_STACKTRACE, byte[].class), StandardCharsets.UTF_8);
		// TODO: FailedProcessing failedProcessing = new FailedProcessing(originalTopic, payload);
		
		Message<byte[]> message = MessageBuilder.withPayload(payload.toString().getBytes(StandardCharsets.UTF_8))
				.setHeader(X_ROUTE_TO, parkingLotTopic).build();
		return message;
	}
	
}
