package esa.s1pdgs.cpoc.ipf.preparation.worker.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerInputsMissingException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.AiopProperties;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings.WaitTempo;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessConfiguration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.XmlConfig;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.JobGeneration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTable;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.JobsGeneratorFactory.JobGenType;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.client.SearchMetadataQuery;
import esa.s1pdgs.cpoc.metadata.model.EdrsSessionMetadata;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.client.MqiClient;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;

/**
 * @author Cyrielle
 */
public class L0AppJobsGeneratorTest {

    /**
     * XML converter
     */
    @Mock
    private XmlConverter xmlConverter;

    @Mock
    private MetadataClient metadataClient;

    @Mock
    private ProcessSettings l0ProcessSettings;

    @Mock
    private IpfPreparationWorkerSettings ipfPreparationWorkerSettings;

    @Mock
    private AiopProperties aiopProperties;

    @Mock
    private MqiClient mqiClient;

    @Mock
    private AppCatalogJobClient appDataService;
    
    @Mock
    private ProcessConfiguration processConfiguration;

    private TaskTable expectedTaskTable;
    private L0AppJobsGenerator generator;
    
    private IpfExecutionJob publishedJob;

    /**
     * Test set up
     * 
     * @throws Exception
     */
    @Before
    public void init() throws Exception {

        // Retrieve task table from the XML converter
        expectedTaskTable = TestL0Utils.buildTaskTableAIOP();

        // Mockito
        MockitoAnnotations.initMocks(this);
        this.mockProcessSettings();
        this.mockJobGeneratorSettings();
        this.mockAiopProperties();
        this.mockXmlConverter();
        this.mockMetadataService();
        this.mockKafkaSender();
        this.mockAppDataService();

        final JobsGeneratorFactory factory = new JobsGeneratorFactory(
                l0ProcessSettings, 
                ipfPreparationWorkerSettings, 
                aiopProperties,
                xmlConverter, 
                metadataClient, 
                processConfiguration, 
                mqiClient,
                new TaskTableFactory(new XmlConfig().xmlConverter())
        );
        final File file = new File("./test/data/generic_config/task_tables/TaskTable.AIOP.xml");
        
        generator = (L0AppJobsGenerator) factory.newJobGenerator(file, appDataService, JobGenType.LEVEL_0);
    }

