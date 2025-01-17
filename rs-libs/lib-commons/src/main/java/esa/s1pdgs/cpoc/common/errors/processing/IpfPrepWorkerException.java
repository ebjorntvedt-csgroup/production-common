package esa.s1pdgs.cpoc.common.errors.processing;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;

/**
 * Abstract class for custom exception concerning jobs
 * 
 * @author Viveris Technologies
 */
public class IpfPrepWorkerException extends AbstractCodedException {

    /**
     * 
     */
    private static final long serialVersionUID = -1059947384304413070L;

    /**
     * Task table XML filename
     */
    private final String taskTable;

    /**
     * @param taskTable
     * @param code
     * @param message
     */
    public IpfPrepWorkerException(final String taskTable, final ErrorCode code,
            final String message) {
        super(code, message);
        this.taskTable = taskTable;
    }

    /**
     * @param taskTable
     * @param code
     * @param message
     * @param e
     */
    public IpfPrepWorkerException(final String taskTable, final ErrorCode code,
            final String message, final Throwable exp) {
        super(code, message, exp);
        this.taskTable = taskTable;
    }

    /**
     * @return
     */
    public String getTaskTable() {
        return taskTable;
    }

    /**
     * 
     */
    @Override
    public String getLogMessage() {
        return String.format("[taskTable %s] [msg %s]", taskTable,
                getMessage());
    }
}
