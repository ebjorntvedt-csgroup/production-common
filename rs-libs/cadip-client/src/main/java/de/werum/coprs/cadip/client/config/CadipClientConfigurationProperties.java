package de.werum.coprs.cadip.client.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "cadip")
public class CadipClientConfigurationProperties {
	
	public enum BearerTokenType {
		AUTHORIZATION,
		OAUTH2_ACCESS_TOKEN
	}

	public static class CadipHostConfiguration {
		private String serviceRootUri;
		private String user;
		private String pass;
		private boolean sslValidation = true;
		private String authType; // basic|oauth2|disable
		private BearerTokenType bearerTokenType = BearerTokenType.AUTHORIZATION;
		private String oauthAuthUrl;
		private String oauthClientId;
		private String oauthClientSecret;
		private boolean useHttpClientDownload = true;

		// - - - - - - - - - - - - - - - - - -

		@Override
		public String toString() {
			return "CadipHostConfiguration [serviceRootUri=" + this.serviceRootUri + ", user=" + this.user
					+ ", pass=****" + ", sslValidation=" + this.sslValidation + ", authType=" + this.authType
					+ ", oauthAuthUrl=" + this.oauthAuthUrl + ", oauthClientId=" + this.oauthClientId
					+ ", bearerTokenType=" + this.bearerTokenType + ", oauthClientSecret=" + this.oauthClientSecret
					+ ", useHttpClientDownload=" + useHttpClientDownload + "]";
		}

		// - - - - - - - - - - - - - - - - - -

		public String getServiceRootUri() {
			return this.serviceRootUri;
		}

		public void setServiceRootUri(final String serviceRootUri) {
			this.serviceRootUri = serviceRootUri;
		}

		public String getUser() {
			return this.user;
		}

		public void setUser(final String user) {
			this.user = user;
		}

		public String getPass() {
			return this.pass;
		}

		public void setPass(final String pass) {
			this.pass = pass;
		}

		public boolean isSslValidation() {
			return this.sslValidation;
		}

		public void setSslValidation(final boolean sslValidation) {
			this.sslValidation = sslValidation;
		}

		public boolean isUseHttpClientDownload() {
			return this.useHttpClientDownload;
		}

		public void setUseHttpClientDownload(final boolean useHttpClientDownload) {
			this.useHttpClientDownload = useHttpClientDownload;
		}

		public String getOauthAuthUrl() {
			return this.oauthAuthUrl;
		}

		public void setOauthAuthUrl(String oauthAuthUrl) {
			this.oauthAuthUrl = oauthAuthUrl;
		}

		public String getOauthClientId() {
			return this.oauthClientId;
		}

		public void setOauthClientId(String oauthClientId) {
			this.oauthClientId = oauthClientId;
		}

		public String getOauthClientSecret() {
			return this.oauthClientSecret;
		}

		public void setOauthClientSecret(String oauthClientSecret) {
			this.oauthClientSecret = oauthClientSecret;
		}

		public String getAuthType() {
			return this.authType;
		}

		public void setAuthType(String authType) {
			this.authType = authType;
		}

		public BearerTokenType getBearerTokenType() {
			return bearerTokenType;
		}

		public void setBearerTokenType(BearerTokenType bearerTokenType) {
			this.bearerTokenType = bearerTokenType;
		}

	}

	// --------------------------------------------------------------------------

	private String proxy;

	private Map<String, CadipHostConfiguration> hostConfigs = new HashMap<>();

	// --------------------------------------------------------------------------

	@Override
	public String toString() {
		return "CadipClientConfigurationProperties [proxy=" + this.proxy + ", hostConfigs=" + this.hostConfigs + "]";
	}

	// --------------------------------------------------------------------------

	public Map<String, CadipHostConfiguration> getHostConfigs() {
		return this.hostConfigs;
	}

	public void setHostConfigs(final Map<String, CadipHostConfiguration> hostConfigs) {
		this.hostConfigs = hostConfigs;
	}

	public String getProxy() {
		return this.proxy;
	}

	public void setProxy(final String proxy) {
		this.proxy = proxy;
	}

}
