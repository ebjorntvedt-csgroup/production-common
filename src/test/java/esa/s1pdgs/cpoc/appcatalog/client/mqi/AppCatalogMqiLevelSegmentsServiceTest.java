package esa.s1pdgs.cpoc.appcatalog.client.mqi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiLevelSegmentMessageDto;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogMqiGetApiError;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogMqiNextApiError;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelSegmentDto;

/**
 * Test the REST service ErrorService
 * 
 * @author Viveris Technologies
 */
public class AppCatalogMqiLevelSegmentsServiceTest {

    /**
     * To check the raised custom exceptions
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Rest template
     */
    @Mock
    private RestTemplate restTemplate;

    /**
     * Service to test
     */
    private AppCatalogMqiLevelSegmentsService service;

    /**
     * DTO
     */
    private MqiLevelSegmentMessageDto message1;
    private MqiLevelSegmentMessageDto message2;
    private List<MqiGenericMessageDto<LevelSegmentDto>> messages;
    private List<MqiLevelSegmentMessageDto> messages2;
    private LevelSegmentDto dto =
            new LevelSegmentDto("name", "keyobs", ProductFamily.L0_SLICE, "FAST");

    /**
     * Initialization
     */
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        service = new AppCatalogMqiLevelSegmentsService(restTemplate, "uri", 2,
                500);

        message1 = new MqiLevelSegmentMessageDto(1234, "topic", 2, 9876, dto);
        message2 = new MqiLevelSegmentMessageDto(1235, "topic", 2, 9877, dto);

