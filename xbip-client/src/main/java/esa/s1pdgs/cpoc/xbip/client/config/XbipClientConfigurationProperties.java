package esa.s1pdgs.cpoc.xbip.client.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource({"classpath:${xbipConfigFile:xbip.properties}"})
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "xbip")
public class XbipClientConfigurationProperties {
	public static class XbipHostConfiguration {
		private String serverName;
		private String user;
		private String pass;
		private boolean trustSelfSignedCertificate = false;
		
		public String getServerName() {
			return serverName;
		}

		public void setServerName(final String serverName) {
			this.serverName = serverName;
		}

		public boolean isTrustSelfSignedCertificate() {
			return trustSelfSignedCertificate;
		}

		public void setTrustSelfSignedCertificate(final boolean trustSelfSignedCertificate) {
			this.trustSelfSignedCertificate = trustSelfSignedCertificate;
		}

		public String getUser() {
			return user;
		}

		public void setUser(final String user) {
			this.user = user;
		}

		public String getPass() {
			return pass;
		}

		public void setPass(final String pass) {
			this.pass = pass;
		}

		@Override
		public String toString() {
			return "XbipHostConfiguration [serverName=" + serverName + ", user=" + user + 
					", pass=****, trustSelfSignedCertificate=" + trustSelfSignedCertificate + "]";
		}
		
	}
	
	private String proxyHost;
	private int proxyPort = 80;

	private List<XbipHostConfiguration> hostConfigs;

	public List<XbipHostConfiguration> getHostConfigs() {
		return hostConfigs;
	}

	public void setHostConfigs(final List<XbipHostConfiguration> hostConfigs) {
		this.hostConfigs = hostConfigs;
	}
	
	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(final String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(final int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@Override
	public String toString() {
		return "XbipClientConfigurationProperties [proxyHost=" + proxyHost + ", proxyPort=" + proxyPort
				+ ", hostConfigs=" + hostConfigs + "]";
	}
}
