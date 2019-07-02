package esa.s1pdgs.cpoc.mdcatalog.extraction;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiAckApiError;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.mdcatalog.ProcessConfiguration;
import esa.s1pdgs.cpoc.mdcatalog.es.EsServices;
import esa.s1pdgs.cpoc.mdcatalog.extraction.obs.ObsService;
import esa.s1pdgs.cpoc.mdcatalog.status.AppStatus;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;
import esa.s1pdgs.cpoc.mqi.model.rest.AckMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.LoggerReporting;

public class GenericExtractorTest {

    /**
     * Elasticsearch services
     */
    @Mock
    protected EsServices esServices;

    /**
     * Elasticsearch services
     */
    @Mock
    protected ObsService obsService;

    /**
     * MQI service
     */
    @Mock
    private GenericMqiClient mqiService;

    /**
     * 
     */
    @Mock
    protected MetadataExtractorConfig extractorConfig;

    /**
     * Application status
     */
    @Mock
    protected AppStatus appStatus;

    /**
     * Extractor
     */
    protected GenericExtractor<ProductDto> extractor;
    
    private final ErrorRepoAppender errorAppender = ErrorRepoAppender.NULL;
    
    private final ProcessConfiguration config = new ProcessConfiguration();

    /**
     * Job to process
     */
    private GenericMessageDto<ProductDto> inputMessage;
    
    private final LoggerReporting.Factory reportingFactory = new LoggerReporting.Factory(
    		LogManager.getLogger(GenericExtractorTest.class), "TestMetadataExtraction")
    		.product("testType", "testProduct");

    /**
     * Initialization
     * 
     * @throws AbstractCodedException
     */
    @Before
    public void init() throws AbstractCodedException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(appStatus).setError(Mockito.any(),
                Mockito.anyString());
        doReturn(true).when(mqiService).ack(Mockito.any(), Mockito.any());

        inputMessage = new GenericMessageDto<ProductDto>(123, "",
                new ProductDto(
                        "S1A_AUX_CAL_V20140402T000000_G20140402T133909.SAFE",
                        "S1A_AUX_CAL_V20140402T000000_G20140402T133909.SAFE",
                        ProductFamily.L0_ACN, "NRT"));

        extractor = new LevelProductsExtractor(esServices, obsService,
                mqiService, appStatus, extractorConfig,
                (new File("./test/workDir/")).getAbsolutePath(),
                "manifest.safe", errorAppender, config, ".safe");
    }

    /**
     * Test ack when exception
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testAckNegativelyWhenException() throws AbstractCodedException {
        doThrow(new MqiAckApiError(ProductCategory.AUXILIARY_FILES, 1,
                "ack-msg", "error-ùmessage")).when(mqiService)
                        .ack(Mockito.any(), Mockito.any());
        
        extractor.ackNegatively(reportingFactory, new FailedProcessingDto(), inputMessage, "error message");

        verify(mqiService, times(1)).ack(
        		Mockito.eq(new AckMessageDto(123, Ack.ERROR, "error message", false)), 
        		Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verify(appStatus, times(1)).setError(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS),
                Mockito.anyString());
    }

    /**
     * Test ack when exception
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testAckNegatively() throws AbstractCodedException {
        doReturn(true).when(mqiService).ack(Mockito.any(), Mockito.any());

        extractor.ackNegatively(reportingFactory, new FailedProcessingDto(), inputMessage, "error message");

        verify(mqiService, times(1)).ack(
        		Mockito.eq(new AckMessageDto(123, Ack.ERROR, "error message", false)),
        		Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verify(appStatus, times(1)).setError(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS),
                Mockito.anyString());
    }

    /**
     * Test ack when exception
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testAckPositivelyWhenException() throws AbstractCodedException {
        doThrow(new MqiAckApiError(ProductCategory.AUXILIARY_FILES, 1,
                "ack-msg", "error-message")).when(mqiService)
                        .ack(Mockito.any(), Mockito.any());

        extractor.ackPositively(reportingFactory, inputMessage);

        verify(mqiService, times(1))
                .ack(Mockito.eq(new AckMessageDto(123, Ack.OK, null, false)),Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verify(appStatus, times(1)).setError(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS),
                Mockito.anyString());
    }

    /**
     * Test ack when exception
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testAckPositively() throws AbstractCodedException {
        doReturn(true).when(mqiService).ack(Mockito.any(), Mockito.any());

        extractor.ackPositively(reportingFactory, inputMessage);

        verify(mqiService, times(1))
                .ack(Mockito.eq(new AckMessageDto(123, Ack.OK, null, false)),
                		Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verify(appStatus, never()).setError(
                Mockito.eq(ProductCategory.LEVEL_PRODUCTS),
                Mockito.anyString());
    }

    @Test
    public void testGenericExtractMEsageBodyEmpty()
            throws AbstractCodedException {
        doReturn(new GenericMessageDto<>(123, "", null)).when(mqiService)
                .next(Mockito.any());

        extractor.genericExtract();
        verify(mqiService, times(1)).next(Mockito.any());
        verifyZeroInteractions(obsService);
        verify(appStatus, times(1))
                .setWaiting(Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verifyNoMoreInteractions(appStatus);
        verifyZeroInteractions(esServices);
    }

    @Test
    public void testGenericExtractMEsageNull() throws AbstractCodedException {
        doReturn(null).when(mqiService).next(Mockito.any());

        extractor.genericExtract();
        verify(mqiService, times(1)).next(Mockito.any());
        verifyZeroInteractions(obsService);
        verify(appStatus, times(1))
                .setWaiting(Mockito.eq(ProductCategory.LEVEL_PRODUCTS));
        verifyNoMoreInteractions(appStatus);
        verifyZeroInteractions(esServices);
    }

}
