package esa.s1pdgs.cpoc.cronbased.trigger.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.common.ProductFamily;

/**
 * Configuration for the timer based metadata catalog trigger
 */
@Configuration
@ConfigurationProperties("trigger")
public class CronbasedTriggerProperties {

	public static class TimerProperties {
		private String cron;

		private ProductFamily family;

		private String satelliteIds;

		private long queryOffsetInS = 0;

		private String timeliness = "";

		public String getCron() {
			return cron;
		}

		public void setCron(String cron) {
			this.cron = cron;
		}

		public ProductFamily getFamily() {
			return family;
		}

		public void setFamily(ProductFamily family) {
			this.family = family;
		}

		public String getSatelliteIds() {
			return satelliteIds;
		}

		public void setSatelliteIds(String satelliteIds) {
			this.satelliteIds = satelliteIds;
		}

		public long getQueryOffsetInS() {
			return queryOffsetInS;
		}

		public void setQueryOffsetInS(long queryOffsetInS) {
			this.queryOffsetInS = queryOffsetInS;
		}

		public String getTimeliness() {
			return timeliness;
		}

		public void setTimeliness(String timeliness) {
			this.timeliness = timeliness;
		}
	}

	/**
	 * The config contains a map of product types mapped to their respective timer
	 * properties
	 */
	private Map<String, TimerProperties> config;

	/**
	 * The pod contains a static unique identifier to distinguish different
	 * configuration of different running applications
	 */
	private String pod;

	public Map<String, TimerProperties> getConfig() {
		return config;
	}

	public void setConfig(Map<String, TimerProperties> config) {
		this.config = config;
	}

	public String getPod() {
		return pod;
	}

	public void setPod(String pod) {
		this.pod = pod;
	}
}
