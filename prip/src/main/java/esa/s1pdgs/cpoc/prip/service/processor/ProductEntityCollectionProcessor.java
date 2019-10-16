package esa.s1pdgs.cpoc.prip.service.processor;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import esa.s1pdgs.cpoc.prip.model.PripDateTimeFilter;
import esa.s1pdgs.cpoc.prip.model.PripMetadata;
import esa.s1pdgs.cpoc.prip.model.PripTextFilter;
import esa.s1pdgs.cpoc.prip.service.edm.EdmProvider;
import esa.s1pdgs.cpoc.prip.service.mapping.MappingUtil;
import esa.s1pdgs.cpoc.prip.service.metadata.PripMetadataRepository;
import esa.s1pdgs.cpoc.prip.service.processor.visitor.ProductsFilterVisitor;

public class ProductEntityCollectionProcessor
		implements org.apache.olingo.server.api.processor.EntityCollectionProcessor {

	private OData odata;
	private ServiceMetadata serviceMetadata;
	private PripMetadataRepository pripMetadataRepository;

	public ProductEntityCollectionProcessor(PripMetadataRepository pripMetadataRepository) {
		this.pripMetadataRepository = pripMetadataRepository;
	}

	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		EntityCollection entitySet = getData(request, edmEntitySet, uriInfo.getSystemQueryOptions());

		ODataSerializer serializer = odata.createSerializer(responseFormat);

		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl)
				.build();
		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet,
				opts);

		InputStream serializedContent = serializerResult.getContent();

		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	private EntityCollection getData(ODataRequest request, EdmEntitySet edmEntitySet,
			List<SystemQueryOption> systemQueryOptions) {
		EntityCollection entityCollection = new EntityCollection();
		if (EdmProvider.ES_PRODUCTS_NAME.equals(edmEntitySet.getName())) {
			List<PripDateTimeFilter> pripDateTimeFilters = Collections.emptyList();
			List<PripTextFilter> pripTextFilters = Collections.emptyList();
			for (SystemQueryOption queryOption : systemQueryOptions) {
				if (queryOption.getKind().equals(SystemQueryOptionKind.FILTER)) {
					if (queryOption instanceof FilterOption) {
						FilterOption filterOption = (FilterOption) queryOption;
						Expression expression = filterOption.getExpression();
						try {
							ProductsFilterVisitor productFilterVistor = new ProductsFilterVisitor();
							expression.accept(productFilterVistor); // also has a return value, which is currently not needed
							pripDateTimeFilters = productFilterVistor.getPripDateTimeFilters();
							pripTextFilters = productFilterVistor.getPripTextFilters();
						} catch (ExpressionVisitException | ODataApplicationException e) {
							// TODO: handle exception instead of returning an empty collection
							return entityCollection;
						}
					}
				}
			}

			List<PripMetadata> queryResult;
			if (pripDateTimeFilters.size() > 0 && pripTextFilters.size() > 0) {
				queryResult = pripMetadataRepository.findByCreationDateAndProductName(pripDateTimeFilters, pripTextFilters);
			} else if (pripDateTimeFilters.size() > 0) {
				queryResult = pripMetadataRepository.findByCreationDate(pripDateTimeFilters);
			} else if (pripTextFilters.size() > 0) {
				queryResult = pripMetadataRepository.findByProductName(pripTextFilters);
			} else {
				queryResult = pripMetadataRepository.findAll();
			}
			
			List<Entity> productList = entityCollection.getEntities();
			for (PripMetadata pripMetadata : queryResult) {
				productList.add(MappingUtil.pripMetadataToEntity(pripMetadata, request.getRawBaseUri()));
			}
		}

		return entityCollection;
	}
}
