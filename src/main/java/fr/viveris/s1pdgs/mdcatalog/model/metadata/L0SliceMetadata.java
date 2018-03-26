package fr.viveris.s1pdgs.mdcatalog.model.metadata;

public class L0SliceMetadata extends AbstractMetadata {
	
	private int instrumentConfigurationId;
	
	private int numberSlice;
	
	private String datatakeId;

	public L0SliceMetadata() {
		super();
	}

	/**
	 * @return the instrumentConfigurationId
	 */
	public int getInstrumentConfigurationId() {
		return instrumentConfigurationId;
	}

	/**
	 * @param instrumentConfigurationId the instrumentConfigurationId to set
	 */
	public void setInstrumentConfigurationId(int instrumentConfigurationId) {
		this.instrumentConfigurationId = instrumentConfigurationId;
	}

	/**
	 * @return the numberSlice
	 */
	public int getNumberSlice() {
		return numberSlice;
	}

	/**
	 * @param numberSlice the numberSlice to set
	 */
	public void setNumberSlice(int numberSlice) {
		this.numberSlice = numberSlice;
	}

	/**
	 * @return the datatakeId
	 */
	public String getDatatakeId() {
		return datatakeId;
	}

	/**
	 * @param datatakeId the datatakeId to set
	 */
	public void setDatatakeId(String datatakeId) {
		this.datatakeId = datatakeId;
	}

}
