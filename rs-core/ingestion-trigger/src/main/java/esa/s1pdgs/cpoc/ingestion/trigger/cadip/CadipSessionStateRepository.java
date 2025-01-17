package esa.s1pdgs.cpoc.ingestion.trigger.cadip;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CadipSessionStateRepository extends MongoRepository<CadipSessionState, ObjectId> {

	Optional<CadipSessionState> findByPodAndCadipUrlAndSessionIdAndRetransfer(final String pod, final String cadipUrl,
			final String sessionId, final boolean retransfer);

	List<CadipSessionState> findByPodAndCadipUrl(final String pod, final String cadipUrl);
}
