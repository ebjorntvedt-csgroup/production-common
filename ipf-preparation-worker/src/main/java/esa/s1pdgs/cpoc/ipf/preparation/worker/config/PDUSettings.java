package esa.s1pdgs.cpoc.ipf.preparation.worker.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.ipf.preparation.worker.model.pdu.PDUReferencePoint;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.pdu.PDUType;

/**
 * Additional settings used to configure the PDU type adapter
 * 
 * @author Julian Kaping
 *
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "pdu")
public class PDUSettings {

	public static class PDUTypeSettings {
		
		/**
		 * Length of frames or stripes
		 */
		private double lengthInS;
		
		/**
		 * Offset for stripes of reference ORBIT
		 */
		private double offsetInS = 0.0;
		
		/**
		 * Reference point for stripes (dump start [DUMP] or anx time [ORBIT])
		 */
		private PDUReferencePoint reference = PDUReferencePoint.ORBIT;
		
		/**
		 * Type of PDUs that should be generated
		 */
		private PDUType type = PDUType.FRAME;
		
		public double getLengthInS() {
			return lengthInS;
		}

		public void setLengthInS(double lengthInS) {
			if (lengthInS <= 0.0) {
				throw new IllegalArgumentException("lengthInS has to be greater than 0");
			}
			this.lengthInS = lengthInS;
		}
		
		public double getOffsetInS() {
			return offsetInS;
		}
		
		public void setOffsetInS(double offsetInS) {
			this.offsetInS = offsetInS;
		}
		
		public PDUReferencePoint getReference() {
			return reference;
		}

		public void setReference(PDUReferencePoint reference) {
			this.reference = reference;
		}

		public PDUType getType() {
			return type;
		}

		public void setType(PDUType type) {
			this.type = type;
		}
	}
	
	private Map<String, PDUTypeSettings> config;

	public Map<String, PDUTypeSettings> getConfig() {
		return config;
	}

	public void setConfig(Map<String, PDUTypeSettings> config) {
		this.config = config;
	}
}
