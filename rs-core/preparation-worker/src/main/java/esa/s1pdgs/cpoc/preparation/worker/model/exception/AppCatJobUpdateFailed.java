package esa.s1pdgs.cpoc.preparation.worker.model.exception;

@SuppressWarnings("serial")
public class AppCatJobUpdateFailed extends RuntimeException {
	public AppCatJobUpdateFailed(final String message) {
		super(message);
	}

	public AppCatJobUpdateFailed(final String message, final Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
}
