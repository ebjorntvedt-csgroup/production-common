package fr.viveris.s1pdgs.jobgenerator.model.tasktable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.util.StringUtils;

import fr.viveris.s1pdgs.jobgenerator.model.tasktable.enums.TaskTableInputMode;
import fr.viveris.s1pdgs.jobgenerator.model.tasktable.enums.TaskTableMandatoryEnum;

/**
 * 
 */
@XmlRootElement(name = "Input")
@XmlAccessorType(XmlAccessType.NONE)
public class TaskTableInput {

	/**
	 * 
	 */
	@XmlAttribute(name = "id")
	private String id;

	/**
	 * 
	 */
	@XmlAttribute(name = "ref")
	private String reference;

	/**
	 * 
	 */
	@XmlElement(name = "Mode")
	private TaskTableInputMode mode;

	/**
	 * 
	 */
	@XmlElement(name = "Mandatory")
	private TaskTableMandatoryEnum mandatory;

	/**
	 * 
	 */
	@XmlElementWrapper(name = "List_of_Alternatives")
	@XmlElement(name = "Alternative")
	private List<TaskTableInputAlternative> alternatives;

	/**
	 * 
	 */
	public TaskTableInput() {
		super();
		this.mode = TaskTableInputMode.BLANK;
		this.mandatory = TaskTableMandatoryEnum.NO;
		this.alternatives = new ArrayList<>();
	}

	/**
	 * 
	 */
	public TaskTableInput(final String reference) {
		super();
		this.reference = reference;
	}

	/**
	 * @param mode
	 * @param mandatory
	 */
	public TaskTableInput(final TaskTableInputMode mode, final TaskTableMandatoryEnum mandatory) {
		this();
		this.mode = mode;
		this.mandatory = mandatory;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * @param reference
	 *            the reference to set
	 */
	public void setReference(final String reference) {
		this.reference = reference;
	}

	/**
	 * @return the mode
	 */
	public TaskTableInputMode getMode() {
		return mode;
	}

	/**
	 * @param mode
	 *            the mode to set
	 */
	public void setMode(final TaskTableInputMode mode) {
		this.mode = mode;
	}

	/**
	 * @return the mandatory
	 */
	public TaskTableMandatoryEnum getMandatory() {
		return mandatory;
	}

	/**
	 * @param mandatory
	 *            the mandatory to set
	 */
	public void setMandatory(final TaskTableMandatoryEnum mandatory) {
		this.mandatory = mandatory;
	}

	/**
	 * @return the alternatives
	 */
	public List<TaskTableInputAlternative> getAlternatives() {
		return alternatives;
	}

	/**
	 * @param alternatives
	 *            the alternatives to set
	 */
	public void setAlternatives(final List<TaskTableInputAlternative> alternatives) {
		this.alternatives = alternatives;
	}

	/**
	 * @param alternatives
	 *            the alternatives to set
	 */
	public void addAlternatives(final List<TaskTableInputAlternative> alternatives) {
		this.alternatives.addAll(alternatives);
	}

	/**
	 * @param alternatives
	 *            the alternatives to set
	 */
	public void addAlternative(final TaskTableInputAlternative alternative) {
		this.alternatives.add(alternative);
	}
	
	/**
	 * 
	 * @return
	 */
	public String toLogMessage() {
		if (StringUtils.isEmpty(reference)) {
			return id;
		}
		return reference;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id, reference, mode, mandatory, alternatives);
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
			TaskTableInput other = (TaskTableInput) obj;
			ret = Objects.equals(id, other.id) && Objects.equals(reference, other.reference)
					&& Objects.equals(mode, other.mode) && Objects.equals(mandatory, other.mandatory)
					&& Objects.equals(alternatives, other.alternatives);
		}
		return ret;
	}

}
