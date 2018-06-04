package fr.viveris.s1pdgs.ingestor.files.model;

import java.util.Locale;

/**
 * Enumeration for file extension
 *
 */
public enum FileExtension {
	XML, SAFE, EOF, RAW, XSD, DAT, UNKNOWN;

	/**
	 * Determinate value from an extension
	 * 
	 * @param extension
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static FileExtension valueOfIgnoreCase(final String extension) throws IllegalArgumentException {
		FileExtension ret;
		try {
			String extensionUC = extension.toUpperCase(Locale.getDefault());
			ret = valueOf(extensionUC);
		} catch (IllegalArgumentException e) {
			ret = UNKNOWN;
		}
		return ret;
	}
}
