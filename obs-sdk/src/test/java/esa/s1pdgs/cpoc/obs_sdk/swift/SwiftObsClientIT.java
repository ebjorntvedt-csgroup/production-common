package esa.s1pdgs.cpoc.obs_sdk.swift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.obs_sdk.ObsConfigurationProperties;
import esa.s1pdgs.cpoc.obs_sdk.ObsDownloadObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsValidationException;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:obs-swift.properties")
@ContextConfiguration(classes = {ObsConfigurationProperties.class})
public class SwiftObsClientIT {

	public final static ProductFamily auxiliaryFiles = ProductFamily.AUXILIARY_FILE;
	public final static String testFilePrefix = "abc/def/";
	public final static String testFileName1 = "testfile1.txt";
	public final static String testFileName2 = "testfile2.txt";
	public final static String testUnexptectedFileName = "unexpected.txt";
	public final static String testDirectoryName = "testdir";
	public final static File testFile1 = getResource("/" + testFileName1);
	public final static File testFile2 = getResource("/" + testFileName2);
	public final static File testDirectory = getResource("/" + testDirectoryName);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Autowired
	private ObsConfigurationProperties configuration;
	
	private SwiftObsClient uut;
	
	public static File getResource(String fileName) {
		try {
			return new File(SwiftObsClientIT.class.getClass().getResource(fileName).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException("Could not get resource");
		}
	}
	
	@Before
	public void setUp() throws ObsException, SdkClientException {	
		uut = (SwiftObsClient) new SwiftObsClient.Factory().newObsClient(configuration);
		
		// prepare environment
		if (!uut.containerExists(auxiliaryFiles)) {
			uut.createContainer(auxiliaryFiles);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testFileName1))) {
			uut.deleteObject(auxiliaryFiles, testFileName1);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testFileName2))) {
			uut.deleteObject(auxiliaryFiles, testFileName2);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1))) {
			uut.deleteObject(auxiliaryFiles, testFilePrefix + testFileName1);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName2))) {
			uut.deleteObject(auxiliaryFiles, testFilePrefix + testFileName2);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1))) {
			uut.deleteObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2))) {
			uut.deleteObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName))) {
			uut.deleteObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName);
		}

		if (uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum"))) {
			uut.deleteObject(auxiliaryFiles, testDirectoryName + ".md5sum");
		}
	}
	
	@Test
	public void uploadWithoutPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		uut.upload(Collections.singletonList(new ObsUploadObject(auxiliaryFiles, testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
	}

	@Test
	public void uploadWithPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		uut.upload(Collections.singletonList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
	}

	@Test
	public void uploadAndValidationOfCompleteDirectoryTest() throws IOException, SdkClientException, AbstractCodedException, ObsValidationException {	
		// upload directory
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName, testDirectory)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));

		// validate complete directory
		uut.validate(new ObsObject(auxiliaryFiles, testDirectoryName));
	}
	
	@Test
	public void uploadAndValidationOfDirectoryWithUnexpectedObejectTest() throws IOException, SdkClientException, AbstractCodedException, ObsValidationException {	
		// upload directory
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName, testDirectory)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));

		// upload unexpected obkect
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName, testFile1)));		
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName)));

		// validate directory with unexpected object
		exception.expect(ObsValidationException.class);
		uut.validate(new ObsObject(auxiliaryFiles, testDirectoryName));				
	}
	
	@Test
	public void uploadAndValidationOfIncompleteDirectoryTest() throws IOException, SdkClientException, AbstractCodedException, ObsValidationException {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName, testDirectory)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));

		// remove object from directory
		uut.deleteObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1);
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));

		// validate incomplete directory
		exception.expect(ObsValidationException.class);
		exception.expectMessage("Object not found: " + testDirectoryName + "/" + testFileName1 + " of family " + auxiliaryFiles); 
		uut.validate(new ObsObject(auxiliaryFiles, testDirectoryName));
	}
	
	@Test
	public void uploadAndValidationOfDirectoryWithWrongChecksumTest() throws IOException, SdkClientException, AbstractCodedException, ObsValidationException {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testUnexptectedFileName)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName, testDirectory)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));

		// replace object with bad one
		uut.deleteObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1);
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1, testFile2)));		
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));

		// validate wrong checksum situation
		exception.expect(ObsValidationException.class);
		exception.expectMessage("Checksum is wrong for object: " + testDirectoryName + "/" + testFileName1 + " of family " + auxiliaryFiles); 
		uut.validate(new ObsObject(auxiliaryFiles, testDirectoryName));
	}
	
	@Test
	public void uploadAndValidationOfDirectoryWithNonexistentChecksumTest() throws IOException, SdkClientException, AbstractCodedException, ObsValidationException {	
		// validate not existing checksum file
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, "not-existing.md5sum")));
		exception.expect(ObsValidationException.class);
		exception.expectMessage("Checksum file not found for: not-existing of family " + auxiliaryFiles); 
		uut.validate(new ObsObject(auxiliaryFiles, "not-existing"));
	}

	@Test
	public void deleteWithoutPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));

		// delete
		if (uut.exists(new ObsObject(auxiliaryFiles, testFileName1))) {
			uut.deleteObject(auxiliaryFiles, testFileName1);
		}
	}
	
	@Test
	public void deleteWithPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));

		// delete
		if (uut.exists(new ObsObject(auxiliaryFiles, testFileName1))) {
			uut.deleteObject(auxiliaryFiles, testFilePrefix + testFileName1);
		}
	}

	@Test
	public void downloadFileWithoutPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		
		// single file download
        String targetDir = Files.createTempDirectory(this.getClass().getCanonicalName() + "-").toString();
		uut.download(Arrays.asList(new ObsDownloadObject(auxiliaryFiles, testFileName1, targetDir)));
		String send1 = new String(Files.readAllBytes(testFile1.toPath()));
		String received1 = new String(Files.readAllBytes((new File(targetDir + "/" + testFileName1)).toPath()));
		assertEquals(send1, received1);
	}

	@Test
	public void downloadFileWithPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName1, testFile1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		
		// single file download
        String targetDir = Files.createTempDirectory(this.getClass().getCanonicalName() + "-").toString();
		uut.download(Arrays.asList(new ObsDownloadObject(auxiliaryFiles, testFilePrefix + testFileName1, targetDir)));
		String send1 = new String(Files.readAllBytes(testFile1.toPath()));
		String received1 = new String(Files.readAllBytes((new File(targetDir + "/" + testFilePrefix + testFileName1)).toPath()));
		assertEquals(send1, received1);
	}

	@Test
	public void downloadOfDirectoryTest() throws IOException, SdkClientException, AbstractCodedException {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testDirectoryName, testDirectory)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + "/" + testFileName2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testDirectoryName + ".md5sum")));
		
		// multi file download
		String targetDir = Files.createTempDirectory(this.getClass().getCanonicalName() + "-").toString();
		uut.download(Arrays.asList(new ObsDownloadObject(auxiliaryFiles, testDirectoryName + "/", targetDir)));

		String send1 = new String(Files.readAllBytes(new File(testDirectory, testFileName1).toPath()));
		String received1 = new String(Files.readAllBytes((new File(targetDir + "/" + testDirectoryName + "/" + testFileName1)).toPath()));
		assertEquals(send1, received1);
		
		String send2 = new String(Files.readAllBytes(new File(testDirectory, testFileName2).toPath()));
		String received2 = new String(Files.readAllBytes((new File(targetDir + "/" + testDirectoryName + "/" + testFileName2)).toPath()));
		assertEquals(send2, received2);
		
		assertFalse(new File(targetDir + "/" + testDirectoryName + ".md5sum").exists());
	}
	
	@Test
	public void numberOfObjectsWithoutPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFileName2)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFileName1, testFile1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFileName2, testFile2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFileName2)));
		
		// count
		int count = uut.numberOfObjects(auxiliaryFiles, "");
		assertEquals(2, count);
	}

	@Test
	public void numberOfObjectsWithPrefixTest() throws Exception {	
		// upload
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName2)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName1, testFile1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName2, testFile2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName2)));
		
		// count
		int count = uut.numberOfObjects(auxiliaryFiles, testFilePrefix);
		assertEquals(2, count);
	}
	
	@Test
	public final void getAllAsStreamTest() throws Exception {
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		assertFalse(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName2)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName1, testFile1)));
		uut.upload(Arrays.asList(new ObsUploadObject(auxiliaryFiles, testFilePrefix + testFileName2, testFile2)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName1)));
		assertTrue(uut.exists(new ObsObject(auxiliaryFiles, testFilePrefix + testFileName2)));
		
		final Map<String,InputStream> res = uut.getAllAsInputStream(auxiliaryFiles, testFilePrefix);
		for (final Map.Entry<String,InputStream> entry : res.entrySet()) {
			try (final InputStream in = entry.getValue()) {
				final String content = IOUtils.toString(in);
				
				if ("abc/def/testfile1.txt".equals(entry.getKey())) {
					assertEquals("test", content);
				}
				else if ("abc/def/testfile2.txt".equals(entry.getKey())) {
					assertEquals("test2", content);
				}
				else {
					fail();
				}
			}
		}
	}
}