        messages = Arrays.asList(message1, message2);
        messages2 = Arrays.asList(message1, message2);
    }

    /**
     * Test next when no response from the rest server
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextWhenNoResponse() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
                .when(restTemplate).exchange(Mockito.any(URI.class),
                        Mockito.any(HttpMethod.class), Mockito.isNull(),
                        Mockito.any(ParameterizedTypeReference.class));

        thrown.expect(AppCatalogMqiNextApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.LEVEL_SEGMENTS)));

        service.next("pod-name");
    }

    /**
     * Test next when the rest server respond an error
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextWhenResponseKO() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.INTERNAL_SERVER_ERROR),
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                                Mockito.any(URI.class),
                                Mockito.any(HttpMethod.class), Mockito.isNull(),
                                Mockito.any(ParameterizedTypeReference.class));

        thrown.expect(AppCatalogMqiNextApiError.class);
        thrown.expectMessage(
                containsString("" + HttpStatus.INTERNAL_SERVER_ERROR.value()));

        service.next("pod-name");
    }

    /**
     * Test the max retries applied before launching an exception
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMaxRetries() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.INTERNAL_SERVER_ERROR),
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                                Mockito.any(URI.class),
                                Mockito.any(HttpMethod.class), Mockito.isNull(),
                                Mockito.any(ParameterizedTypeReference.class));

        String uriStr = "uri/mqi/level_segments/next";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(uriStr).queryParam("pod", "pod-name");
        URI expectedUri = builder.build().toUri();

        try {
            service.next("pod-name");
            fail("An exception shall be raised");
        } catch (AppCatalogMqiNextApiError mpee) {
            verify(restTemplate, times(2)).exchange(Mockito.eq(expectedUri),
                    Mockito.eq(HttpMethod.GET), Mockito.eq(null), Mockito.eq(
                            new ParameterizedTypeReference<List<MqiLevelSegmentMessageDto>>() {
                            }));
            verifyNoMoreInteractions(restTemplate);
        }
    }

    /**
     * Test next when the first time fails and the second works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNext1() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<List<MqiLevelSegmentMessageDto>>(messages2,
                        HttpStatus.OK)).when(restTemplate).exchange(
                                Mockito.any(URI.class),
                                Mockito.any(HttpMethod.class), Mockito.isNull(),
                                Mockito.any(ParameterizedTypeReference.class));

        String uriStr = "uri/mqi/level_segments/next";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(uriStr).queryParam("pod", "pod-name");
        URI expectedUri = builder.build().toUri();

        List<MqiGenericMessageDto<LevelSegmentDto>> result =
                service.next("pod-name");
        assertEquals(messages, result);
        verify(restTemplate, times(2)).exchange(Mockito.eq(expectedUri),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null), Mockito.eq(
                        new ParameterizedTypeReference<List<MqiLevelSegmentMessageDto>>() {
                        }));
        verifyNoMoreInteractions(restTemplate);
    }

    /**
     * Test next when the first time works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNext2() throws AbstractCodedException {
        doReturn(new ResponseEntity<List<MqiLevelSegmentMessageDto>>(messages2,
                HttpStatus.OK)).when(restTemplate).exchange(
                        Mockito.any(URI.class), Mockito.any(HttpMethod.class),
                        Mockito.isNull(),
                        Mockito.any(ParameterizedTypeReference.class));

        String uriStr = "uri/mqi/level_segments/next";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(uriStr).queryParam("pod", "pod-name");
        URI expectedUri = builder.build().toUri();

        List<MqiGenericMessageDto<LevelSegmentDto>> result =
                service.next("pod-name");
        assertEquals(messages, result);
        verify(restTemplate, times(1)).exchange(Mockito.eq(expectedUri),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null), Mockito.eq(
                        new ParameterizedTypeReference<List<MqiLevelSegmentMessageDto>>() {
                        }));
        verifyNoMoreInteractions(restTemplate);
    }

    /**
     * Test next when the server returns an empty body
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextEmptyBody() throws AbstractCodedException {
        doReturn(new ResponseEntity<List<MqiLevelSegmentMessageDto>>(
                HttpStatus.OK)).when(restTemplate).exchange(
                        Mockito.any(URI.class), Mockito.any(HttpMethod.class),
                        Mockito.isNull(),
                        Mockito.any(ParameterizedTypeReference.class));

        String uriStr = "uri/mqi/level_segments/next";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(uriStr).queryParam("pod", "pod-name");
        URI expectedUri = builder.build().toUri();

        List<MqiGenericMessageDto<LevelSegmentDto>> result =
                service.next("pod-name");
        assertEquals(0, result.size());
        verify(restTemplate, times(1)).exchange(Mockito.eq(expectedUri),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null), Mockito.eq(
                        new ParameterizedTypeReference<List<MqiLevelSegmentMessageDto>>() {
                        }));
        verifyNoMoreInteractions(restTemplate);
    }

    /**
     * Test next when no response from the rest server
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetWhenNoResponse() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
                .when(restTemplate).exchange(Mockito.anyString(),
                        Mockito.any(HttpMethod.class), Mockito.any(),
                        Mockito.any(Class.class));

        thrown.expect(AppCatalogMqiGetApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.LEVEL_SEGMENTS)));
        thrown.expect(hasProperty("uri", is("uri/mqi/level_segments/1234")));

        service.get(1234);
    }

    /**
     * Test next when the rest server respond an error
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetWhenResponseKO() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<MqiLevelSegmentMessageDto>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<MqiLevelSegmentMessageDto>(
                        HttpStatus.INTERNAL_SERVER_ERROR),
                new ResponseEntity<MqiLevelSegmentMessageDto>(
                        HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class), Mockito.any(),
                                Mockito.any(Class.class));

        thrown.expect(AppCatalogMqiGetApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.LEVEL_SEGMENTS)));
        thrown.expect(hasProperty("uri", is("uri/mqi/level_segments/1234")));
        thrown.expectMessage(
                containsString("" + HttpStatus.INTERNAL_SERVER_ERROR.value()));

        service.get(1234);
    }

    /**
     * Test next when the rest server respond an error
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetWhenResponseNotFound() throws AbstractCodedException {
        doReturn(new ResponseEntity<MqiLevelSegmentMessageDto>(
                HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                        Mockito.anyString(), Mockito.any(HttpMethod.class),
                        Mockito.any(), Mockito.any(Class.class));

        thrown.expect(AppCatalogMqiGetApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.LEVEL_SEGMENTS)));
        thrown.expect(hasProperty("uri", is("uri/mqi/level_segments/1234")));
        thrown.expectMessage(containsString("Message not found"));

        service.get(1234);
    }

    /**
     * Test next when the first time fails and the second works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGet1() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<MqiLevelSegmentMessageDto>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<MqiLevelSegmentMessageDto>(message1,
                        HttpStatus.OK)).when(restTemplate).exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class), Mockito.any(),
                                Mockito.any(Class.class));

        MqiGenericMessageDto<LevelSegmentDto> ret = service.get(1234);
        assertEquals(ret, message1);
        verify(restTemplate, times(2)).exchange(
                Mockito.eq("uri/mqi/level_segments/1234"),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null),
                Mockito.eq(MqiLevelSegmentMessageDto.class));
        verifyNoMoreInteractions(restTemplate);
    }

    /**
     * Test next when the first time works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGet2() throws AbstractCodedException {
        doReturn(new ResponseEntity<MqiLevelSegmentMessageDto>(message1,
                HttpStatus.OK)).when(restTemplate).exchange(Mockito.anyString(),
                        Mockito.any(HttpMethod.class), Mockito.any(),
                        Mockito.any(Class.class));

        MqiGenericMessageDto<LevelSegmentDto> ret = service.get(1234);
        assertEquals(ret, message1);
        verify(restTemplate, times(1)).exchange(
                Mockito.eq("uri/mqi/level_segments/1234"),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null),
                Mockito.eq(MqiLevelSegmentMessageDto.class));
        verifyNoMoreInteractions(restTemplate);
    }
}
