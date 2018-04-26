package fr.viveris.s1pdgs.archives.services;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.util.CollectionUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import fr.viveris.s1pdgs.archives.model.exception.ObjectStorageException;

public abstract class AbstractS3Services implements S3Services {

	protected static final Logger LOGGER = LogManager.getLogger(AbstractS3Services.class);

	protected final AmazonS3 s3client;

	protected final String bucketName;

	public AbstractS3Services(final AmazonS3 s3client, final String bucketName) {
		this.s3client = s3client;
		this.bucketName = bucketName;
	}

	@Override
	public int downloadFiles(String prefixKey, String directoryPath) throws ObjectStorageException {
		int nbObj = 0;
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Downloading objects with prefix {} from bucket {} in {}", prefixKey, bucketName,
						directoryPath);
			}

			ObjectListing objectListing = s3client.listObjects(bucketName, prefixKey);
			if (objectListing != null && !CollectionUtils.isEmpty(objectListing.getObjectSummaries())) {
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					String key = objectSummary.getKey();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Downloading object {} from bucket {} in {}", key, bucketName, directoryPath);
					}
					File f = new File(directoryPath + "/" + key);
					if (f.getParentFile() != null) {
						f.getParentFile().mkdirs();
					}
					f.createNewFile();
					s3client.getObject(new GetObjectRequest(bucketName, key), f);
					nbObj++;
				}
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Download {} objects with prefix {} from bucket {} in {} succeeded", nbObj, prefixKey,
						bucketName, directoryPath);
			}
		} catch (AmazonServiceException ase) {
			LOGGER.error(ase.getMessage(), ase);
			throw new ObjectStorageException(prefixKey, prefixKey, bucketName, ase);
		} catch (AmazonClientException sce) {
			LOGGER.error(sce.getMessage(), sce);
			throw new ObjectStorageException(prefixKey, prefixKey, bucketName, sce);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ObjectStorageException(prefixKey, prefixKey, bucketName, e);
		}
		return nbObj;
	}

	@Override
	public int uploadDirectory(String prefix, File directory) throws ObjectStorageException {
		int r = 0;
		if (directory.isDirectory()) {
			File[] childs = directory.listFiles();
			if (childs != null) {
				for (File c : childs) {
					if (c.isDirectory()) {
						r += this.uploadDirectory(prefix + "/" + c.getName(), c);
					} else {
						this.uploadFile(prefix + "/" + c.getName(), c);
						r += 1;
					}
				}
			}
		} else {
			this.uploadFile(prefix, directory);
			r = 1;
		}
		return r;
	}

	@Override
	public void uploadFile(String keyName, File uploadFile) throws ObjectStorageException {
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Uploading object {} in bucket {}", keyName, bucketName);
			}

			s3client.putObject(new PutObjectRequest(bucketName, keyName, uploadFile));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Upload object {} in bucket {} succeeded", keyName, bucketName);
			}
		} catch (SdkClientException sce) {
			throw new ObjectStorageException(keyName, keyName, bucketName, sce);
		}
	}

	@Override
	public boolean exist(String keyName) throws ObjectStorageException {
		try {
			return s3client.doesObjectExist(bucketName, keyName);
		} catch (SdkClientException sce) {
			throw new ObjectStorageException(keyName, keyName, bucketName, sce);
		}
	}

	@Override
	public int getNbObjects(String prefix) throws ObjectStorageException {
		int nbObj = 0;
		try {
			ObjectListing objectListing = s3client.listObjects(bucketName, prefix);
			if (objectListing != null && !CollectionUtils.isEmpty(objectListing.getObjectSummaries())) {
				nbObj = objectListing.getObjectSummaries().size();
			}
		} catch (SdkClientException sce) {
			throw new ObjectStorageException(prefix, prefix, bucketName, sce);
		}
		return nbObj;
	}

	@Override
	public String getBucketName() {
		return this.bucketName;
	}

}
