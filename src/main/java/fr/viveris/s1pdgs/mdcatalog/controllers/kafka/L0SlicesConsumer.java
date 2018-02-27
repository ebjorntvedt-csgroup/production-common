package fr.viveris.s1pdgs.mdcatalog.controllers.kafka;

import java.io.File;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;

import fr.viveris.s1pdgs.mdcatalog.model.L0OutputFileDescriptor;
import fr.viveris.s1pdgs.mdcatalog.model.dto.KafkaL0SliceDto;
import fr.viveris.s1pdgs.mdcatalog.model.exception.FilePathException;
import fr.viveris.s1pdgs.mdcatalog.model.exception.IgnoredFileException;
import fr.viveris.s1pdgs.mdcatalog.model.exception.MetadataExtractionException;
import fr.viveris.s1pdgs.mdcatalog.model.exception.ObjectStorageException;
import fr.viveris.s1pdgs.mdcatalog.services.es.EsServices;
import fr.viveris.s1pdgs.mdcatalog.services.files.FileDescriptorBuilder;
import fr.viveris.s1pdgs.mdcatalog.services.files.MetadataBuilder;
import fr.viveris.s1pdgs.mdcatalog.services.s3.L0SlicesS3Services;

public class L0SlicesConsumer {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(L0SlicesConsumer.class);

	/**
	 * Pattern for L0 output files
	 */
	private static final String PATTERN_L0_OUTPUT = "";

	/**
	 * Amazon S3 service for configuration files
	 */
	private final L0SlicesS3Services l0SlicesS3Services;

	/**
	 * Elasticsearch services
	 */
	private final EsServices esServices;

	/**
	 * Builder of file descriptors
	 */
	private final FileDescriptorBuilder fileDescriptorBuilder;

	/**
	 * Metadata builder
	 */
	private final MetadataBuilder mdBuilder;

	/**
	 * Local directory to upload files
	 */
	private final String localDirectory;

	@Autowired
	public L0SlicesConsumer(final EsServices esServices, final L0SlicesS3Services l0SlicesS3Services,
			@Value("${file.l0-slices.local-directory}") final String localDirectory) {
		this.localDirectory = localDirectory;
		this.fileDescriptorBuilder = new FileDescriptorBuilder(this.localDirectory,
				Pattern.compile(PATTERN_L0_OUTPUT, Pattern.CASE_INSENSITIVE));
		this.mdBuilder = new MetadataBuilder();
		this.esServices = esServices;
		this.l0SlicesS3Services = l0SlicesS3Services;
	}

	@KafkaListener(topics = "${kafka.topic.l0-slices}", groupId = "${kafka.group-id}", containerFactory = "l0SlicesKafkaListenerContainerFactory")
	public void receive(KafkaL0SliceDto dto) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[receive] Consume message {}", dto);
		}
		File metadataFile = null;
		try {
			// Upload file
			if (l0SlicesS3Services.exist(dto.getKeyObjectStorage())) {
				// Upload file
				metadataFile = this.l0SlicesS3Services.getFile(dto.getKeyObjectStorage(),
						this.localDirectory + dto.getKeyObjectStorage());

				// Extract metadata from name
				L0OutputFileDescriptor descriptor = this.fileDescriptorBuilder
						.buildL0OutputFileDescriptor(metadataFile);

				// Build metadata from file and extracted
				JSONObject metadata = mdBuilder.buildL0OutputFileMetadata(descriptor, metadataFile);

				// Publish metadata
				if (!esServices.isMetadataExist(metadata)) {
					esServices.createMetadata(metadata);
				}
			} else {
				throw new FilePathException(dto.getProductName(), dto.getKeyObjectStorage(),
						"No such L0 Slices in object storage");
			}

		} catch (ObjectStorageException | FilePathException | MetadataExtractionException | IgnoredFileException e1) {
			LOGGER.error("[productName {}] {}", dto.getProductName(), e1.getMessage());
		} catch (Exception e) {
			LOGGER.error("[productName {}] Exception occurred: {}", dto.getProductName(), e.getMessage());
		} finally {
			// Remove file
			if (metadataFile != null) {
				File parent = metadataFile.getParentFile();
				metadataFile.delete();
				// Remove upper directory if needed
				if (!this.localDirectory.endsWith(parent.getName() + "/")) {
					parent.delete();
				}
			}
		}
	}

}
