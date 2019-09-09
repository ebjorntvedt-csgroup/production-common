package esa.s1pdgs.cpoc.queuewatcher.kafka;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaLoggerService {
	private static final Logger LOGGER = LogManager.getLogger(KafkaLoggerService.class);
	
	@PostConstruct
	public void init() {
		LOGGER.info("Starting kafka logger service...");
	}
	@KafkaListener(topics = "t-pdgs-edrs-sessions", groupId = "foo")
	public void listen(String message) {
	    System.out.println("Received Messasge in group foo: " + message);
	}
}
