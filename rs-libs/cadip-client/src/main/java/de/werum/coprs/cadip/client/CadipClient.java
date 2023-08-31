package de.werum.coprs.cadip.client;

import java.io.Closeable;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;

import de.werum.coprs.cadip.client.model.CadipFile;
import de.werum.coprs.cadip.client.model.CadipQualityInfo;
import de.werum.coprs.cadip.client.model.CadipSession;

public interface CadipClient extends Closeable {

	/**
	 * Retrieve sessions from CADIP interface. The session objects can be filtered
	 * by satellite, downlink orbit and publishing date and any combination of the
	 * mentioned. If multiple filtering options are provided they are to be combined
	 * by "AND".
	 * 
	 * @param satellite      Satellite id to filter sessions for
	 * @param orbits         List of downlink orbits to filter sessions for
	 * @param publishingDate Earliest publishing date to retrieve sessions for
	 * @return List of session objects retrieved by CADIP interface
	 */
	List<CadipSession> getSessions(String satellite, List<String> orbits, LocalDateTime publishingDate);

	/**
	 * Retrieve files from CADIP interface. The file objects can be filtered by
	 * session id, file name (with "contains" operation) and publishing date and any
	 * combination of the mentioned. If multiple filtering options are provided they
	 * are to be combined by "AND".
	 * 
	 * @param sessionId      Session id to filter files for
	 * @param name           String that has to be part of the name of the file
	 * @param publishingDate Earliest publishing date to retrieve sessions for
	 * @return List of file objects retrieved by CADIP interface
	 */
	List<CadipFile> getFiles(String sessionId, String name, LocalDateTime publishingDate);

	/**
	 * Get input stream for a given file. As the CADIP interface allows eviction of
	 * files it can not be guaranteed, that the file is still downloadable, even
	 * when the file was just available a second ago.
	 * 
	 * @param fileId id of the file to download
	 * @return input stream of the file
	 */
	InputStream downloadFile(UUID fileId);

	/**
	 * Retrieve the quality info objects related to a given session. The amount of
	 * quality info objects is equal to the number of channels of the session.
	 * 
	 * @param sessionId id of the session to retrieve quality info for channels for 
	 * @return List of quality info objects retrieved by CADIP interface
	 */
	List<CadipQualityInfo> getQualityInfo(UUID sessionId);
}
