package esa.s1pdgs.cpoc.appcatalog.client.mqi;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import esa.s1pdgs.cpoc.appcatalog.rest.MqiCompressedJobMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericMessageDto;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogMqiGetApiError;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogMqiNextApiError;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.mqi.model.queue.CompressionJobDto;

public class AppCatalogMqiCompressionJobService extends GenericAppCatalogMqiService<CompressionJobDto> {

	/**
	 * Constructor
	 * 
	 * @param restTemplate
	 * @param hostUri
	 * @param maxRetries
	 * @param tempoRetryMs
	 */
	public AppCatalogMqiCompressionJobService(final RestTemplate restTemplate, final String hostUri,
			final int maxRetries, final int tempoRetryMs) {
		super(restTemplate, ProductCategory.COMPRESSED_PRODUCTS, hostUri, maxRetries, tempoRetryMs);
	}

	/**
	 * @see GenericAppCatalogMqiService#next()
	 */
	@Override
	public List<MqiGenericMessageDto<CompressionJobDto>> next(String podName) throws AbstractCodedException {
		int retries = 0;
		while (true) {
			retries++;
			String uriStr = hostUri + "/mqi/" + category.name().toLowerCase() + "/next";
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriStr).queryParam("pod", podName);
			URI uri = builder.build().toUri();
			try {
				ResponseEntity<List<MqiCompressedJobMessageDto>> response = restTemplate.exchange(uri, HttpMethod.GET,
						null, new ParameterizedTypeReference<List<MqiCompressedJobMessageDto>>() {
						});
				if (response.getStatusCode() == HttpStatus.OK) {
					List<MqiCompressedJobMessageDto> body = response.getBody();
					if (body == null) {
						return new ArrayList<>();
					} else {
						List<MqiGenericMessageDto<CompressionJobDto>> ret = new ArrayList<MqiGenericMessageDto<CompressionJobDto>>();
						ret.addAll(body);
						return ret;
					}
				} else {
					waitOrThrow(retries,
							new AppCatalogMqiNextApiError(category, "HTTP status code " + response.getStatusCode()),
							"next");
				}
			} catch (RestClientException rce) {
				waitOrThrow(retries, new AppCatalogMqiNextApiError(category,
						"RestClientException occurred: " + rce.getMessage(), rce), "next");
			}
		}
	}

	/**
	 * @see GenericAppCatalogMqiService#get(long)
	 */
	@Override
	public MqiGenericMessageDto<CompressionJobDto> get(final long messageId) throws AbstractCodedException {
		int retries = 0;
		while (true) {
			retries++;
			String uri = hostUri + "/mqi/" + category.name().toLowerCase() + "/" + messageId;
			LogUtils.traceLog(LOGGER, String.format("[uri %s]", uri));
			try {
				ResponseEntity<MqiCompressedJobMessageDto> response = restTemplate.exchange(uri, HttpMethod.GET, null,
						MqiCompressedJobMessageDto.class);
				if (response.getStatusCode() == HttpStatus.OK) {
					LogUtils.traceLog(LOGGER, String.format("[uri %s] [ret %s]", uri, response.getBody()));
					return response.getBody();
				} else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
					LogUtils.traceLog(LOGGER, String.format("[uri %s] [ret NOT_FOUND]", uri));
					throw new AppCatalogMqiGetApiError(category, uri, "Message not found");
				} else {
					waitOrThrow(retries,
							new AppCatalogMqiGetApiError(category, uri, "HTTP status code " + response.getStatusCode()),
							"ack");
				}
			} catch (RestClientException rce) {
				waitOrThrow(retries, new AppCatalogMqiGetApiError(category, uri,
						"RestClientException occurred: " + rce.getMessage(), rce), "ack");
			}
		}
	}

}
