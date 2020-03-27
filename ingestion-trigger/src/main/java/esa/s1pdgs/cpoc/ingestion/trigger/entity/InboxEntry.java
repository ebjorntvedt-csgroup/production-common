package esa.s1pdgs.cpoc.ingestion.trigger.entity;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class InboxEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	private String name;
	private String relativePath;
	private String pickupPath;
	private Date lastModified;
	private long size;

	public InboxEntry() {
	}

	public InboxEntry(final String name, final String relativePath, final String pickupPath, final Date lastModified,
			final long size) {
		this.name = name;
		this.relativePath = relativePath;
		this.pickupPath = pickupPath;
		this.lastModified = lastModified;
		this.setSize(size);
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(final String relativePath) {
		this.relativePath = relativePath;
	}

	public String getPickupPath() {
		return pickupPath;
	}

	public void setPickupPath(final String pickupPath) {
		this.pickupPath = pickupPath;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
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
		// WARNING: Don't take 'id' into account when implementing equals/hashCode
		// because it's always 0 when created from Inbox
		final InboxEntry other = (InboxEntry) obj;
		return Objects.equals(name, other.name) && Objects.equals(pickupPath, other.pickupPath)
				&& Objects.equals(relativePath, other.relativePath) && Objects.equals(lastModified, other.lastModified)
				&& size == other.size;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pickupPath, relativePath, lastModified, size);
	}

	@Override
	public String toString() {
		return String.format("InboxEntry [name=%s, relativePath=%s, pickupPath=%s, lastModified=%t, size=%d]", name,
				relativePath, pickupPath, lastModified, size);
	}
}
