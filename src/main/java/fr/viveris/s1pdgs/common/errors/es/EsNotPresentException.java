package fr.viveris.s1pdgs.common.errors.es;

import fr.viveris.s1pdgs.common.errors.AbstractCodedException;

/**
 * @author Viveris Technologies
 */
public class EsNotPresentException extends AbstractCodedException {

    /**
     * UUID
     */
    private static final long serialVersionUID = -616528427720024929L;

    /**
     * Generic message
     */
    private static final String MESSAGE = "Metadata not present";

    /**
     * Constructor
     */
    public EsNotPresentException() {
        super(ErrorCode.ES_NOT_PRESENT_ERROR, MESSAGE);
    }

    /**
     * 
     */
    @Override
    public String getLogMessage() {
        return String.format("[msg %s]", getMessage());
    }

}
