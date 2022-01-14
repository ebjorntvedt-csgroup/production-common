package de.werum.coprs.nativeapi.rest.model.stac;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geojson.Crs;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(property = "type", use = Id.NAME)
@JsonSubTypes({ /* @Type(StacItem.class), */ @Type(StacItemCollection.class) })
public abstract class GeoJsonBase implements Serializable {

	private static final long serialVersionUID = -2152365274928331743L;

	public enum GeoJsonType {
		FeatureCollection,
		Feature,
		Point,
		LineString,
		MultiPoint,
		Polygon,
		MultiLineString,
		MultiPolygon,
		GeometryCollection
	}

	private Crs crs;

	private double[] bbox;

	private final Map<String, Object> foreignMembers = new HashMap<>();

	public Crs getCrs() {
		return this.crs;
	}

	public void setCrs(Crs crs) {
		this.crs = crs;
	}

	public double[] getBbox() {
		return this.bbox;
	}

	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}

	@JsonAnyGetter
	public Map<String, Object> getForeignMembers() {
		return this.foreignMembers;
	}

	@JsonAnySetter
	public void setForeignMember(final String attributeName, final Object value) {
		this.foreignMembers.put(attributeName, value);
	}

}
