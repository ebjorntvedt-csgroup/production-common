package esa.s1pdgs.cpoc.cronbased.trigger.db;

import java.util.Date;
import java.util.Objects;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import esa.s1pdgs.cpoc.common.ProductFamily;

@Document(collection = "catalogEventTimerEntry")
@CompoundIndexes({
		@CompoundIndex(name = "productType_productFamily_pod", def = "{'productType' : 1, 'productFamily' : 1, 'pod': 1}") })
public class CronbasedTriggerEntry {

	@Id
	private ObjectId id;

	private String productType;

	private ProductFamily productFamily;

	private Date lastCheckDate;

	private String pod;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public ProductFamily getProductFamily() {
		return productFamily;
	}

	public void setProductFamily(ProductFamily productFamily) {
		this.productFamily = productFamily;
	}

	public Date getLastCheckDate() {
		return lastCheckDate;
	}

	public void setLastCheckDate(Date lastCheckDate) {
		this.lastCheckDate = lastCheckDate;
	}

	public String getPod() {
		return pod;
	}

	public void setPod(String pod) {
		this.pod = pod;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, lastCheckDate, productFamily, productType, pod);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CronbasedTriggerEntry)) {
			return false;
		}
		CronbasedTriggerEntry other = (CronbasedTriggerEntry) obj;
		return Objects.equals(id, other.id) && Objects.equals(lastCheckDate, other.lastCheckDate)
				&& productFamily == other.productFamily && Objects.equals(productType, other.productType)
				&& Objects.equals(pod, other.pod);
	}

	@Override
	public String toString() {
		return "CatalogEventTimerEntry [id=" + id + ", productType=" + productType + ", productFamily=" + productFamily
				+ ", lastCheckDate=" + lastCheckDate + ", pod=" + pod + "]";
	}
}
