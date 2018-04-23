package fr.viveris.s1pdgs.jobgenerator.tasks.dispatcher;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import fr.viveris.s1pdgs.jobgenerator.config.JobGeneratorSettings;
import fr.viveris.s1pdgs.jobgenerator.exception.BuildTaskTableException;
import fr.viveris.s1pdgs.jobgenerator.exception.JobDispatcherException;
import fr.viveris.s1pdgs.jobgenerator.exception.JobGenerationException;
import fr.viveris.s1pdgs.jobgenerator.model.Job;
import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1Routing;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0Slice;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0SliceProduct;
import fr.viveris.s1pdgs.jobgenerator.service.XmlConverter;
import fr.viveris.s1pdgs.jobgenerator.tasks.generator.JobsGeneratorFactory;
import fr.viveris.s1pdgs.jobgenerator.tasks.generator.L0SlicesJobsGenerator;
import fr.viveris.s1pdgs.jobgenerator.utils.TestDateUtils;
import fr.viveris.s1pdgs.jobgenerator.utils.TestL1Utils;

public class L0SliceJobsDispatcherTest {

	/**
	 * Job generator factory
	 */
	@Mock
	private JobsGeneratorFactory jobsGeneratorFactory;

	/**
	 * Job generator settings
	 */
	@Mock
	private JobGeneratorSettings jobGeneratorSettings;

	/**
	 * Job generator task scheduler
	 */
	@Mock
	private ThreadPoolTaskScheduler jobGenerationTaskScheduler;

	@Mock
	private L0SlicesJobsGenerator mockGeneratorIW;
	@Mock
	private L0SlicesJobsGenerator mockGeneratorOther;

	@Mock
	private XmlConverter xmlConverter;

	private File taskTable1 = new File("./data_test/l1_config/task_tables/EN_RAW__0_GRDF_1.xml");
	private File taskTable2 = new File("./data_test/l1_config/task_tables/EW_RAW__0_GRDH_1.xml");
	private File taskTable3 = new File("./data_test/l1_config/task_tables/EW_RAW__0_GRDM_1.xml");
	private File taskTable4 = new File("./data_test/l1_config/task_tables/IW_RAW__0_GRDH_1.xml");
	private int nbTaskTables;

	private L0SliceJobsDispatcher dispatcher;

	/**
	 * Test set up
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Mocks
		MockitoAnnotations.initMocks(this);

		// Mock the converter
		L1Routing routing = TestL1Utils.buildL1Routing();
		try {
			Mockito.when(xmlConverter.convertFromXMLToObject(Mockito.anyString())).thenReturn(routing);
		} catch (IOException | JAXBException e) {
			fail("Exception occurred: " + e.getMessage());
		}

		// Mock the job generator settings
		doAnswer(i -> {
			return "./data_test/l1_config/task_tables/";
		}).when(jobGeneratorSettings).getDirectoryoftasktables();
		doAnswer(i -> {
			return 25;
		}).when(jobGeneratorSettings).getMaxnumberoftasktables();
		doAnswer(i -> {
			return 2000;
		}).when(jobGeneratorSettings).getScheduledfixedrate();

		// Mock the job generators
		doNothing().when(mockGeneratorIW).addJob(Mockito.any());
		doNothing().when(mockGeneratorOther).addJob(Mockito.any());
		doNothing().when(mockGeneratorIW).run();
		doNothing().when(mockGeneratorOther).run();

		// Mock the job generator factory
		try {
			doAnswer(i -> {
				return null;
			}).when(jobsGeneratorFactory).createJobGeneratorForEdrsSession(Mockito.any());
			doAnswer(i -> {
				File f = (File) i.getArgument(0);
				if (f.getName().startsWith("IW")) {
					return this.mockGeneratorIW;
				}
				return this.mockGeneratorOther;
			}).when(jobsGeneratorFactory).createJobGeneratorForL0Slice(Mockito.any());
		} catch (BuildTaskTableException e) {
			fail("Exception occurred: " + e.getMessage());
		}

		// Mock the task scheduler
		doAnswer(i -> {
			return null;
		}).when(jobGenerationTaskScheduler).scheduleAtFixedRate(Mockito.any(), Mockito.any());

		// Retrieve number of tasktables
		File taskTableDirectory = new File("./data_test/l1_config/task_tables");
		if (taskTableDirectory.isDirectory()) {
			String[] files = taskTableDirectory.list();
			if (files != null) {
				this.nbTaskTables = files.length;
			} else {
				this.nbTaskTables = -1;
			}
		} else {
			this.nbTaskTables = -1;
		}

		// Return the dispatcher
		this.dispatcher = new L0SliceJobsDispatcher(jobGeneratorSettings, jobsGeneratorFactory,
				jobGenerationTaskScheduler, xmlConverter, "./data_test/l1_config/routing.xml");
	}

	@Test
	public void testCreate() {
		try {
			this.dispatcher.createJobGenerator(taskTable1);
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(any());
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(eq(taskTable1));
		} catch (BuildTaskTableException e) {
			fail("Invalid raised exception: " + e.getMessage());
		}
	}

	/**
	 * Test the initialize function
	 */
	@Test
	public void testInitialize() {
		try {
			this.dispatcher.initialize();

			// Verify creation of job generator
			verify(jobGenerationTaskScheduler, times(this.nbTaskTables)).scheduleAtFixedRate(any(), anyLong());
			verify(jobGenerationTaskScheduler, times(this.nbTaskTables)).scheduleAtFixedRate(any(), eq(2000L));
			verify(jobsGeneratorFactory, never()).createJobGeneratorForEdrsSession(any());
			verify(jobsGeneratorFactory, times(this.nbTaskTables)).createJobGeneratorForL0Slice(any());
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(eq(taskTable1));
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(eq(taskTable2));
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(eq(taskTable3));
			verify(jobsGeneratorFactory, times(1)).createJobGeneratorForL0Slice(eq(taskTable4));
			assertTrue(dispatcher.generators.size() == this.nbTaskTables);
			assertTrue(dispatcher.generators.containsKey(taskTable1.getName()));
			assertTrue(dispatcher.generators.containsKey(taskTable2.getName()));
			assertTrue(dispatcher.generators.containsKey(taskTable3.getName()));
			assertTrue(dispatcher.generators.containsKey(taskTable4.getName()));

			// Verify creation of routing creation
			L1Routing routing = TestL1Utils.buildL1Routing();
			assertTrue("Invalid number of routes", routing.getRoutes().size() == dispatcher.routingMap.size());
			routing.getRoutes().forEach(route -> {
				String key = route.getFrom().getAcquisition() + "_" + route.getFrom().getSatelliteId();
				assertTrue("The key does not exists " + key, dispatcher.routingMap.containsKey(key));
				assertTrue("Invalid number of task tables for " + key,
						route.getTo().getTaskTables().size() == dispatcher.routingMap.get(key).size());
			});

		} catch (BuildTaskTableException | JobDispatcherException e) {
			fail("Invalid raised exception: " + e.getMessage());
		}
	}

