package esa.s1pdgs.cpoc.mqi.model.queue;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import esa.s1pdgs.cpoc.mqi.model.control.ControlAction;

public class CatalogEvent extends AbstractMessage {	
	private String productName;
	private String productType;
	private Map<String,Object> metadata;
	
	public CatalogEvent() {
		super();
		setAllowedControlActions(Arrays.asList(ControlAction.RESUBMIT));
	}
	
	public String getProductName() {
		return productName;
	}
	
	public void setProductName(final String productName) {
		this.productName = productName;
	}
	
	public String getProductType() {
		return productType;
	}
	
	public void setProductType(final String productType) {
		this.productType = productType;
	}
	
	public Map<String,Object> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(final Map<String,Object> metadata) {
		this.metadata = metadata;
	}

	@Override
	public int hashCode() {
		return Objects.hash(creationDate, hostname, productName, productType,
				keyObjectStorage, metadata, productFamily, uid,
				allowedControlActions, controlDemandType, controlDebug, controlRetryCounter);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CatalogEvent other = (CatalogEvent) obj;
		return Objects.equals(creationDate, other.creationDate) 
				&& Objects.equals(productName, other.productName)
				&& Objects.equals(hostname, other.hostname) 
				&& Objects.equals(productType, other.productType)
				&& Objects.equals(metadata, other.metadata)
				&& Objects.equals(keyObjectStorage, other.keyObjectStorage)
				&& Objects.equals(uid, other.uid)
				&& productFamily == other.productFamily
				&& Objects.equals(allowedControlActions, other.getAllowedControlActions())
		        && controlDemandType == other.controlDemandType
		        && controlDebug == other.controlDebug
		        && controlRetryCounter == other.controlRetryCounter;
	}

	@Override
	public String toString() {
		return "CatalogEvent [productName=" + productName + ", productType=" + productType + 
				", metadata=" + metadata + ", productFamily=" + productFamily + 
				", keyObjectStorage=" + keyObjectStorage + ", creationDate=" + creationDate + 
				", hostname=" + hostname + ", uid=" + uid + "]";
	}
}