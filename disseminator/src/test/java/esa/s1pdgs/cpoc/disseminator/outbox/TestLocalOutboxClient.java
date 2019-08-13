package esa.s1pdgs.cpoc.disseminator.outbox;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.utils.FileUtils;
import esa.s1pdgs.cpoc.disseminator.FakeObsClient;
import esa.s1pdgs.cpoc.disseminator.config.DisseminationProperties.OutboxConfiguration;

public class TestLocalOutboxClient {
	private File testDir;
	
	@Before
	public final void setUp() throws IOException {
		testDir = Files.createTempDirectory("foo").toFile();
	}
	
	@After
	public final void tearDown() throws IOException {
		FileUtils.delete(testDir.getPath());
	}
	
	@Test
	public final void testTransfer() throws Exception {
		final FakeObsClient fakeObsClient = new FakeObsClient() {
			@Override
			public File downloadFile(ProductFamily family, String key, String targetDir) {
				final File file = new File(targetDir, key);
				try {
					FileUtils.writeFile(file, "expected content");
				} catch (InternalErrorException e) {
					throw new RuntimeException("foo bar");
				}
				return file;
			}			
		};
		final OutboxConfiguration config = new OutboxConfiguration();
		config.setPath(testDir.getPath());
		
		final LocalOutboxClient outbox = new LocalOutboxClient(fakeObsClient, config);
		outbox.transfer(ProductFamily.BLANK, "foo.bar");
		
		final File expected = new File(testDir, "foo.bar");
		assertEquals(true, expected.exists());
		
		assertEquals("expected content", FileUtils.readFile(expected));		
	}
	

}
