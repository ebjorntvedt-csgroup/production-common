package esa.s1pdgs.cpoc.inbox.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("inbox")
public class InboxPollingConfigurationProperties {
	
	private long pollingIntervalMs;
	
	private List<InboxConfiguration> polling = new ArrayList<>();

	public List<InboxConfiguration> getPolling() {
		return polling;
	}

	public void setPolling(List<InboxConfiguration> polling) {
		this.polling = polling;
	}
	
	public long getPollingIntervalMs() {
		return pollingIntervalMs;
	}

	public void setPollingIntervalMs(long pollingIntervalMs) {
		this.pollingIntervalMs = pollingIntervalMs;
	}

	@Override
	public String toString() {
		return "InboxPollingConfigurationProperties [pollingIntervalMs=" + pollingIntervalMs + ", polling=" + polling + "]";
	}
}
