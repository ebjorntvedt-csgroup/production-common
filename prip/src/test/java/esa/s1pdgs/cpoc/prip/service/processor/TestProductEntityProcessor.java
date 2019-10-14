package esa.s1pdgs.cpoc.prip.service.processor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.core.uri.UriResourceEntitySetImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.prip.model.PripMetadata;
import esa.s1pdgs.cpoc.prip.service.metadata.PripMetadataRepository;

public class TestProductEntityProcessor {
	
	ProductEntityProcessor uut;

	@Mock
	PripMetadataRepository pripMetadataRepositoryMock;

	@Mock
	OData odataMock;
	
	@Mock
	UriInfo uriInfoMock;
	
	@Mock
	EdmEntitySet edmEntitySetMock;
	
	@Mock
	ODataRequest odataRequestMock;
	
	@Mock
	UriResourceEntitySet uriResourceEntitySetMock;
	
	@Mock
	UriParameter uriParameterMock;
	
	@Mock
	ODataSerializer odataSerializerMock;
	
	@Mock
	SerializerResult serializerResultMock;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		uut = new ProductEntityProcessor(pripMetadataRepositoryMock);
		uut.init(odataMock, null);
	}

	@Test
	public void TestReadEntity_OnExistentProduct_ShallReturnStatusOk() throws ODataApplicationException, ODataLibraryException, IOException {
		String entity = "Products";
		String uuid = "00000000-0000-0000-0000-000000000001";
		String baseUri = "http://example.org";
		String odataPath = "/" + entity + "(%27" + uuid + "%27)";

		PripMetadata pripMetadata = new PripMetadata();
		pripMetadata.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		
		doReturn(pripMetadata).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(entity).when(edmEntitySetMock).getName();
		
		doReturn(odataSerializerMock).when(odataMock).createSerializer(Mockito.any());
		
		doReturn(serializerResultMock).when(odataSerializerMock).entity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
		doReturn(new ByteArrayInputStream("expected result".getBytes())).when(serializerResultMock).getContent();
		
		ODataResponse odataResponse = new ODataResponse();
		uut.readEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.OK.getStatusCode(), odataResponse.getStatusCode());
		assertEquals("expected result", IOUtils.toString(odataResponse.getContent(), StandardCharsets.UTF_8));
	}

	@Test
	public void TestReadEntity_OnNonExistentProduct_ShallReturnStatusNotFound() throws ODataApplicationException, ODataLibraryException {
		String entity = "Products";
		String uuid = "00000000-0000-0000-0000-000000000002";
		String baseUri = "http://example.org";
		String odataPath = "/" + entity + "(%27" + uuid + "%27)";

		doReturn(null).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		UriResourceEntitySetImpl uriResourceEntitySet = new UriResourceEntitySetImpl(edmEntitySetMock);			
		uriResourceEntitySet.setKeyPredicates(Arrays.asList(uriParameterMock));
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn(entity).when(edmEntitySetMock).getName();
				
		ODataResponse odataResponse = new ODataResponse();
		uut.readEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), odataResponse.getStatusCode());
	}
}
