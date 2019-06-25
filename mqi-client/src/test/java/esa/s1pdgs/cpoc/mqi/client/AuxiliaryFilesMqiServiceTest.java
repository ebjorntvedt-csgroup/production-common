package esa.s1pdgs.cpoc.mqi.client;

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiNextApiError;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.AuxiliaryFilesMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

/**
 * Test the REST service ErrorService
 * 
 * @author Viveris Technologies
 */
public class AuxiliaryFilesMqiServiceTest {

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
    private AuxiliaryFilesMqiService service;

    /**
     * DTO
     */
    private AuxiliaryFilesMessageDto message;

    /**
     * Initialization
     */
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        service = new AuxiliaryFilesMqiService(restTemplate, "uri", 2, 500);

        message = new AuxiliaryFilesMessageDto(123, "input-key",
                new ProductDto("name", "keyobs", ProductFamily.AUXILIARY_FILE));
    }

    /**
     * Test publish when no response from the rest server
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextWhenNoResponse() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
                .when(restTemplate).exchange(Mockito.anyString(),
                        Mockito.any(HttpMethod.class),
                        Mockito.isNull(),
                        Mockito.any(Class.class));

        thrown.expect(MqiNextApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.AUXILIARY_FILES)));

        service.next();
    }

    /**
     * Test publish when the rest server respond an error
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextWhenResponseKO() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.INTERNAL_SERVER_ERROR),
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class),
                                Mockito.isNull(),
                                Mockito.any(Class.class));

        thrown.expect(MqiNextApiError.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.AUXILIARY_FILES)));
        thrown.expectMessage(
                containsString("" + HttpStatus.INTERNAL_SERVER_ERROR.value()));

        service.next();
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
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.INTERNAL_SERVER_ERROR),
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.NOT_FOUND)).when(restTemplate).exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class),
                                Mockito.isNull(),
                                Mockito.any(Class.class));

        try {
            service.next();
            fail("An exception shall be raised");
        } catch (MqiNextApiError mpee) {
            verify(restTemplate, times(2)).exchange(
                    Mockito.eq("uri/messages/auxiliary_files/next"),
                    Mockito.eq(HttpMethod.GET), Mockito.eq(null),
                    Mockito.eq(AuxiliaryFilesMessageDto.class));
            verifyNoMoreInteractions(restTemplate);
        }
    }

    /**
     * Test publish when the first time fails and the second works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNext1() throws AbstractCodedException {
        doReturn(
                new ResponseEntity<AuxiliaryFilesMessageDto>(
                        HttpStatus.BAD_GATEWAY),
                new ResponseEntity<AuxiliaryFilesMessageDto>(message,
                        HttpStatus.OK)).when(restTemplate).exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class),
                                Mockito.isNull(),
                                Mockito.any(Class.class));

        GenericMessageDto<ProductDto> ret = service.next();
        assertEquals(message, ret);
        verify(restTemplate, times(2)).exchange(
                Mockito.eq("uri/messages/auxiliary_files/next"),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null),
                Mockito.eq(AuxiliaryFilesMessageDto.class));
        verifyNoMoreInteractions(restTemplate);
    }

    /**
     * Test publish when the first time works
     * 
     * @throws AbstractCodedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNext2() throws AbstractCodedException {
        doReturn(new ResponseEntity<AuxiliaryFilesMessageDto>(message, HttpStatus.OK))
                .when(restTemplate).exchange(Mockito.anyString(),
                        Mockito.any(HttpMethod.class),
                        Mockito.isNull(),
                        Mockito.any(Class.class));

        GenericMessageDto<ProductDto> ret = service.next();
        assertEquals(message, ret);
        verify(restTemplate, times(1)).exchange(
                Mockito.eq("uri/messages/auxiliary_files/next"),
                Mockito.eq(HttpMethod.GET), Mockito.eq(null),
                Mockito.eq(AuxiliaryFilesMessageDto.class));
        verifyNoMoreInteractions(restTemplate);
    }
}
