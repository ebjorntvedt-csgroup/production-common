package fr.viveris.s1pdgs.jobgenerator.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.FileCopyUtils;

import fr.viveris.s1pdgs.jobgenerator.exception.AbstractCodedException;
import fr.viveris.s1pdgs.jobgenerator.exception.InvalidFormatProduct;
import fr.viveris.s1pdgs.jobgenerator.exception.ObsS3Exception;
import fr.viveris.s1pdgs.jobgenerator.model.EdrsSessionFile;
import fr.viveris.s1pdgs.jobgenerator.service.s3.SessionFilesS3Services;
import fr.viveris.s1pdgs.jobgenerator.utils.TestL0Utils;

public class EdrsSessionFileServiceTest {

	/**
	 * S3 service
	 */
	@Mock
	private SessionFilesS3Services s3Services;

	/**
	 * XML converter
	 */
	@Mock
	private XmlConverter xmlConverter;

	private EdrsSessionFileService service;

	private File fileCh1;

	private File fileCh2;

	private EdrsSessionFile session1;

	private EdrsSessionFile session2;

	/**
	 * Test set up
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		// Mcokito
		MockitoAnnotations.initMocks(this);
		
		(new File("./tmp")).mkdirs();

		service = new EdrsSessionFileService(s3Services, xmlConverter, "./tmp/");
		fileCh1 = new File("./tmp/DCS_02_L20171109175634707000125_ch1_DSIB.xml");
		fileCh1.createNewFile();
		fileCh2 = new File("./tmp/DCS_02_L20171109175634707000125_ch2_DSIB.xml");
		fileCh2.createNewFile();
		FileCopyUtils.copy(new File("./test/data/DCS_02_L20171109175634707000125_ch1_DSIB.xml"), fileCh1);
		FileCopyUtils.copy(new File("./test/data/DCS_02_L20171109175634707000125_ch2_DSIB.xml"), fileCh2);
		session1 = TestL0Utils.createEdrsSessionFileChannel1(true);
		session2 = TestL0Utils.createEdrsSessionFileChannel2(true);

		// Mock the dispatcher
		Mockito.doAnswer(i -> {
			return fileCh1;
		}).when(s3Services).getFile(Mockito.eq("S1A/SESSION_1/ch1/KEY_OBS_SESSION_1_1.xml"), Mockito.any());
		Mockito.doAnswer(i -> {
			return fileCh2;
		}).when(s3Services).getFile(Mockito.eq("S1A/SESSION_1/ch2/KEY_OBS_SESSION_1_2.xml"), Mockito.any());
		Mockito.doAnswer(i -> {
			return fileCh2;
		}).when(s3Services).getFile(Mockito.eq("KEY_OBS_SESSION_1_2.xml"), Mockito.any());

		// Mock the XML converter
		Mockito.doAnswer(i -> {
			return session1;
		}).when(xmlConverter).convertFromXMLToObject(Mockito.eq(fileCh1.getAbsolutePath()));
		Mockito.doAnswer(i -> {
			return session2;
		}).when(xmlConverter).convertFromXMLToObject(Mockito.eq(fileCh2.getAbsolutePath()));

	}

	@Test
	public void testCreateSessionFile() throws AbstractCodedException {
		try {
			EdrsSessionFile r1 = service.createSessionFile("S1A/SESSION_1/ch1/KEY_OBS_SESSION_1_1.xml");
			Mockito.verify(s3Services, times(1)).getFile(Mockito.eq("S1A/SESSION_1/ch1/KEY_OBS_SESSION_1_1.xml"),
					Mockito.eq("./tmp/KEY_OBS_SESSION_1_1.xml"));
			Mockito.verify(xmlConverter, times(1)).convertFromXMLToObject(Mockito.eq(fileCh1.getAbsolutePath()));
			assertEquals(session1, r1);

			EdrsSessionFile r2 = service.createSessionFile("S1A/SESSION_1/ch2/KEY_OBS_SESSION_1_2.xml");
			Mockito.verify(s3Services, times(1)).getFile(Mockito.eq("S1A/SESSION_1/ch2/KEY_OBS_SESSION_1_2.xml"),
					Mockito.eq("./tmp/KEY_OBS_SESSION_1_2.xml"));
			Mockito.verify(xmlConverter, times(1)).convertFromXMLToObject(Mockito.eq(fileCh2.getAbsolutePath()));
			assertEquals(session2, r2);

			EdrsSessionFile r3 = service.createSessionFile("KEY_OBS_SESSION_1_2.xml");
			Mockito.verify(s3Services, times(1)).getFile(Mockito.eq("KEY_OBS_SESSION_1_2.xml"),
					Mockito.eq("./tmp/KEY_OBS_SESSION_1_2.xml"));
			Mockito.verify(xmlConverter, times(2)).convertFromXMLToObject(Mockito.eq(fileCh2.getAbsolutePath()));
			assertEquals(session2, r3);

		} catch (ObsS3Exception | InvalidFormatProduct | IOException | JAXBException e) {
			fail("Invalid exception raised " + e.getMessage());
		}
	}
}
