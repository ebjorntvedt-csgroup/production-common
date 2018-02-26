package fr.viveris.s1pdgs.mdcatalog.model.exception;

public class FileTerminatedException extends AbstractFileException {

	private static final long serialVersionUID = -1107106717498506533L;

	public FileTerminatedException(String msg, String productName) {
		super(msg, productName);
	}
	
	public FileTerminatedException(String msg, String productName, Throwable cause) {
		super(msg, productName, cause);
	}

}
