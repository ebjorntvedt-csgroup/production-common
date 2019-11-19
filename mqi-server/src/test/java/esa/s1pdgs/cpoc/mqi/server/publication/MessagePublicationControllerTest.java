package esa.s1pdgs.cpoc.mqi.server.publication;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import esa.s1pdgs.cpoc.common.EdrsSessionFileType;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiCategoryNotAvailable;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiPublicationError;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiRouteNotAvailable;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelReportDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductionEvent;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties.ProductCategoryProperties;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties.ProductCategoryPublicationProperties;
import esa.s1pdgs.cpoc.mqi.server.GenericKafkaUtils;
import esa.s1pdgs.cpoc.mqi.server.KafkaProperties;
import esa.s1pdgs.cpoc.mqi.server.converter.XmlConverter;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.GenericProducer;

/**
 * @author Viveris Technologies
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class MessagePublicationControllerTest {

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true,
            GenericKafkaUtils.TOPIC_ERROR, GenericKafkaUtils.TOPIC_L0_PRODUCTS,
            GenericKafkaUtils.TOPIC_L0_ACNS, GenericKafkaUtils.TOPIC_L0_REPORTS,
            GenericKafkaUtils.TOPIC_L1_PRODUCTS,
            GenericKafkaUtils.TOPIC_AUXILIARY_FILES,
            GenericKafkaUtils.TOPIC_L1_ACNS, GenericKafkaUtils.TOPIC_L1_REPORTS,
            GenericKafkaUtils.TOPIC_L0_JOBS, GenericKafkaUtils.TOPIC_L1_JOBS,
            GenericKafkaUtils.TOPIC_EDRS_SESSIONS,
            GenericKafkaUtils.TOPIC_L0_SEGMENTS,
            GenericKafkaUtils.TOPIC_L2_JOBS,
            GenericKafkaUtils.TOPIC_L2_REPORTS,
            GenericKafkaUtils.TOPIC_L2_PRODUCTS,
            GenericKafkaUtils.TOPIC_L2_ACNS
            );

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private XmlConverter xmlConverter;
    
    @Autowired
    private GenericProducer producer;

    @Autowired
    private MessagePublicationController autowiredController;

    @Mock
    private ApplicationProperties appProperties;

    private MessagePublicationController customController;
    private GenericKafkaUtils<ProductionEvent> kafkaUtilsProducts;
    private GenericKafkaUtils<LevelReportDto> kafkaUtilsReports;
    private GenericKafkaUtils<IngestionEvent> kafkaUtilsEdrsSession;
    private GenericKafkaUtils<IpfExecutionJob> kafkaUtilsJobs;
    
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        kafkaUtilsProducts = new GenericKafkaUtils<ProductionEvent>(embeddedKafka);
        kafkaUtilsReports = new GenericKafkaUtils<LevelReportDto>(embeddedKafka);
        kafkaUtilsEdrsSession = new GenericKafkaUtils<IngestionEvent>(embeddedKafka);
        kafkaUtilsJobs = new GenericKafkaUtils<IpfExecutionJob>(embeddedKafka);
    }

    private void initCustomControllerForNoPublication() {

        doReturn(new HashMap<>()).when(appProperties).getProductCategories();

        customController = new MessagePublicationController(appProperties,
                 xmlConverter, producer);
        try {
            customController.initialize();
        } catch (IOException | JAXBException e) {
            fail(e.getMessage());
        }
    }

    private void initCustomControllerForAllPublication() {
        Map<ProductCategory, ProductCategoryProperties> map = new HashMap<>();
        map.put(ProductCategory.AUXILIARY_FILES, new ProductCategoryProperties(
                null, new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/auxiliary-files.xml")));
        map.put(ProductCategory.EDRS_SESSIONS, new ProductCategoryProperties(
                null, new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/edrs-sessions.xml")));
        map.put(ProductCategory.LEVEL_JOBS, new ProductCategoryProperties(null,
                new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/level-jobs.xml")));
        map.put(ProductCategory.LEVEL_PRODUCTS, new ProductCategoryProperties(
                null, new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/level-products.xml")));
        map.put(ProductCategory.LEVEL_REPORTS, new ProductCategoryProperties(
                null, new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/level-reports.xml")));
        map.put(ProductCategory.LEVEL_SEGMENTS, new ProductCategoryProperties(
                null, new ProductCategoryPublicationProperties(true,
                        "./src/test/resources/routing-files/level-segments.xml")));
        doReturn(map).when(appProperties).getProductCategories();

        customController = new MessagePublicationController(appProperties, xmlConverter, producer);
        try {
            customController.initialize();
        } catch (IOException | JAXBException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetTopicWhenNoCategory()
            throws MqiCategoryNotAvailable, MqiRouteNotAvailable {
        initCustomControllerForNoPublication();

        thrown.expect(MqiCategoryNotAvailable.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.EDRS_SESSIONS)));
        thrown.expect(hasProperty("type", is("publisher")));

        customController.getTopic(ProductCategory.EDRS_SESSIONS,
                ProductFamily.EDRS_SESSION, "NONE", "NONE");
    }

    @Test
    public void testGetTopicWhenNoRouting()
            throws MqiCategoryNotAvailable, MqiRouteNotAvailable {
        initCustomControllerForAllPublication();
        thrown.expect(MqiRouteNotAvailable.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.EDRS_SESSIONS)));
        thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));

        customController.getTopic(ProductCategory.EDRS_SESSIONS,
                ProductFamily.AUXILIARY_FILE, "NONE", "NONE");
    }

    @Test
    public void publishNoCat() throws MqiPublicationError,
            MqiCategoryNotAvailable, MqiRouteNotAvailable {
        IngestionEvent ingestionEvent = new IngestionEvent("obs-key", "/path/of/inbox", 1,
                EdrsSessionFileType.RAW, "S1", "A", "WILE", "sessionId");
        initCustomControllerForNoPublication();

        thrown.expect(MqiCategoryNotAvailable.class);
        thrown.expect(
                hasProperty("category", is(ProductCategory.EDRS_SESSIONS)));
        thrown.expect(hasProperty("type", is("publisher")));

        customController.publish(ProductCategory.EDRS_SESSIONS, ingestionEvent, "NONE", "NONE");
    }

    @Test
    public void publishEdrsSessions() throws Exception {
        IngestionEvent ingestionEvent = new IngestionEvent("obs-key", "/path/of/inbox", 2,
                EdrsSessionFileType.RAW, "S1", "A", "WILE", "sessionId");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.EDRS_SESSIONS, ingestionEvent, "NONE", "NONE");

        ConsumerRecord<String, IngestionEvent> record =
                kafkaUtilsEdrsSession.getReceivedRecordEdrsSession(
                        GenericKafkaUtils.TOPIC_EDRS_SESSIONS);

        assertEquals(ingestionEvent, record.value());
    }


    @Test
    public void publishAuxiliaryFiles() throws Exception {
        ProductionEvent dto = new ProductionEvent("product-name", "key-obs", ProductFamily.AUXILIARY_FILE);
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.AUXILIARY_FILES, dto, "NONE", "NONE");

        ConsumerRecord<String, ProductionEvent> record = kafkaUtilsProducts
                .getReceivedRecordAux(GenericKafkaUtils.TOPIC_AUXILIARY_FILES);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelProducts() throws Exception {
        ProductionEvent dto = new ProductionEvent("product-name", "key-obs",
                ProductFamily.L0_SLICE, "NRT");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_PRODUCTS, dto, "NONE", "NONE");

        ConsumerRecord<String, ProductionEvent> record = kafkaUtilsProducts
                .getReceivedRecordProducts(GenericKafkaUtils.TOPIC_L0_PRODUCTS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelProducts1() throws Exception {
        ProductionEvent dto = new ProductionEvent("product-name", "key-obs",
                ProductFamily.L1_ACN, "NRT");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_PRODUCTS,dto, "t-pdgs-l1-jobs-nrt", "L1_ACN");

        ConsumerRecord<String, ProductionEvent> record = kafkaUtilsProducts
                .getReceivedRecordProducts(GenericKafkaUtils.TOPIC_L1_ACNS);

        assertEquals(dto, record.value());
    }


    @Test
    public void publishLevelSegments() throws Exception {
        ProductionEvent dto = new ProductionEvent("product-name", "key-obs",
                ProductFamily.L0_SEGMENT, "FAST");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_SEGMENTS, dto, "NONE", "NONE");

        ConsumerRecord<String, ProductionEvent> record = kafkaUtilsProducts
                .getReceivedRecordSegments(GenericKafkaUtils.TOPIC_L0_SEGMENTS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelJobs() throws Exception {
        IpfExecutionJob dto = new IpfExecutionJob(ProductFamily.L1_JOB, "product-name", "NRT",
                "work-directory", "job-order");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_JOBS, dto, "t-pdgs-l0-slices-nrt", "L1_JOB");

        ConsumerRecord<String, IpfExecutionJob> record = kafkaUtilsJobs
                .getReceivedRecordJobs(GenericKafkaUtils.TOPIC_L1_JOBS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelJobs1() throws Exception {
        IpfExecutionJob dto = new IpfExecutionJob(ProductFamily.L0_JOB, "product-name", "NRT",
                "work-directory", "job-order");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_JOBS, dto, "NONE", "NONE");

        ConsumerRecord<String, IpfExecutionJob> record = kafkaUtilsJobs
                .getReceivedRecordJobs(GenericKafkaUtils.TOPIC_L0_JOBS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelJobsL2() throws Exception {
        IpfExecutionJob dto = new IpfExecutionJob(ProductFamily.L2_JOB, "product-name", "FAST",
                "work-directory", "job-order");
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_JOBS, dto, "NONE", "NONE");

        ConsumerRecord<String, IpfExecutionJob> record = kafkaUtilsJobs
                .getReceivedRecordJobs(GenericKafkaUtils.TOPIC_L2_JOBS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelReports() throws Exception {
        LevelReportDto dto = new LevelReportDto("product-name", "content",
                ProductFamily.L1_REPORT);
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_REPORTS, dto, "NONE", "NONE");

        ConsumerRecord<String, LevelReportDto> record = kafkaUtilsReports
                .getReceivedRecordReports(GenericKafkaUtils.TOPIC_L1_REPORTS);

        assertEquals(dto, record.value());
    }

    @Test
    public void publishLevelReports1() throws Exception {
        LevelReportDto dto = new LevelReportDto("product-name2", "content2",
                ProductFamily.L0_REPORT);
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_REPORTS, dto, "NONE", "NONE");

        ConsumerRecord<String, LevelReportDto> record = kafkaUtilsReports
                .getReceivedRecordReports(GenericKafkaUtils.TOPIC_L0_REPORTS);

        assertEquals(dto, record.value());
    }
    
    @Test
    public void publishLevelReportsL2() throws Exception {
        LevelReportDto dto = new LevelReportDto("product-name2", "content2",
                ProductFamily.L2_REPORT);
        initCustomControllerForAllPublication();

        customController.publish(ProductCategory.LEVEL_REPORTS, dto, "NONE", "NONE");

        ConsumerRecord<String, LevelReportDto> record = kafkaUtilsReports
                .getReceivedRecordReports(GenericKafkaUtils.TOPIC_L2_REPORTS);

        assertEquals(dto, record.value());
    }
}
