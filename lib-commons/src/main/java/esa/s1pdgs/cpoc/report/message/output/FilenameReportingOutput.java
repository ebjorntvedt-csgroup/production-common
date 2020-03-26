package esa.s1pdgs.cpoc.report.message.output;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import esa.s1pdgs.cpoc.report.ReportingOutput;

public class FilenameReportingOutput implements ReportingOutput {		
	@JsonProperty("filename_strings")
	@JsonInclude(value=Include.NON_EMPTY, content=Include.NON_NULL)
	private List<String> filenames;
	
	@JsonProperty("segment_strings")
	@JsonInclude(value=Include.NON_EMPTY, content=Include.NON_NULL)
	private List<String> segments;
	
	public FilenameReportingOutput(final List<String> filenames, final List<String> segments) {
		this.filenames = filenames;
		this.segments = segments;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(final List<String> filenames) {
		this.filenames = filenames;
	}

	public List<String> getSegments() {
		return segments;
	}

	public void setSegments(final List<String> segments) {
		this.segments = segments;
	}
}
