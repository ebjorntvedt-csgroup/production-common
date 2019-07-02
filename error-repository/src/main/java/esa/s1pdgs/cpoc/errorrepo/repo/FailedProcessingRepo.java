package esa.s1pdgs.cpoc.errorrepo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.appcatalog.common.FailedProcessing;

@Service
public interface FailedProcessingRepo extends MongoRepository<FailedProcessing, Long>{

	public FailedProcessing findByIdentifier(long identifier);
	
	public void deleteByIdentifier(long identifier);
}