	@Test
	public void testDispatchIWA() throws ParseException {
		try {
			L0Slice sliceA = new L0Slice("IW");
			L0SliceProduct productA = new L0SliceProduct(
					"S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE", "A", "S1",
					TestDateUtils.convertDateIso("20171213T142312"), TestDateUtils.convertDateIso("20171213T142312"),
					sliceA);
			Job<L0Slice> jobA = new Job<>(productA);
			this.dispatcher.initialize();
			this.dispatcher.dispatch(jobA);
			verify(mockGeneratorOther, never()).addJob(Mockito.any());
			verify(mockGeneratorIW, times(5)).addJob(Mockito.eq(jobA));

		} catch (BuildTaskTableException | JobDispatcherException | JobGenerationException e) {
			fail("Invalid raised exception: " + e.getMessage());
		}
	}

	@Test
	public void testDispatchIWB() throws ParseException {
		try {
			L0Slice sliceA = new L0Slice("IW");
			L0SliceProduct productA = new L0SliceProduct(
					"S1B_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE", "B", "S1",
					TestDateUtils.convertDateIso("20171213T142312"), TestDateUtils.convertDateIso("20171213T142312"),
					sliceA);
			Job<L0Slice> jobA = new Job<>(productA);
			this.dispatcher.initialize();
			this.dispatcher.dispatch(jobA);
			verify(mockGeneratorOther, never()).addJob(Mockito.any());
			verify(mockGeneratorIW, times(3)).addJob(Mockito.eq(jobA));

		} catch (BuildTaskTableException | JobDispatcherException | JobGenerationException e) {
			fail("Invalid raised exception: " + e.getMessage());
		}
	}

	@Test
	public void testDispatchOther() throws ParseException {
		try {
			L0Slice sliceA = new L0Slice("EW");
			L0SliceProduct productA = new L0SliceProduct(
					"S1A_EW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE", "A", "S1",
					TestDateUtils.convertDateIso("20171213T142312"), TestDateUtils.convertDateIso("20171213T142312"),
					sliceA);
			Job<L0Slice> jobA = new Job<>(productA);
			this.dispatcher.initialize();
			this.dispatcher.dispatch(jobA);
			verify(mockGeneratorIW, never()).addJob(Mockito.any());
			verify(mockGeneratorOther, times(5)).addJob(Mockito.eq(jobA));

		} catch (BuildTaskTableException | JobDispatcherException | JobGenerationException e) {
			fail("Invalid raised exception: " + e.getMessage());
		}
	}

	@Test(expected = JobDispatcherException.class)
	public void testDispatchInvalid()
			throws ParseException, BuildTaskTableException, JobDispatcherException, JobGenerationException {
		L0Slice sliceA = new L0Slice("ZZ");
		L0SliceProduct productA = new L0SliceProduct(
				"S1A_EW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE", "A", "S1",
				TestDateUtils.convertDateIso("20171213T142312"), TestDateUtils.convertDateIso("20171213T142312"),
				sliceA);
		Job<L0Slice> jobA = new Job<>(productA);
		this.dispatcher.initialize();
		this.dispatcher.dispatch(jobA);
	}
}
