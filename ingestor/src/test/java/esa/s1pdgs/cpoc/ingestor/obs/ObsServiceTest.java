package esa.s1pdgs.cpoc.ingestor.obs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsFamily;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsServiceException;
import esa.s1pdgs.cpoc.obs_sdk.ObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;

/**
 * Test the ObsService
 * 
 * @author Viveris Technologies
 */
public class ObsServiceTest {

    /**
     * Mock OBS client
     */
    @Mock
    private ObsClient client;

    /**
     * Service to test
     */
    private ObsService service;

    /**
     * To check the raised custom exceptions
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Initialization
     * 
     * @throws SdkClientException
     * @throws ObsServiceException
     */
    @Before
    public void init() throws ObsServiceException, SdkClientException {
        MockitoAnnotations.initMocks(this);

        doThrow(new ObsServiceException("error 1 message")).when(client)
                .doesObjectExist(Mockito.eq(
                        new ObsObject("error-key", ObsFamily.AUXILIARY_FILE)));
        doThrow(new SdkClientException("error 2 message")).when(client)
                .doesObjectExist(Mockito.eq(
                        new ObsObject("error-key", ObsFamily.EDRS_SESSION)));
        doReturn(true).when(client).doesObjectExist(Mockito
                .eq(new ObsObject("test-key", ObsFamily.AUXILIARY_FILE)));
        doReturn(false).when(client).doesObjectExist(
                Mockito.eq(new ObsObject("test-key", ObsFamily.EDRS_SESSION)));

        doThrow(new ObsServiceException("error 1 message")).when(client)
                .uploadObject(Mockito.eq(new ObsUploadObject("error-key",
                        ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));
        doThrow(new SdkClientException("error 2 message")).when(client)
                .uploadObject(Mockito.eq(new ObsUploadObject("error-key",
                        ObsFamily.EDRS_SESSION, new File("pom.xml"))));
        doReturn(2).when(client)
                .uploadObject(Mockito.eq(new ObsUploadObject("test-key",
                        ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));
        doReturn(1).when(client)
                .uploadObject(Mockito.eq(new ObsUploadObject("test-key",
                        ObsFamily.EDRS_SESSION, new File("pom.xml"))));

        service = new ObsService(client);
    }

    /**
     * Test getObsFamily
     */
    @Test
    public void testGetObsFamily() {
        assertEquals(ObsFamily.AUXILIARY_FILE,
                service.getObsFamily(ProductFamily.AUXILIARY_FILE));
        assertEquals(ObsFamily.EDRS_SESSION,
                service.getObsFamily(ProductFamily.EDRS_SESSION));
        assertEquals(ObsFamily.UNKNOWN,
                service.getObsFamily(ProductFamily.BLANK));
    }

    /**
     * Test exist when client raise ObsServiceException
     * 
     * @throws ObjectStorageException
     */
    @Test
    public void testExistWhenException() throws ObsException {
        thrown.expect(ObsException.class);
        thrown.expect(hasProperty("key", is("error-key")));
        thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));
        thrown.expectMessage("error 1 message");
        thrown.expectCause(isA(ObsServiceException.class));

        service.exist(ProductFamily.AUXILIARY_FILE, "error-key");
    }

    /**
     * Test exist when client raise ObsServiceException
     * 
     * @throws ObjectStorageException
     */
    @Test
    public void testExistWhenException2() throws ObsException {
        thrown.expect(ObsException.class);
        thrown.expect(hasProperty("key", is("error-key")));
        thrown.expect(hasProperty("family", is(ProductFamily.EDRS_SESSION)));
        thrown.expectMessage("error 2 message");
        thrown.expectCause(isA(SdkClientException.class));

        service.exist(ProductFamily.EDRS_SESSION, "error-key");
    }

    /**
     * Test nominal case of exists
     * 
     * @throws ObjectStorageException
     * @throws ObsServiceException
     * @throws SdkClientException
     */
    @Test
    public void testNominalExist()
            throws ObsException, ObsServiceException, SdkClientException {
        boolean ret = service.exist(ProductFamily.AUXILIARY_FILE, "test-key");
        assertTrue(ret);
        verify(client, times(1)).doesObjectExist(Mockito
                .eq(new ObsObject("test-key", ObsFamily.AUXILIARY_FILE)));

        ret = service.exist(ProductFamily.EDRS_SESSION, "test-key");
        assertFalse(ret);
        verify(client, times(1)).doesObjectExist(
                Mockito.eq(new ObsObject("test-key", ObsFamily.EDRS_SESSION)));
    }

    /**
     * Test exist when client raise ObsServiceException
     * 
     * @throws ObjectStorageException
     */
    @Test
    public void testUploadFileWhenException() throws ObsException {
        thrown.expect(ObsException.class);
        thrown.expect(hasProperty("key", is("error-key")));
        thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));
        thrown.expectMessage("error 1 message");
        thrown.expectCause(isA(ObsServiceException.class));

        service.uploadFile(ProductFamily.AUXILIARY_FILE, "error-key",
                new File("pom.xml"));
    }

    /**
     * Test exist when client raise ObsServiceException
     * 
     * @throws ObjectStorageException
     */
    @Test
    public void testUploadFileWhenException2() throws ObsException {
        thrown.expect(ObsException.class);
        thrown.expect(hasProperty("key", is("error-key")));
        thrown.expect(hasProperty("family", is(ProductFamily.EDRS_SESSION)));
        thrown.expectMessage("error 2 message");
        thrown.expectCause(isA(SdkClientException.class));

        service.uploadFile(ProductFamily.EDRS_SESSION, "error-key",
                new File("pom.xml"));
    }

    /**
     * Test nominal case of exists
     * 
     * @throws ObjectStorageException
     * @throws ObsServiceException
     * @throws SdkClientException
     */
    @Test
    public void testNominalUpload() throws ObsException,
            ObsServiceException, SdkClientException {
        service.uploadFile(ProductFamily.AUXILIARY_FILE, "test-key",
                new File("pom.xml"));
        verify(client, times(1))
                .uploadObject(Mockito.eq(new ObsUploadObject("test-key",
                        ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));

        service.uploadFile(ProductFamily.EDRS_SESSION, "test-key",
                new File("pom.xml"));
        verify(client, times(1))
                .uploadObject(Mockito.eq(new ObsUploadObject("test-key",
                        ObsFamily.EDRS_SESSION, new File("pom.xml"))));
    }
}
