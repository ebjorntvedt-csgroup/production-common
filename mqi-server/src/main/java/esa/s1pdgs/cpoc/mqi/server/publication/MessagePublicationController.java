package esa.s1pdgs.cpoc.mqi.server.publication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiCategoryNotAvailable;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiPublicationError;
import esa.s1pdgs.cpoc.common.errors.mqi.MqiRouteNotAvailable;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelProductDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelReportDto;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelSegmentDto;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties.ProductCategoryProperties;
import esa.s1pdgs.cpoc.mqi.server.ApplicationProperties.ProductCategoryPublicationProperties;
import esa.s1pdgs.cpoc.mqi.server.KafkaProperties;
import esa.s1pdgs.cpoc.mqi.server.converter.XmlConverter;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.AbstractGenericProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.AuxiliaryFileProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.CompressedProductProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.EdrsSessionsProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.ErrorsProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.LevelJobProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.LevelProductProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.LevelReportProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.kafka.producer.LevelSegmentProducer;
import esa.s1pdgs.cpoc.mqi.server.publication.routing.DefaultRoute;
import esa.s1pdgs.cpoc.mqi.server.publication.routing.Route;
import esa.s1pdgs.cpoc.mqi.server.publication.routing.Routing;

/**
 * Manager of consumers
 * 
 * @author Viveris Technologies
 */
@Controller
public class MessagePublicationController {

	/**
	 * Logger
	 */
	protected static final Logger LOGGER = LogManager.getLogger(MessagePublicationController.class);

	/**
	 * List of producers
	 */
	protected final Map<ProductCategory, AbstractGenericProducer<?>> producers;

	/**
	 * List of producers
	 */
	protected final Map<ProductCategory, Routing> routing;

	/**
	 * Application properties
	 */
	private final ApplicationProperties appProperties;

	/**
	 * Kafka properties
	 */
	private final KafkaProperties kafkaProperties;

	/**
	 * XML converter
	 */
	private final XmlConverter xmlConverter;

	/**
	 * Producer dedicated to errors
	 */
	private final ErrorsProducer errorsProducer;

	/**
	 * Constructor
	 * 
	 * @param appProperties
	 * @param kafkaProperties
	 */
	@Autowired
	public MessagePublicationController(final ApplicationProperties appProperties,
			final KafkaProperties kafkaProperties, final XmlConverter xmlConverter,
			final ErrorsProducer errorsProducer) {
		this.producers = new HashMap<>();
		this.routing = new HashMap<>();
		this.appProperties = appProperties;
		this.kafkaProperties = kafkaProperties;
		this.xmlConverter = xmlConverter;
		this.errorsProducer = errorsProducer;
	}

	/**
	 * Start consumers according the configuration
	 * 
	 * @throws JAXBException
	 * @throws IOException
	 */
	@PostConstruct
	public void initialize() throws IOException, JAXBException {

		// Init the list of consumers

		for (ProductCategory cat : appProperties.getProductCategories().keySet()) {
			ProductCategoryProperties catProp = appProperties.getProductCategories().get(cat);
			ProductCategoryPublicationProperties prop = catProp.getPublication();

			if (prop.isEnable()) {

				// Create publisher
				LOGGER.info("Creating publisher for category {}", cat);
				switch (cat) {
				case AUXILIARY_FILES:
					producers.put(cat, new AuxiliaryFileProducer(kafkaProperties));
					break;
				case EDRS_SESSIONS:
					producers.put(cat, new EdrsSessionsProducer(kafkaProperties));
					break;
				case LEVEL_JOBS:
					producers.put(cat, new LevelJobProducer(kafkaProperties));
					break;
				case LEVEL_PRODUCTS:
					producers.put(cat, new LevelProductProducer(kafkaProperties));
					break;
				case LEVEL_REPORTS:
					producers.put(cat, new LevelReportProducer(kafkaProperties));
					break;
				case LEVEL_SEGMENTS:
					producers.put(cat, new LevelSegmentProducer(kafkaProperties));
					break;
				case COMPRESSED_PRODUCTS:
					producers.put(cat, new CompressedProductProducer(kafkaProperties));
					break;
				}

				// Create routing map
				LOGGER.info("Creating routing map for category {}", cat);
				routing.put(cat, (Routing) xmlConverter.convertFromXMLToObject(prop.getRoutingFile()));
			}
		}
	}

	/**
	 * Publish a message in the topic of errors
	 * 
	 * @param message
	 */
	public boolean publishError(final String message) {
		return this.errorsProducer.send(message);
	}

