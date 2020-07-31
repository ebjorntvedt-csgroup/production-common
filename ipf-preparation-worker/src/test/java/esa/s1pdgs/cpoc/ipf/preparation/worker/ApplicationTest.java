package esa.s1pdgs.cpoc.ipf.preparation.worker;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.IpfPreparationService;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class ApplicationTest {

	@Autowired
	private IpfPreparationService service;

	@Test
	public void applicationContextTest() throws Exception {
		Application.main(new String[] {});
		System.out.println(service.onMessage(newMessage()));
	}
	
	private final GenericMessageDto<IpfPreparationJob> newMessage() {
		final GenericMessageDto<IpfPreparationJob> mess = new GenericMessageDto<IpfPreparationJob>();
		mess.setBody(newPrepJob());
		return mess;
	}

	private IpfPreparationJob newPrepJob() {
		final IpfPreparationJob job = new IpfPreparationJob();
		job.setAppDataJob(newJob());
		return job;
	}

	private AppDataJob newJob() {
		final AppDataJob job = new AppDataJob();
		job.setProductName("fooBar");
		job.getMessages().add(new GenericMessageDto<CatalogEvent>(1, "foo", newCatEvent()));
		return job;
	}

	private CatalogEvent newCatEvent() {
		final CatalogEvent catEvent = new CatalogEvent();
		catEvent.setMetadata(newMetadata());
		return catEvent;
	}

	private Map<String, Object> newMetadata() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("sessionId", "123");
		map.put("stationCode", "DIMS");
		
		return map;
	}
}
