/**
 * 
 */
package fr.viveris.s1pdgs.mdcatalog.controllers.kafka;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import fr.viveris.s1pdgs.mdcatalog.config.MetadataExtractorConfig;
import fr.viveris.s1pdgs.mdcatalog.model.ConfigFileDescriptor;
import fr.viveris.s1pdgs.mdcatalog.model.dto.KafkaConfigFileDto;
import fr.viveris.s1pdgs.mdcatalog.model.exception.AbstractCodedException;
import fr.viveris.s1pdgs.mdcatalog.model.exception.AbstractCodedException.ErrorCode;
import fr.viveris.s1pdgs.mdcatalog.model.exception.FilePathException;
import fr.viveris.s1pdgs.mdcatalog.services.es.EsServices;
import fr.viveris.s1pdgs.mdcatalog.services.files.FileDescriptorBuilder;
import fr.viveris.s1pdgs.mdcatalog.services.files.MetadataBuilder;
import fr.viveris.s1pdgs.mdcatalog.services.s3.ConfigFilesS3Services;

/**
 * KAFKA consumer. Consume on a topic defined in configuration file
 * 
 * @author Olivier Bex-Chauvet
 *
 */
@Service
public class ConfigFileConsumer {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LogManager.getLogger(ConfigFileConsumer.class);

	/**
	 * Pattern for configuration files to extract data
	 */
	private final static String PATTERN_CONFIG = "^([0-9a-z][0-9a-z]){1}([0-9a-z]){1}(_(OPER|TEST))?_(AUX_OBMEMC|AUX_PP1|AUX_CAL|AUX_INS|AUX_RESORB|MPL_ORBPRE|MPL_ORBSCT)_\\w{1,}\\.(XML|EOF|SAFE)(/.*)?$";

	/**
	 * Elasticsearch services
	 */
	private final EsServices esServices;

	/**
	 * Amazon S3 service for configuration files
	 */
	private final ConfigFilesS3Services configFilesS3Services;

	/**
	 * Metadata builder
	 */
	private final MetadataBuilder mdBuilder;

	/**
	 * Local directory for configurations files
	 */
	private final String localDirectory;

	/**
	 * 
	 */
	private final MetadataExtractorConfig extractorConfig;

	/**
	 * Builder of file descriptors
	 */
	private final FileDescriptorBuilder fileDescriptorBuilder;

	/**
	 * Manifest filename
	 */
	private final String manifestFilename;
	private final String fileWithManifestExt;

	@Autowired
	public ConfigFileConsumer(final EsServices esServices, final ConfigFilesS3Services configFilesS3Services,
			@Value("${file.auxiliary-files.local-directory}") final String localDirectory,
			final MetadataExtractorConfig extractorConfig,
			@Value("${file.manifest-filename}") final String manifestFilename,
			@Value("${file.file-with-manifest-ext}") final String fileWithManifestExt) {
		this.localDirectory = localDirectory;
		this.fileDescriptorBuilder = new FileDescriptorBuilder(this.localDirectory,
				Pattern.compile(PATTERN_CONFIG, Pattern.CASE_INSENSITIVE));
		this.extractorConfig = extractorConfig;
		this.mdBuilder = new MetadataBuilder(this.extractorConfig);
		this.esServices = esServices;
		this.configFilesS3Services = configFilesS3Services;
		this.manifestFilename = manifestFilename;
		this.fileWithManifestExt = fileWithManifestExt;
	}

	/**
	 * Message listener container. Read a message
	 * 
	 * @param payload
	 */
	@KafkaListener(topics = "${kafka.topic.auxiliary-files}", groupId = "${kafka.group-id}")
	public void receive(KafkaConfigFileDto dto) {
		LOGGER.info("[MONITOR] [Step 0] [auxiliary] [productName {}] Starting metadata extraction",
				dto.getProductName());

		File metadataFile = null;
		int step = 0;
		// Create metadata
		try {
			step++;
			// Build key object storage
			String keyObs = dto.getKeyObjectStorage();
			if (dto.getKeyObjectStorage().toLowerCase().endsWith(this.fileWithManifestExt.toLowerCase())) {
				keyObs += "/" + manifestFilename;
			}
			// Upload file
			LOGGER.info("[MONITOR] [step 1] [auxiliary] [productName {}] Downloading file {}", dto.getProductName(),
					keyObs);
			if (!configFilesS3Services.exist(keyObs)) {
				throw new FilePathException(dto.getProductName(), dto.getKeyObjectStorage(), "CONFIG",
						"No such Auxiliary files in object storage");
			}
			metadataFile = configFilesS3Services.getFile(keyObs, this.localDirectory + keyObs);

			// Extract metadata from name
			step++;
			LOGGER.info("[MONITOR] [step 2] [auxiliary] [productName {}] Extracting from filename",
					dto.getProductName());
			ConfigFileDescriptor configFileDescriptor = fileDescriptorBuilder.buildConfigFileDescriptor(metadataFile);

			// Build metadata from file and extracted
			step++;
			LOGGER.info("[MONITOR] [step 3] [auxiliary] [productName {}] Extracting from file", dto.getProductName());
			JSONObject metadata = mdBuilder.buildConfigFileMetadata(configFileDescriptor, metadataFile);

			// Publish metadata
			step++;
			LOGGER.info("[MONITOR] [step 4] [auxiliary] [productName {}] Publishing metadata", dto.getProductName());
			if (!esServices.isMetadataExist(metadata)) {
				esServices.createMetadata(metadata);
			}

		} catch (AbstractCodedException e1) {
			LOGGER.error("[MONITOR] [step {}] [auxiliary] [productName {}] [code {}] {}", step, dto.getProductName(),
					e1.getCode(), e1.getLogMessage());
		} catch (Exception e) {
			LOGGER.error("[MONITOR] [step {}] [auxiliary] [productName {}] [code {}] [msg {}]", step,
					dto.getProductName(), ErrorCode.INTERNAL_ERROR, e.getMessage());
		} finally {
			// Remove file
			if (metadataFile != null) {
				LOGGER.info("[MONITOR] [step 5] [auxiliary] [productName {}] Removing downloaded file",
						dto.getProductName());
				File parent = metadataFile.getParentFile();
				metadataFile.delete();
				// Remove upper directory if needed
				if (!this.localDirectory.endsWith(parent.getName() + "/")) {
					parent.delete();
				}
			}
		}
		LOGGER.info("[MONITOR] [step 0] [auxiliary] [productName {}] End", dto.getProductName());
	}

}
