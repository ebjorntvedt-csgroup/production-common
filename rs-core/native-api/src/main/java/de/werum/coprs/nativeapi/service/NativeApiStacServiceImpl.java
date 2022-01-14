package de.werum.coprs.nativeapi.service;

import static de.werum.coprs.nativeapi.service.mapping.PripOdataEntityProperties.ContentDate;
import static de.werum.coprs.nativeapi.service.mapping.PripOdataEntityProperties.End;
import static de.werum.coprs.nativeapi.service.mapping.PripOdataEntityProperties.Start;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import de.werum.coprs.nativeapi.config.NativeApiProperties;
import de.werum.coprs.nativeapi.rest.model.stac.StacItemCollection;
import de.werum.coprs.nativeapi.service.exception.NativeApiBadRequestException;
import de.werum.coprs.nativeapi.service.exception.NativeApiException;
import de.werum.coprs.nativeapi.service.mapping.PripToStacMapper;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.common.utils.StringUtil;

@Service
public class NativeApiStacServiceImpl implements NativeApiStacService {

	private static final Logger LOG = LogManager.getLogger(NativeApiStacServiceImpl.class);

	public static final Pattern RFC3339_DATE_PATTERN = Pattern
			.compile("^(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)(T|t)(\\d\\d):(\\d\\d):(\\d\\d)([.]\\d+)?(Z|z|([-+])(\\d\\d):(\\d\\d))$");

	private final NativeApiProperties apiProperties;
	private final RestTemplate restTemplate;
	private final URL internalPripUrl;
	private final URI externalPripUrl;

	@Autowired
	public NativeApiStacServiceImpl(final NativeApiProperties apiProperties, final RestTemplate restTemplate) {
		this.apiProperties = apiProperties;
		this.restTemplate = restTemplate;
		this.internalPripUrl = buildInternalPripUrl(apiProperties);
		this.externalPripUrl = buildExternalPripUrl(apiProperties);
	}

	@Override
	public StacItemCollection find(final String datetime) {
		final String odataQueryUrl = buildPripQueryUrl(this.internalPripUrl, datetime);
		LOG.debug("sending PRIP request: {}", odataQueryUrl);
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		final HttpEntity<String> requestEntity = new HttpEntity<>(null, httpHeaders);
		final ResponseEntity<String> responseEntity = this.restTemplate.exchange(odataQueryUrl, HttpMethod.GET, requestEntity, String.class);

		return mapResponse(responseEntity, this.externalPripUrl);
	}

