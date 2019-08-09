package esa.s1pdgs.cpoc.disseminator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.amazonaws.SdkClientException;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.disseminator.config.DisseminationProperties;
import esa.s1pdgs.cpoc.disseminator.outbox.OutboxClient;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public class TestDisseminationService {
	@Test
	public final void testAssertExists_OnNonExistingFile_ShallThrowException() throws ObsException {
		final FakeObsClient fakeObsClient = new FakeObsClient() {
			@Override public final boolean exist(ProductFamily family, String key) throws ObsException {
				return false;
			}			
		};		
		final DisseminationService uut = new DisseminationService(null, fakeObsClient, new DisseminationProperties(), ErrorRepoAppender.NULL);
		final ProductDto fakeProduct = new ProductDto("fakeProduct", "my/key", ProductFamily.BLANK);
		
		try {
			uut.assertExists(fakeProduct);
			fail();
		} catch (DisseminationException e) {
			assertEquals("OBS file 'my/key' (BLANK) does not exist", e.getMessage());
		}	
	}
	
	@Test
	public final void testAssertExists_OnExistingFile_ShallNotFail() throws ObsException {
		final FakeObsClient fakeObsClient = new FakeObsClient() {
			@Override public final boolean exist(ProductFamily family, String key) throws ObsException {
				return true;
			}			
		};		
		final DisseminationService uut = new DisseminationService(null, fakeObsClient, new DisseminationProperties(), ErrorRepoAppender.NULL);
		final ProductDto fakeProduct = new ProductDto("fakeProduct", "my/key", ProductFamily.BLANK);
		uut.assertExists(fakeProduct);
	}
	
	@Test
	public final void testClientForTarget_OnValidTarget_ShallReturnClientForTarget() {
		final DisseminationService uut = new DisseminationService(null, null, new DisseminationProperties(), ErrorRepoAppender.NULL);
		uut.put("foo", OutboxClient.NULL);		
		assertEquals(OutboxClient.NULL, uut.clientForTarget("foo"));		
	}
	
	@Test
	public final void testClientForTarget_OnInvalidTarget_ShallThrowException() {
		final DisseminationService uut = new DisseminationService(null, null, new DisseminationProperties(), ErrorRepoAppender.NULL);
		uut.put("foo", OutboxClient.NULL);		
		try {
			uut.clientForTarget("bar");
			fail();
		} catch (DisseminationException e) {
			assertEquals("No outbox configured for 'bar'. Available are: [foo]", e.getMessage());
		}		
	}
	
	@Test
	public final void testHandleTransferTo_OnSuccessfulTransfer_ShallNotFail() {
		final FakeObsClient fakeObsClient = new FakeObsClient() {
			@Override public final boolean exist(ProductFamily family, String key) throws ObsException {
				return true;
			}			
		};		
		final DisseminationService uut = new DisseminationService(null, fakeObsClient, new DisseminationProperties(), ErrorRepoAppender.NULL);
		uut.put("foo", OutboxClient.NULL);		
		final ProductDto fakeProduct = new ProductDto("fakeProduct", "my/key", ProductFamily.BLANK);
		final GenericMessageDto<ProductDto> fakeMessage = new GenericMessageDto<ProductDto>(123, "myKey", fakeProduct); 
		uut.handleTransferTo(fakeMessage, "foo");
	}
	
	@Test
	public final void testHandleTransferTo_OnTransferError_ShallThrowException() {
		final FakeObsClient fakeObsClient = new FakeObsClient() {
			@Override public final boolean exist(ProductFamily family, String key) throws ObsException {
				return true;
			}			
		};		
		final OutboxClient failOuboxClient = new OutboxClient() {			
			@Override
			public void transfer(ProductFamily family, String keyObjectStorage) throws SdkClientException, ObsException {
				throw new SdkClientException("EXPECTED");
			}
		};
		
		final DisseminationService uut = new DisseminationService(null, fakeObsClient, new DisseminationProperties(), ErrorRepoAppender.NULL);
		uut.put("foo", failOuboxClient);		
		final ProductDto fakeProduct = new ProductDto("fakeProduct", "my/key", ProductFamily.BLANK);
		final GenericMessageDto<ProductDto> fakeMessage = new GenericMessageDto<ProductDto>(123, "myKey", fakeProduct); 
		try {
			uut.handleTransferTo(fakeMessage, "foo");
		} catch (Exception e) {
			assertEquals(true, e.getMessage().startsWith("Error on dissemination of product to outbox foo: com.amazonaws.SdkClientException: EXPECTED"));
		}
	}
}
