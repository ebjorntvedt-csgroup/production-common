package esa.s1pdgs.cpoc.wrapper.job.mqi;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelReportDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericPublicationMessageDto;
import esa.s1pdgs.cpoc.wrapper.job.model.mqi.FileQueueMessage;
import esa.s1pdgs.cpoc.wrapper.job.model.mqi.ObsQueueMessage;

/**
 * Test the factory for producer message in topics
 * 
 * @author Viveris Technologies
 */
public class OutputProducerFactoryTest {

    /**
     * Kafka producer for segments
     */
    @Mock
    private GenericMqiService<ProductDto> senderSegments;

    /**
     * Kafka producer for L0 slices
     */
    @Mock
    private GenericMqiService<ProductDto> senderProducts;

    /**
     * Kafka producer for report
     */
    @Mock
    private GenericMqiService<LevelReportDto> senderReports;

    /**
     * Factory to test
     */
    private OutputProcuderFactory outputProcuderFactory;

    /**
     * Input message
     */
    private GenericMessageDto<LevelJobDto> inputMessage;

    /**
     * Iinitialization
     * 
     * @throws AbstractCodedException
     */
    @Before
    public void init() throws AbstractCodedException {
        MockitoAnnotations.initMocks(this);
        doNothing().when(senderProducts).publish(Mockito.any());
        doNothing().when(senderReports).publish(Mockito.any());
        this.outputProcuderFactory = new OutputProcuderFactory(senderSegments,
                senderProducts, senderReports);
        inputMessage = new GenericMessageDto<LevelJobDto>(123, "",
                new LevelJobDto(ProductFamily.L0_JOB, "product-name", "FAST",
                        "work-dir", "job-order"));
        inputMessage.setInputKey(null);
    }

    /**
     * Test send L0 reports
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendReport() throws AbstractCodedException {
        this.outputProcuderFactory.sendOutput(
                new FileQueueMessage(ProductFamily.L0_REPORT, "test.txt",
                        new File("./test/data/report.txt")),
                inputMessage);
        verify(this.senderProducts, never()).publish(Mockito.any());
        GenericPublicationMessageDto<LevelReportDto> message =
                new GenericPublicationMessageDto<LevelReportDto>(123,
                        ProductFamily.L0_REPORT, new LevelReportDto("test.txt",
                                "Test report file", ProductFamily.L0_REPORT));
        verify(this.senderReports, times(1)).publish(Mockito.eq(message));
        verify(this.senderSegments, never()).publish(Mockito.any());
    }

    /**
     * Test send L0 product
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendProduct() throws AbstractCodedException {
        this.outputProcuderFactory
                .sendOutput(new ObsQueueMessage(ProductFamily.L0_SLICE,
                        "test.txt", "test.txt", "NRT"), inputMessage);
        GenericPublicationMessageDto<ProductDto> message =
                new GenericPublicationMessageDto<ProductDto>(123,
                        ProductFamily.L0_SLICE, new ProductDto("test.txt",
                                "test.txt", ProductFamily.L0_SLICE, "NRT"));
        message.setOutputKey("L0_SLICE");
        verify(this.senderProducts, times(1)).publish(Mockito.eq(message));
        verify(this.senderReports, never()).publish(Mockito.any());
        verify(this.senderSegments, never()).publish(Mockito.any());
    }

    /**
     * Test send L0 product
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendSegment() throws AbstractCodedException {
        this.outputProcuderFactory
                .sendOutput(new ObsQueueMessage(ProductFamily.L0_SEGMENT,
                        "test.txt", "test.txt", "NRT"), inputMessage);
        GenericPublicationMessageDto<ProductDto> message =
                new GenericPublicationMessageDto<ProductDto>(123,
                        ProductFamily.L0_SEGMENT, new ProductDto("test.txt",
                                "test.txt", ProductFamily.L0_SEGMENT, "NRT"));
        verify(this.senderSegments, times(1)).publish(Mockito.eq(message));
        verify(this.senderProducts, never()).publish(Mockito.any());
        verify(this.senderReports, never()).publish(Mockito.any());
    }

    /**
     * Test send L0 acns
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendAcn() throws AbstractCodedException {
        this.outputProcuderFactory
                .sendOutput(new ObsQueueMessage(ProductFamily.L0_ACN,
                        "test.txt", "test.txt", "FAST"), inputMessage);
        GenericPublicationMessageDto<ProductDto> message =
                new GenericPublicationMessageDto<ProductDto>(123,
                        ProductFamily.L0_ACN, new ProductDto("test.txt",
                                "test.txt", ProductFamily.L0_ACN, "FAST"));
        message.setOutputKey("L0_ACN");
        verify(this.senderProducts, times(1)).publish(Mockito.eq(message));
        verify(this.senderReports, never()).publish(Mockito.any());
    }

    /**
     * Test send L1 report
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendL1Report() throws AbstractCodedException {
        this.outputProcuderFactory.sendOutput(
                new FileQueueMessage(ProductFamily.L1_REPORT, "test.txt",
                        new File("./test/data/report.txt")),
                inputMessage);
        GenericPublicationMessageDto<LevelReportDto> message =
                new GenericPublicationMessageDto<LevelReportDto>(123,
                        ProductFamily.L1_REPORT, new LevelReportDto("test.txt",
                                "Test report file", ProductFamily.L1_REPORT));
        verify(this.senderProducts, never()).publish(Mockito.any());
        verify(this.senderReports, times(1)).publish(Mockito.eq(message));
    }

    /**
     * Test send L1 product
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendL1Product() throws AbstractCodedException {
        this.outputProcuderFactory
                .sendOutput(new ObsQueueMessage(ProductFamily.L1_SLICE,
                        "test.txt", "test.txt", "FAST"), inputMessage);
        GenericPublicationMessageDto<ProductDto> message =
                new GenericPublicationMessageDto<ProductDto>(123,
                        ProductFamily.L1_SLICE, new ProductDto("test.txt",
                                "test.txt", ProductFamily.L1_SLICE, "FAST"));
        message.setOutputKey("L1_SLICE");
        verify(this.senderProducts, times(1)).publish(Mockito.eq(message));
        verify(this.senderReports, never()).publish(Mockito.any());
    }

    /**
     * Test send L1 ACN
     * 
     * @throws AbstractCodedException
     */
    @Test
    public void testSendL1Acn() throws AbstractCodedException {
        this.outputProcuderFactory
                .sendOutput(new ObsQueueMessage(ProductFamily.L1_ACN,
                        "test.txt", "test.txt", "NRT"), inputMessage);
        GenericPublicationMessageDto<ProductDto> message =
                new GenericPublicationMessageDto<ProductDto>(123,
                        ProductFamily.L1_ACN, new ProductDto("test.txt",
                                "test.txt", ProductFamily.L1_ACN, "NRT"));
        message.setOutputKey("L1_ACN");
        verify(this.senderProducts, times(1)).publish(Mockito.eq(message));
        verify(this.senderReports, never()).publish(Mockito.any());
    }
}
