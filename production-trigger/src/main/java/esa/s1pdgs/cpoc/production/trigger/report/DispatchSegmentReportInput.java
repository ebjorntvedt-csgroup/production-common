package esa.s1pdgs.cpoc.production.trigger.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import esa.s1pdgs.cpoc.report.message.input.SegmentReportingInput;

public class DispatchSegmentReportInput extends SegmentReportingInput {
	
	@JsonProperty("job_id_long")
	private long jobId;
	
	@JsonProperty("input_type_string")
	private String inputType;

	public DispatchSegmentReportInput() {		
	}
	
	public DispatchSegmentReportInput(final long jobId, final String filename, final String inputType) {
		super(filename);
		this.jobId = jobId; 
		this.inputType = inputType;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(final String inputType) {
		this.inputType = inputType;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(final long jobId) {
		this.jobId = jobId;
	}	
}