	static StacItemCollection mapResponse(final ResponseEntity<String> responseEntity, final URI externalPripUrl) {
		// TODO: check for status 200 and containing "value", then map, else return error
		if (null != responseEntity) {

			final String responseBody = responseEntity.getBody();
			if (null != responseBody) {
				//LOG.debug(String.format("PRIP response body: %s", responseBody.length() > 256 ? responseBody.substring(0, 252) + "..." : responseBody));
				final JSONObject jsonObject = new JSONObject(responseBody);
				// TODO: check is odata and contains value
				try {
					return PripToStacMapper.mapFromPripOdataJson(jsonObject, externalPripUrl);
				} catch (JSONException | URISyntaxException e) {
					throw new NativeApiException("error mapping PRIP response to STAC item collection", e, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}

		return null;
	}

	static String buildPripQueryUrl(final URL pripUrl, final String datetime) {
		return String.format("%s%s%s", pripUrl, "/odata/v1/Products?", createPripFilterTerm(datetime));
	}

	static String createPripFilterTerm(final String datetime) {
		final String odataDatetimeFilterTerm = createDatetimeOdataFilterTerm(datetime);

		if (StringUtil.isNotBlank(odataDatetimeFilterTerm)) {
			return String.format("$filter=%s", odataDatetimeFilterTerm);
		}

		return "";
	}

	static String createDatetimeOdataFilterTerm(final String datetimeStr) {
		if (StringUtil.isNotBlank(datetimeStr)) {
			final String[] datetimeStrings = datetimeStr.toUpperCase().split("/");

			if (datetimeStrings.length == 1) {
				final String singleDatetimeStr = datetimeStrings[0];

				if ("".equals(singleDatetimeStr) || "..".equals(singleDatetimeStr)) {
					throw new NativeApiBadRequestException(String.format("invalid datetime format, single value must be a datetime value: %s", datetimeStr));
				}

				final String singleOdataDatetime = convertDatetimeToOdataFormat(singleDatetimeStr);
				// ContentDate/Start le 2021-12-21T12:21:12.000Z and ContentDate/End ge 2021-12-21T12:21:12.000Z
				return String.format("%s/%s le %s and %s/%s ge %s", ContentDate, Start, singleOdataDatetime, ContentDate, End, singleOdataDatetime);

			} else if (datetimeStrings.length == 2) {
				final String datetimeLowerBoundaryStr = datetimeStrings[0];
				final String datetimeUpperBoundaryStr = datetimeStrings[1];
				final boolean openStart = "".equals(datetimeLowerBoundaryStr) || "..".equals(datetimeLowerBoundaryStr);
				final boolean openEnd = "".equals(datetimeUpperBoundaryStr) || "..".equals(datetimeUpperBoundaryStr);

				if (openStart && openEnd) {
					throw new NativeApiBadRequestException(
							String.format("invalid datetime value, at least one boundary (lower or upper) required: %s", datetimeStr));
				}

				if (openStart) {
					final String odataDatetimeUpperBoundary = convertDatetimeToOdataFormat(datetimeUpperBoundaryStr);
					// ContentDate/Start le 2021-12-21T12:21:12.000Z
					return String.format("%s/%s le %s", ContentDate, Start, odataDatetimeUpperBoundary);

				} else if (openEnd) {
					final String odataDatetimeLowerBoundary = convertDatetimeToOdataFormat(datetimeLowerBoundaryStr);
					// ContentDate/End ge 2021-12-21T00:00:00.000Z
					return String.format("%s/%s ge %s", ContentDate, End, odataDatetimeLowerBoundary);

				} else {
					final LocalDateTime datetimeLowerBoundary = convertDatetime(datetimeLowerBoundaryStr);
					final LocalDateTime datetimeUpperBoundary = convertDatetime(datetimeUpperBoundaryStr);

					if (datetimeUpperBoundary.isBefore(datetimeLowerBoundary)) {
						throw new NativeApiBadRequestException(String.format("lower boundary %s must not be smaller than upper boundary %s",
								datetimeLowerBoundaryStr, datetimeUpperBoundaryStr));
					}

					final String odataDatetimeLowerBoundary = formatToOdataFormat(datetimeLowerBoundary);
					final String odataDatetimeUpperBoundary = formatToOdataFormat(datetimeUpperBoundary);

					// ContentDate/Start le 2021-12-21T12:21:12.000Z and ContentDate/End ge 2021-12-21T00:00:00.000Z
					return String.format("%s/%s le %s and %s/%s ge %s", ContentDate, Start, odataDatetimeUpperBoundary, ContentDate, End, odataDatetimeLowerBoundary);
				}
			} else {
				throw new NativeApiBadRequestException(String.format("invalid datetime format: %s", datetimeStr));
			}
		}

		return "";
	}

	static LocalDateTime convertDatetime(final String rfc3339DatetimeStr) {
		final Matcher matcher = RFC3339_DATE_PATTERN.matcher(rfc3339DatetimeStr);
		if (!matcher.matches()) {
			throw new NativeApiBadRequestException(String.format("invalid datetime format: %s", rfc3339DatetimeStr));
		}

		try {
			return DateUtils.parse(rfc3339DatetimeStr);
		} catch (final RuntimeException e) {
			throw new NativeApiBadRequestException(String.format("invalid datetime format: %s", rfc3339DatetimeStr));
		}
	}

	static String formatToOdataFormat(final LocalDateTime datetime) {
		try {
			return DateUtils.formatToOdataDateTimeFormat(datetime);
		} catch (final RuntimeException e) {
			throw new NativeApiException(String.format("error formatting datetime to OData format: %s", e.getMessage()), e, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	static String convertDatetimeToOdataFormat(final String rfc3339DatetimeStr) {
		return formatToOdataFormat(convertDatetime(rfc3339DatetimeStr));
	}

	static URL buildInternalPripUrl(final NativeApiProperties apiProperties) {
		return buildPripUrl(apiProperties.getPripProtocol(), apiProperties.getPripHost(), apiProperties.getPripPort());
	}

	static URI buildExternalPripUrl(final NativeApiProperties apiProperties) {
		try {
			return buildPripUrl(Objects.requireNonNull(apiProperties).getExternalPripProtocol(), apiProperties.getExternalPripHost(),
					apiProperties.getExternalPripPort()).toURI();
		} catch (final Exception e) {
			final String msg = String.format(
					"could not initialize PRIP URL for external interface, metadata/download links will not be added to responses [protocol (%s), host (%s) and port (%s)]: %s",
					apiProperties.getExternalPripProtocol(), apiProperties.getExternalPripHost(), apiProperties.getExternalPripPort(), e.getMessage());
			LOG.warn(msg);
			return null;
		}
	}

	static URL buildPripUrl(final String protocol, final String host, final int port) {
		try {
			return UriComponentsBuilder
					.fromHttpUrl(String.format("%s://%s:%d",
							Objects.requireNonNull(protocol),
							Objects.requireNonNull(host),
							port))
					.build().toUri().toURL();
		} catch (final MalformedURLException e) {
			final String msg = String.format("could not initialize PRIP URL; protocol (%s), host (%s) and port (%s) must be valid: %s",
					protocol, host, port, e.getMessage());
			throw new IllegalArgumentException(msg, e);
		}
	}

}