    private void mockProcessSettings() {
        Mockito.doAnswer(i -> {
            final Map<String, String> r = new HashMap<String, String>(2);
            return r;
        }).when(l0ProcessSettings).getParams();
        Mockito.doAnswer(i -> {
            final Map<String, String> r = new HashMap<String, String>(5);
            r.put("SM_RAW__0S", "^S1[A-B]_S[1-6]_RAW__0S.*$");
            r.put("AN_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("ZS_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("REP_L0PSA_", "^S1[A|B|_]_OPER_REP_ACQ.*$");
            r.put("REP_EFEP_", "^S1[A|B|_]_OPER_REP_PASS.*.EOF$");
            return r;
        }).when(l0ProcessSettings).getOutputregexps();
        Mockito.doAnswer(i -> {
            return ApplicationLevel.L0;
        }).when(l0ProcessSettings).getLevel();
        Mockito.doAnswer(i -> {
            return "hostname";
        }).when(l0ProcessSettings).getHostname();
    }

    private void mockJobGeneratorSettings() {
        Mockito.doAnswer(i -> {
            final Map<String, ProductFamily> r =
                    new HashMap<String, ProductFamily>(20);
            final String families =
                    "MPL_ORBPRE:AUXILIARY_FILE||MPL_ORBSCT:AUXILIARY_FILE||AUX_OBMEMC:AUXILIARY_FILE||AUX_CAL:AUXILIARY_FILE||AUX_PP1:AUXILIARY_FILE||AUX_INS:AUXILIARY_FILE||AUX_RESORB:AUXILIARY_FILE||AUX_RES:AUXILIARY_FILE";
            if (!StringUtils.isEmpty(families)) {
                final String[] paramsTmp = families.split("\\|\\|");
                for (int k = 0; k < paramsTmp.length; k++) {
                    if (!StringUtils.isEmpty(paramsTmp[k])) {
                        final String[] tmp = paramsTmp[k].split(":", 2);
                        if (tmp.length == 2) {
                            r.put(tmp[0], ProductFamily.fromValue(tmp[1]));
                        }
                    }
                }
            }
            return r;
        }).when(ipfPreparationWorkerSettings).getInputfamilies();
        Mockito.doAnswer(i -> {
            final Map<String, ProductFamily> r = new HashMap<>();
            r.put("", ProductFamily.L0_REPORT);
            r.put("", ProductFamily.L0_ACN);
            return r;
        }).when(ipfPreparationWorkerSettings).getOutputfamilies();
        Mockito.doAnswer(i -> {
            return ProductFamily.L0_ACN.toString();
        }).when(ipfPreparationWorkerSettings).getDefaultfamily();
        Mockito.doAnswer(i -> {
            return 2;
        }).when(ipfPreparationWorkerSettings).getMaxnumberofjobs();
        Mockito.doAnswer(i -> {
            return new WaitTempo(2000, 3);
        }).when(ipfPreparationWorkerSettings).getWaitprimarycheck();
        Mockito.doAnswer(i -> {
            return new WaitTempo(10000, 3);
        }).when(ipfPreparationWorkerSettings).getWaitmetadatainput();
    }
    
    private void mockAiopProperties() {
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "MTI_");
    		r.put("cgs2", "SGS_");
    		r.put("cgs3", "MPS_");
    		r.put("cgs4", "INU_");
    		r.put("erds", "WILE");
    		return r;
    	}).when(aiopProperties).getStationCodes();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "yes");
    		r.put("cgs2", "yes");
    		r.put("cgs3", "yes");
    		r.put("cgs4", "yes");
    		r.put("erds", "yes");
    		return r;
    	}).when(aiopProperties).getPtAssembly();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "NRT");
    		r.put("cgs2", "NRT");
    		r.put("cgs3", "NRT");
    		r.put("cgs4", "NRT");
    		r.put("erds", "NRT");
    		return r;
    	}).when(aiopProperties).getProcessingMode();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "FAST24");
    		r.put("cgs2", "FAST24");
    		r.put("cgs3", "FAST24");
    		r.put("cgs4", "FAST24");
    		r.put("erds", "FAST24");
    		return r;
    	}).when(aiopProperties).getReprocessingMode();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "300");
    		r.put("cgs2", "300");
    		r.put("cgs3", "300");
    		r.put("cgs4", "360");
    		r.put("erds", "360");
    		return r;
    	}).when(aiopProperties).getTimeoutSec();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "yes");
    		r.put("cgs2", "yes");
    		r.put("cgs3", "yes");
    		r.put("cgs4", "yes");
    		r.put("erds", "yes");
    		return r;
    	}).when(aiopProperties).getDescramble();
    	Mockito.doAnswer(i -> {
    		final Map<String,String> r = new HashMap<>();
    		r.put("cgs1", "no");
    		r.put("cgs2", "no");
    		r.put("cgs3", "no");
    		r.put("cgs4", "yes");
    		r.put("erds", "yes");
    		return r;
    	}).when(aiopProperties).getRsEncode();
    	Mockito.doReturn(true).when(aiopProperties).getDisableTimeout();
    }

    private void mockXmlConverter() {
        try {
            Mockito.when(
                    xmlConverter.convertFromXMLToObject(Mockito.anyString()))
                    .thenReturn(expectedTaskTable);
            Mockito.when(
                    xmlConverter.convertFromObjectToXMLString(Mockito.any()))
                    .thenReturn(null);
        } catch (IOException | JAXBException e1) {
            fail("BuildTaskTableException raised: " + e1.getMessage());
        }
    }

    private void mockMetadataService() {
        try {
            Mockito.doAnswer(i -> {
                final String productName = i.getArgument(1);
                final Calendar start = Calendar.getInstance();
                start.set(2017, Calendar.DECEMBER, 5, 20, 3, 9);
                final Calendar stop = Calendar.getInstance();
                stop.set(2017, Calendar.DECEMBER, 15, 20, 3, 9);
                if (productName.contains("ch1")) {
                    return new EdrsSessionMetadata(productName, "RAW",
                            "S1A/L20171109175634707000125/ch01/" + productName,
                            "session",
                            null, null,
                            null, null,
                            "S1",
                            "A",
                            "WILE",
                            Collections.emptyList());
                } else {
                    return new EdrsSessionMetadata(productName, "RAW",
                            "S1A/L20171109175634707000125/ch02/" + productName,
                            "session",
                            null, null,
                            null, null,
                            "S1",
                            "A",
                            "WILE",
                            Collections.emptyList());
                }
            }).when(this.metadataClient).getEdrsSession(Mockito.anyString(),
                    Mockito.anyString());
            Mockito.doAnswer(i -> {
                final SearchMetadataQuery query = i.getArgument(0);
                if ("MPL_ORBPRE".equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_OPER_MPL_ORBPRE_20171208T200309_20171215T200309_0001.EOF",
                            "MPL_ORBPRE",
                            "S1A_OPER_MPL_ORBPRE_20171208T200309_20171215T200309_0001.EOF",
                            "2017-12-05T20:03:09.123456Z", "2017-12-15T20:03:09.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("MPL_ORBSCT"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_OPER_MPL_ORBSCT_20140507T150704_99999999T999999_0020.EOF",
                            "MPL_ORBSCT",
                            "S1A_OPER_MPL_ORBSCT_20140507T150704_99999999T999999_0020.EOF",
                            "2014-04-03T22:46:09.123456Z", "9999-12-31T23:59:59.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("AUX_OBMEMC"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_OPER_AUX_OBMEMC_PDMC_20140201T000000.xml",
                            "AUX_OBMEMC",
                            "S1A_OPER_AUX_OBMEMC_PDMC_20140201T000000.xml",
                            "2014-02-01T00:00:00.123456Z", "9999-12-31T23:59:59.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                }
                return null;
            }).when(this.metadataClient).search(Mockito.any(), Mockito.any(),
                    Mockito.any(), Mockito.anyString(), Mockito.anyInt(),
                    Mockito.anyString(), Mockito.anyString());
        } catch (final MetadataQueryException e) {
            fail(e.getMessage());
        }
    }

    private void mockKafkaSender() throws AbstractCodedException {
        Mockito.doAnswer(i -> {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("./tmp/inputMessageL0.json"),
                    i.getArgument(0));
            mapper.writeValue(new File("./tmp/jobDtoL0.json"),
                    i.getArgument(1));
            publishedJob = i.getArgument(1);
            return null;
        }).when(this.mqiClient).publish(Mockito.any(), Mockito.any());
    }

    private void mockAppDataService()
            throws InternalErrorException, AbstractCodedException {
        doReturn(Arrays.asList(TestL0Utils.buildAppDataEdrsSession(true)))
                .when(appDataService)
                .findNByPodAndGenerationTaskTableWithNotSentGeneration(
                        Mockito.anyString(), Mockito.anyString());
        final AppDataJob<?> primaryCheckAppJob =
                TestL0Utils.buildAppDataEdrsSession(true);
        primaryCheckAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.PRIMARY_CHECK);
        final AppDataJob<?> readyAppJob =
                TestL0Utils.buildAppDataEdrsSession(true);
        readyAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.READY);
        final AppDataJob<?> sentAppJob =
                TestL0Utils.buildAppDataEdrsSession(true);
        sentAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.SENT);

        doReturn(primaryCheckAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.AIOP.xml"),
                Mockito.eq(AppDataJobGenerationState.PRIMARY_CHECK));
        doReturn(readyAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.AIOP.xml"),
                Mockito.eq(AppDataJobGenerationState.READY));
        doReturn(sentAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.AIOP.xml"),
                Mockito.eq(AppDataJobGenerationState.SENT));
        Mockito.doAnswer(i -> {
            return i.getArgument(1);
        }).when(appDataService).patchJob(Mockito.anyLong(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.anyBoolean(),
                Mockito.anyBoolean());
    }

    @Test
    public void testPreSearch() {
        final AppDataJob appDataJob =
                TestL0Utils.buildAppDataEdrsSession(true);
        final AppDataJob appDataJobComplete =
                TestL0Utils.buildAppDataEdrsSession(false);
        final JobGeneration job =
                new JobGeneration(appDataJob, "TaskTable.AIOP.xml");

        try {
            generator.preSearch(job);
            for (int i = 0; i < appDataJobComplete.getProduct().getRaws1()
                    .size(); i++) {
                assertEquals(
                        appDataJobComplete.getProduct().getRaws1().get(i)
                                .getKeyObs(),
                        appDataJob.getProduct().getRaws1().get(i).getKeyObs());
            }
            for (int i = 0; i < appDataJobComplete.getProduct().getRaws2()
                    .size(); i++) {
                assertEquals(
                        appDataJobComplete.getProduct().getRaws2().get(i)
                                .getKeyObs(),
                        appDataJob.getProduct().getRaws2().get(i).getKeyObs());
            }
        } catch (final IpfPrepWorkerInputsMissingException e) {
            fail("MetadataMissingException raised: " + e.getMessage());
        }
    }

    @Test
    public void testPreSearchMissingRaw() throws MetadataQueryException {
        Mockito.doAnswer(i -> {
            return null;
        }).when(this.metadataClient).getEdrsSession(Mockito.anyString(),
                Mockito.anyString());

        final AppDataJob appDataJob =
                TestL0Utils.buildAppDataEdrsSession(true);
        final JobGeneration job =
                new JobGeneration(appDataJob, "TaskTable.AIOP.xml");
        try {
            generator.preSearch(job);
            fail("MetadataMissingException shall be raised");
        } catch (final IpfPrepWorkerInputsMissingException e) {
            assertTrue(e.getMissingMetadata().containsKey(
                    "DCS_02_L20171109175634707000125_ch1_DSDB_00001.raw"));
            assertTrue(e.getMissingMetadata().containsKey(
                    "DCS_02_L20171109175634707000125_ch1_DSDB_00023.raw"));
        }
    }
    
	@Test
	public void testTimeoutReachedForPrimarySearch_reached() {

		final String downlinkEndTime = "2020-01-01T00:00:00.000000Z";
		final long startToWaitMs = DateUtils.parse("2020-01-01T00:20:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();
		final long currentTimeMs = DateUtils.parse("2020-01-01T01:30:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();

		final long minimalWaitingTimeMs = 30 * 60 * 1000;
		final long timeoutForDownlinkStationMs = 55 * 60 * 1000;

		final boolean timeoutReached = generator.timeoutReachedForPrimarySearch(downlinkEndTime, currentTimeMs, startToWaitMs,
				minimalWaitingTimeMs, timeoutForDownlinkStationMs);
		
		assertTrue(timeoutReached);
	}
	
	@Test
	public void testTimeoutReachedForPrimarySearch_not_reached() {

		final String downlinkEndTime = "2020-01-01T00:00:00.000000Z";
		final long startToWaitMs =  DateUtils.parse("2020-01-01T00:20:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();
		final long currentTimeMs = DateUtils.parse("2020-01-01T00:50:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();

		final long minimalWaitingTimeMs = 30 * 60 * 1000;
		final long timeoutForDownlinkStationMs = 55 * 60 * 1000;

		final boolean timeoutReached = generator.timeoutReachedForPrimarySearch(downlinkEndTime, currentTimeMs, startToWaitMs,
				minimalWaitingTimeMs, timeoutForDownlinkStationMs);
		
		assertFalse(timeoutReached);
	}
	
	
	@Test
	public void testTimeoutReachedForPrimarySearch_minimal_waiting_not_reached() {

		final String downlinkEndTime = "2020-01-01T00:00:00.000000Z";
		final long startToWaitMs = DateUtils.parse("2020-01-01T01:20:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();
		final long currentTimeMs = DateUtils.parse("2020-01-01T01:30:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();

		final long minimalWaitingTimeMs = 30 * 60 * 1000;
		final long timeoutForDownlinkStationMs = 55 * 60 * 1000;

		final boolean timeoutReached = generator.timeoutReachedForPrimarySearch(downlinkEndTime, currentTimeMs, startToWaitMs,
				minimalWaitingTimeMs, timeoutForDownlinkStationMs);
		
		assertFalse(timeoutReached);
	}
	
	@Test
	public void testTimeoutReachedForPrimarySearch_minimal_waiting_reached() {

		final String downlinkEndTime = "2020-01-01T00:00:00.000000Z";
		final long startToWaitMs = DateUtils.parse("2020-01-01T01:20:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();
		final long currentTimeMs = DateUtils.parse("2020-01-01T01:55:00.000000Z").toInstant(ZoneOffset.UTC).toEpochMilli();

		final long minimalWaitingTimeMs = 30 * 60 * 1000;
		final long timeoutForDownlinkStationMs = 55 * 60 * 1000;

		final boolean timeoutReached = generator.timeoutReachedForPrimarySearch(downlinkEndTime, currentTimeMs, startToWaitMs,
				minimalWaitingTimeMs, timeoutForDownlinkStationMs);
		
		assertTrue(timeoutReached);
	}


// FIXME: Enable test
//    @Test
//    public void testCustomDto() {
//        AppDataJob appDataJob =
//                TestL0Utils.buildAppDataEdrsSession(false);
//        JobGeneration job =
//                new JobGeneration(appDataJob, "TaskTable.AIOP.xml");
//        job.setJobOrder(TestL0Utils.buildJobOrderL20171109175634707000125());
//        ProductFamily family = ProductFamily.EDRS_SESSION;
//        IpfExecutionJob dto = new IpfExecutionJob(family,
//                appDataJob.getProduct().getProductName(),
//                appDataJob.getProduct().getProcessMode(), "/data/test/workdir/",
//                "/data/test/workdir/JobOrder.xml");
//
//        generator.customJobDto(job, dto);
//        int nbChannel1 = appDataJob.getProduct().getRaws1().size();
//        int nbChannel2 = appDataJob.getProduct().getRaws2().size();
//        assertTrue(dto.getInputs().size() == nbChannel1 + nbChannel2);
//        for (int i = 0; i < nbChannel1; i++) {
//            AppDataJobFileDto raw1 = appDataJob.getProduct().getRaws1().get(i);
//            AppDataJobFileDto raw2 = appDataJob.getProduct().getRaws2().get(i);
//            int indexRaw1 = i * 2;
//            int indexRaw2 = i * 2 + 1;
//            assertEquals(raw1.getKeyObs(),
//                    dto.getInputs().get(indexRaw1).getContentRef());
//            assertEquals(ProductFamily.EDRS_SESSION.name(),
//                    dto.getInputs().get(indexRaw1).getFamily());
//            assertEquals("/data/test/workdir/ch01/" + raw1.getFilename(),
//                    dto.getInputs().get(indexRaw1).getLocalPath());
//            assertEquals(raw2.getKeyObs(),
//                    dto.getInputs().get(indexRaw2).getContentRef());
//            assertEquals(ProductFamily.EDRS_SESSION.name(),
//                    dto.getInputs().get(indexRaw2).getFamily());
//            assertEquals("/data/test/workdir/ch02/" + raw2.getFilename(),
//                    dto.getInputs().get(indexRaw2).getLocalPath());
//        }
//    }

// FIXME: Enable test
//    @Test
//    public void testCustomJobOrder() {
//        AppDataJob appDataJob =
//                TestL0Utils.buildAppDataEdrsSession(false);
//        JobGeneration job =
//                new JobGeneration(appDataJob, "TaskTable.AIOP.xml");
//        job.setJobOrder(TestL0Utils.buildJobOrderL20171109175634707000125());
//        generator.customJobOrder(job);
//        job.getJobOrder().getConf().getProcParams().forEach(param -> {
//            if ("Mission_Id".equals(param.getName())) {
//                assertEquals("S1A", param.getValue());
//            }
//        });
//
//        AppDataJob appDataJob1 =
//                TestL0Utils.buildAppDataEdrsSession(false, "S2", true, true);
//        JobGeneration job1 =
//                new JobGeneration(appDataJob1, "TaskTable.AIOP.xml");
//        job1.setJobOrder(TestL0Utils.buildJobOrderL20171109175634707000125());
//        generator.customJobOrder(job1);
//        job1.getJobOrder().getConf().getProcParams().forEach(param -> {
//            if ("Mission_Id".equals(param.getName())) {
//                assertEquals("S2A", param.getValue());
//            }
//        });
//    }

    // FIXME Enable test
//    @Test
//    public void testRun() throws InternalErrorException, AbstractCodedException {
//
//        mockAppDataService();
//
//        generator.run();
//
//        Mockito.verify(jobsSender).sendJob(Mockito.any(), Mockito.any());
//        
//        assertEquals(ProductFamily.L0_JOB, publishedJob.getFamily());
//        assertEquals("", publishedJob.getProductProcessMode());
//        assertEquals("L20171109175634707000125", publishedJob.getProductIdentifier());
//        assertEquals(expectedTaskTable.getPools().size(), publishedJob.getPools().size());
//        for (int i = 0; i < expectedTaskTable.getPools().size(); i++) {
//            assertEquals(expectedTaskTable.getPools().get(i).getTasks().size(), publishedJob.getPools().get(i).getTasks().size());
//            for (int j = 0; j < expectedTaskTable.getPools().get(i).getTasks().size(); j++) {
//                assertEquals(expectedTaskTable.getPools().get(i).getTasks().get(j).getFileName(), publishedJob.getPools().get(i).getTasks().get(j).getBinaryPath());
//            }
//        }
//
//    }
}
