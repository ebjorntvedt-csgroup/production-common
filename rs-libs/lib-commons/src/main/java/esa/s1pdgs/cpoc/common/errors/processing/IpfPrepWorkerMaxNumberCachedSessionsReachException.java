package esa.s1pdgs.cpoc.common.errors.processing;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;

/**
 * Exception occurred during job generation
 * 
 * @author Cyrielle Gailliard
 */
public class IpfPrepWorkerMaxNumberCachedSessionsReachException
        extends AbstractCodedException {

    private static final long serialVersionUID = -7488001919910076897L;

    /**
     * @param message
     */
    public IpfPrepWorkerMaxNumberCachedSessionsReachException(final String message) {
        super(ErrorCode.MAX_NUMBER_CACHED_SESSIONS_REACH, message);
    }

    /**
     * 
     */
    @Override
    public String getLogMessage() {
        return String.format("[msg %s]", getMessage());
    }

}
