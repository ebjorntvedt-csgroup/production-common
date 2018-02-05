package fr.viveris.s1pdgs.ingestor.services.s3;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service
public class ConfigFilesS3Services implements S3Services {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFilesS3Services.class);

	@Autowired
	private AmazonS3 s3client;

	@Value("${storage.buckets.config-files}")
	private String bucketName;

	@Override
	public void downloadFile(String keyName) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Downloading object {} from bucket {}", keyName, bucketName);
		}
		s3client.getObject(new GetObjectRequest(bucketName, keyName));
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Download object {} from bucket {} succeeded", keyName, bucketName);
		}
	}

	@Override
	public void uploadFile(String keyName, File uploadFile) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Uploading object {} in bucket {}", keyName, bucketName);
		}
		s3client.putObject(new PutObjectRequest(bucketName, keyName, uploadFile));
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Upload object {} in bucket {} succeeded", keyName, bucketName);
		}
	}
}
