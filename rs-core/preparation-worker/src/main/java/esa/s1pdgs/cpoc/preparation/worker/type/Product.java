package esa.s1pdgs.cpoc.preparation.worker.type;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobPreselectedInput;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobTaskInputs;

public interface Product {
	public static Product nullProduct(final AppDataJob job) {
		return new Product() {
			@Override
			public AppDataJobProduct toProduct() {
				return job.getProduct();
			}
		};
	}

	Logger LOGGER = LogManager.getLogger(Product.class);

	AppDataJobProduct toProduct();

	default List<AppDataJobTaskInputs> overridingInputs() {
		return Collections.emptyList();
	}
	
	default List<AppDataJobPreselectedInput> preselectedInputs() {
		return Collections.emptyList();
	}
}