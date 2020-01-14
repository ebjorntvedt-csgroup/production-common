package esa.s1pdgs.cpoc.appcatalog;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Object used for persisting data around job generation per task table
 * 
 * @author Viveris Technologies
 */
public class AppDataJobGeneration {

    /**
     * Date of the creation of the job generation
     */
    private Date creationDate;

    /**
     * Date of the last modification done on the job generation
     */
    private Date lastUpdateDate;

    /**
     * Name of the task table (= filename)
     */
    private String taskTable;

    /**
     * State of the job generation
     */
    private AppDataJobGenerationState state;

    /**
     * Number of consecutive errors
     */
    private int nbErrors;
    
    /**
     * UID of the reporting (root layer) task
     */
    private UUID reportingTaskUID;

    public AppDataJobGeneration() {
        super();
        this.state = AppDataJobGenerationState.INITIAL;
        this.creationDate = new Date();
        this.nbErrors = 0;
    }

    /**
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate
     *            the creationDate to set
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the lastUpdateDate
     */
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * @param lastUpdateDate
     *            the lastUpdateDate to set
     */
    public void setLastUpdateDate(final Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @return the taskTable
     */
    public String getTaskTable() {
        return taskTable;
    }

    /**
     * @param taskTable
     *            the taskTable to set
     */
    public void setTaskTable(final String taskTable) {
        this.taskTable = taskTable;
    }

    /**
     * @return the state
     */
    public AppDataJobGenerationState getState() {
        return state;
    }

    /**
     * @param state
     *            the state to set
     */
    public void setState(final AppDataJobGenerationState state) {
        this.state = state;
    }

    /**
     * @return the nbErrors
     */
    public int getNbErrors() {
        return nbErrors;
    }

    /**
     * @param nbErrors
     *            the nbErrors to set
     */
    public void setNbErrors(final int nbErrors) {
        this.nbErrors = nbErrors;
    }

	/**
	 * @return the reportingTaskUID
	 */
	public UUID getReportingTaskUID() {
		return reportingTaskUID;
	}

	/**
	 * @param reportingTaskUID the reportingTaskUID to set
	 */
	public void setReportingTaskUID(UUID reportingTaskUID) {
		this.reportingTaskUID = reportingTaskUID;
	}
	
    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "{creationDate: %s, lastUpdateDate: %s, taskTable: %s, state: %s, nbErrors: %s, reportingTaskUID: %s}",
                creationDate, lastUpdateDate, taskTable, state, nbErrors, reportingTaskUID);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(creationDate, lastUpdateDate, taskTable, state,
                nbErrors, reportingTaskUID);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        boolean ret;
        if (this == obj) {
            ret = true;
        } else if (obj == null || getClass() != obj.getClass()) {
            ret = false;
        } else {
            final AppDataJobGeneration other = (AppDataJobGeneration) obj;
            ret = Objects.equals(creationDate, other.creationDate)
                    && Objects.equals(lastUpdateDate, other.lastUpdateDate)
                    && Objects.equals(taskTable, other.taskTable)
                    && Objects.equals(state, other.state)
                    && nbErrors == other.nbErrors
                    && Objects.equals(reportingTaskUID, other.reportingTaskUID);
        }
        return ret;
    }

}
