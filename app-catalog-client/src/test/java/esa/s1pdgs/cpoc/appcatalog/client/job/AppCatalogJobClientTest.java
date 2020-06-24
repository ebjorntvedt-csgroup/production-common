package esa.s1pdgs.cpoc.appcatalog.client.job;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGeneration;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogJobNewApiError;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogJobPatchApiError;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogJobPatchGenerationApiError;
import esa.s1pdgs.cpoc.common.errors.appcatalog.AppCatalogJobSearchApiError;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;


public class AppCatalogJobClientTest {

    /**
     * Rest template
     */
    @Mock
    private RestTemplate restTemplate;

    
    /**
     * Client to test
     */
    private AppCatalogJobClient<CatalogEvent> client;
    
    private static final CatalogEvent DUMMY = new CatalogEvent();

    /**
     * Initialization
     */
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        client = new AppCatalogJobClient<>(restTemplate, "http://localhost:8080", 3, 200);
    }

    @Test
    public void testConstructor() {
        assertEquals(3, client.getMaxRetries());
        assertEquals(200, client.getTempoRetryMs());
        assertEquals("http://localhost:8080", client.getHostUri());
    }

    @SuppressWarnings("unchecked")
	@Test(expected = AppCatalogJobSearchApiError.class)
    public void testSearchWhenError() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
	        .when(restTemplate).exchange(
	        		Mockito.any(URI.class),
	                Mockito.any(HttpMethod.class),
	                Mockito.any(),
	                Mockito.any(ParameterizedTypeReference.class)
	    );        
        final Map<String, String> filters = new HashMap<>();
        filters.put("filter1", "value1");
        filters.put("filter2", "value2");
        client.search(filters);
    }
    
    @SuppressWarnings("unchecked")
	private final void runSearchTest(final Callable<Void> callable, final String uriArgs) throws Exception {   
    	final URI expectedUri = new URI("http://localhost:8080/jobs/search?" + uriArgs);
    	doReturn(new ResponseEntity<AppDataJob>(HttpStatus.OK))
	    	.when(restTemplate).exchange(
		        		Mockito.any(URI.class),
		                Mockito.any(HttpMethod.class),
		                Mockito.any(),
		                Mockito.any(ParameterizedTypeReference.class)
	    );
        callable.call();
        
        verify(restTemplate, times(1)).exchange(
                Mockito.eq(expectedUri),
                Mockito.eq(HttpMethod.GET),
                Mockito.isNull(),
                Mockito.eq(new ParameterizedTypeReference<List<AppDataJob>>() {})
        );
        verifyNoMoreInteractions(restTemplate);
    }

    @Test
    public void testSearch() throws Exception {
    	runSearchTest(
    			() -> {
    		        final Map<String, String> filters = new HashMap<>();
    		        filters.put("filter1", "value1");
    		        filters.put("filter2", "value2");
    		        client.search(filters);
    				return null;
    			}, 
    			"filter1=value1&filter2=value2"
    	);
    }

    @Test
    public void testfindByMessagesIdentifier() throws Exception {  
    	runSearchTest(
    			() -> {
    		        client.findByMessagesId(12L);
    		        return null;
    			}, 
    			"messages.id=12"
    	);
    } 

	@Test
    public void testfindByPodAndState() throws Exception {
    	runSearchTest(
    			() -> {
    				client.findByPodAndState("pod-name", AppDataJobState.DISPATCHING);
    		        return null;
    			}, 
    			"pod=pod-name&state=DISPATCHING"
    	);
    }

    @Test
    public void testfindNByPodAndGenerationTaskTableWithNotSentGeneration() throws Exception {
    	runSearchTest(
    			() -> {
    			    client.findNByPodAndGenerationTaskTableWithNotSentGeneration("pod-name","task-table");
    		        return null;
    			}, 
    			"[orderByAsc]=generations.lastUpdateDate&pod=pod-name&"
    			+ "generations.state[neq]=SENT&generations.taskTable=task-table"
    	);
    }

    private AppDataJob buildJob() {
        final AppDataJob job = new AppDataJob();
        job.setId(142);
        job.setState(AppDataJobState.DISPATCHING);
        
        final AppDataJobProduct product = new AppDataJobProduct();
        product.setProductName("toto");
        job.setProduct(product);
        
        final GenericMessageDto<CatalogEvent> message1 = new GenericMessageDto<CatalogEvent>(1, "key1", DUMMY);
        final GenericMessageDto<CatalogEvent> message2 = new GenericMessageDto<CatalogEvent>(2, "key2", DUMMY);
        job.setMessages(Arrays.asList(message1, message2));
        
        final AppDataJobGeneration gen1 = new AppDataJobGeneration();
        gen1.setTaskTable("tasktable1");
        gen1.setState(AppDataJobGenerationState.INITIAL);
        gen1.setCreationDate(new Date(0L));
        final AppDataJobGeneration gen2 = new AppDataJobGeneration();
        gen2.setTaskTable("tasktable2");
        gen2.setState(AppDataJobGenerationState.READY);
        gen2.setCreationDate(new Date(0L));
        job.setGenerations(Arrays.asList(gen1, gen2));
        return job;
    }

    @SuppressWarnings("unchecked")
	@Test(expected = AppCatalogJobNewApiError.class)
    public void testNewWhenError() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
	        .when(restTemplate).exchange(
	        		Mockito.anyString(),
	                Mockito.eq(HttpMethod.POST),
	                Mockito.any(HttpEntity.class),
	                Mockito.any(ParameterizedTypeReference.class)
	    );
        client.newJob(buildJob());
    }

    @SuppressWarnings("unchecked")
	@Test
    public void testNew() throws AbstractCodedException {
    	final AppDataJob job = buildJob();
    	doReturn(new ResponseEntity<AppDataJob>(job, HttpStatus.OK))
	    	.when(restTemplate).exchange(
	        		Mockito.anyString(),
	                Mockito.eq(HttpMethod.POST),
	                Mockito.any(HttpEntity.class),
	                Mockito.any(ParameterizedTypeReference.class)
	    );
        final AppDataJob result = client.newJob(job);
        assertEquals(job, result);
        verify(restTemplate, times(1)).exchange(
                Mockito.eq("http://localhost:8080/jobs"),
                Mockito.eq(HttpMethod.POST),
                Mockito.eq(new HttpEntity<AppDataJob>(job)),
                  Mockito.eq(new ParameterizedTypeReference<AppDataJob>() {})
        );
        verifyNoMoreInteractions(restTemplate);        
    }

    @SuppressWarnings("unchecked")
	@Test(expected = AppCatalogJobPatchApiError.class)
    public void testPatchWhenError() throws AbstractCodedException {
        doThrow(new RestClientException("rest client exception"))
	        .when(restTemplate).exchange(
	        		Mockito.anyString(),
	                Mockito.eq(HttpMethod.PATCH),
	                Mockito.any(HttpEntity.class),
	                Mockito.any(ParameterizedTypeReference.class)
	    );    	
        final AppDataJob job = buildJob();
        client.updateJob(job);
    }
    
	@SuppressWarnings("unchecked")
	private final AppDataJob runPatchTest(final AppDataJob job, final Callable<AppDataJob> callable) throws Exception {   
    	doReturn(new ResponseEntity<AppDataJob>(job, HttpStatus.OK))
    	
	    	.when(restTemplate).exchange(
	        		Mockito.anyString(),
	                Mockito.eq(HttpMethod.PATCH),
	                Mockito.any(HttpEntity.class),
	                Mockito.any(ParameterizedTypeReference.class)
	    );
        final AppDataJob result = callable.call();     
        verify(restTemplate, times(1)).exchange(
                Mockito.eq("http://localhost:8080/jobs/142"),
                Mockito.eq(HttpMethod.PATCH),
                Mockito.eq(new HttpEntity<AppDataJob>(result)),
                Mockito.eq(new ParameterizedTypeReference<AppDataJob>() {})
        );
        verifyNoMoreInteractions(restTemplate);
        return result;
    }

	@Test
    public void testPatch() throws Exception {
    	final AppDataJob job = buildJob();
    	final AppDataJob result = runPatchTest(
    			job,
    			() ->  client.updateJob(job)
    	);
    	assertEquals(job, result);    	
    }

	@Test
    public void testUpdate() throws Exception {
        final AppDataJob job = buildJob();
        final AppDataJob result = runPatchTest(
    			job,
    			() -> client.updateJob(job)
    	);        
        assertEquals(job.getMessages().size(), result.getMessages().size());
        assertEquals(job.getProduct(), result.getProduct());
        assertEquals(job.getGenerations(), result.getGenerations());
    }
        
    @SuppressWarnings("unchecked")
	@Test(expected = AppCatalogJobPatchGenerationApiError.class)
    public void testPatchTaskTableWhenError() throws AbstractCodedException {
        final AppDataJob job = buildJob();
        doThrow(new RestClientException("rest client exception"))
	    	.when(restTemplate).exchange(
	    		Mockito.anyString(),
	            Mockito.eq(HttpMethod.PATCH),
	            Mockito.any(HttpEntity.class),
	            Mockito.any(ParameterizedTypeReference.class)
	    );   
        client.patchTaskTableOfJob(
        		job.getId(),
                job.getGenerations().get(0).getTaskTable(),
                AppDataJobGenerationState.SENT
        );
	}

    @SuppressWarnings("unchecked")
	@Test
    public void testPatchTaskTable() throws Exception {
    	final AppDataJob job = buildJob();    	
    	doReturn(new ResponseEntity<AppDataJob>(job, HttpStatus.OK))
	    	.when(restTemplate).exchange(
	        		Mockito.anyString(),
	                Mockito.eq(HttpMethod.PATCH),
	                Mockito.any(HttpEntity.class),
	                Mockito.any(ParameterizedTypeReference.class)
	    );
	    client.patchTaskTableOfJob(
                job.getId(), 
                "tasktable2",
                AppDataJobGenerationState.SENT
        );
	    verify(restTemplate, times(1)).exchange(
	            Mockito.eq("http://localhost:8080/jobs/142/generations/tasktable2"),
	            Mockito.eq(HttpMethod.PATCH),
	            Mockito.any(),
	              Mockito.eq(new ParameterizedTypeReference<AppDataJob>() {})
	    );
	    verifyNoMoreInteractions(restTemplate);
    }
}
