package fr.viveris.s1pdgs.mdcatalog.model;

/**
 * Enumeration for ERDS session file type
 * @author Cyrielle Gailliard
 *
 */
public enum EdrsSessionFileType {
	RAW, SESSION;
	
	/**
	 * Determinate value from an extension
	 * @param extension
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static EdrsSessionFileType valueFromExtension(FileExtension extension) throws IllegalArgumentException {
		switch (extension) {
		case XML:
			return SESSION;
		case RAW:
			return RAW;
		default:
			// TODO custome exception
			throw new IllegalArgumentException("Cannot retrieve ERDS session file type from extension " + extension);
		}
	}
}