	/**
	 * Publish a message of a given category
	 * 
	 * @param category
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	public void publish(ProductCategory category, Object dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		if (producers.containsKey(category)) {
			switch (category) {
			case AUXILIARY_FILES:
				publishAuxiliaryFiles((ProductDto) dto, inputKey, outputKey);
				break;
			case EDRS_SESSIONS:
				publishEdrsSessions((EdrsSessionDto) dto, inputKey, outputKey);
				break;
			case LEVEL_JOBS:
				publishLevelJobs((LevelJobDto) dto, inputKey, outputKey);
				break;
			case LEVEL_PRODUCTS:
				publishLevelProducts((LevelProductDto) dto, inputKey, outputKey);
				break;
			case LEVEL_REPORTS:
				publishLevelReports((LevelReportDto) dto, inputKey, outputKey);
				break;
			case LEVEL_SEGMENTS:
				publishLevelSegments((LevelSegmentDto) dto, inputKey, outputKey);
				break;
			case COMPRESSED_PRODUCTS:
				publishCompressedProducts((ProductDto)dto, inputKey, outputKey);
				break;
			}
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Get the topic to use for publication according the routing map
	 * 
	 * @param category
	 * @param family
	 * @return
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected String getTopic(final ProductCategory category, final ProductFamily family, final String inputKey,
			final String outputKey) throws MqiCategoryNotAvailable, MqiRouteNotAvailable {
		String result = "";
		if (routing.containsKey(category)) {
			Route rte = routing.get(category).getRoute(inputKey, outputKey);
			if (rte == null) {
				DefaultRoute dft = routing.get(category).getDefaultRoute(family);
				if (dft == null) {
					throw new MqiRouteNotAvailable(category, family);
				} else {
					result = dft.getRouteTo().getTopic();
				}
			} else {
				result = rte.getRouteTo().getTopic();
			}
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
		return result;
	}

	/**
	 * Publish a message for the category AUXILIARY_FILES
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishAuxiliaryFiles(final ProductDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.AUXILIARY_FILES;
		if (producers.containsKey(category)) {
			AuxiliaryFileProducer producer = (AuxiliaryFileProducer) producers.get(category);
			producer.send(getTopic(category, ProductFamily.AUXILIARY_FILE, inputKey, outputKey), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}
	
	/**
	 * Publish a message for the category COMPRESSED_PRODUCTS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishCompressedProducts(final ProductDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.COMPRESSED_PRODUCTS;
		if (producers.containsKey(category)) {
			CompressedProductProducer producer = (CompressedProductProducer)  producers.get(category);
			//FIXME: Hard coded kafka topic (sorry)
			producer.send("t-pdgs-compressed-products", dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Publish a message for the category EDRS_SESSIONS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishEdrsSessions(final EdrsSessionDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.EDRS_SESSIONS;
		if (producers.containsKey(category)) {
			EdrsSessionsProducer producer = (EdrsSessionsProducer) producers.get(category);
			producer.send(getTopic(category, ProductFamily.EDRS_SESSION, inputKey, outputKey), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Publish a message for the category LEVEL_PRODUCTS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishLevelProducts(final LevelProductDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.LEVEL_PRODUCTS;
		if (producers.containsKey(category)) {
			LevelProductProducer producer = (LevelProductProducer) producers.get(category);
			producer.send(getTopic(category, dto.getFamily(), inputKey, outputKey), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Publish a message for the category LEVEL_SEGMENTS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishLevelSegments(final LevelSegmentDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.LEVEL_SEGMENTS;
		if (producers.containsKey(category)) {
			LevelSegmentProducer producer = (LevelSegmentProducer) producers.get(category);
			producer.send(getTopic(category, dto.getFamily(), inputKey, outputKey), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Publish a message for the category LEVEL_JOBS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishLevelJobs(final LevelJobDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.LEVEL_JOBS;
		if (producers.containsKey(category)) {
			LevelJobProducer producer = (LevelJobProducer) producers.get(category);
			producer.send(getTopic(category, dto.getFamily(), inputKey, outputKey), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

	/**
	 * Publish a message for the category LEVEL_REPORTS
	 * 
	 * @param dto
	 * @throws MqiPublicationError
	 * @throws MqiCategoryNotAvailable
	 * @throws MqiRouteNotAvailable
	 */
	protected void publishLevelReports(final LevelReportDto dto, String inputKey, String outputKey)
			throws MqiPublicationError, MqiCategoryNotAvailable, MqiRouteNotAvailable {
		ProductCategory category = ProductCategory.LEVEL_REPORTS;
		if (producers.containsKey(category)) {
			LevelReportProducer producer = (LevelReportProducer) producers.get(category);
			producer.send(getTopic(category, dto.getFamily(), inputKey, "NONE"), dto);
		} else {
			throw new MqiCategoryNotAvailable(category, "publisher");
		}
	}

}
