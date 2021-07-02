package esa.s1pdgs.cpoc.prip.model.filter;

/**
 * Double filter for querying the persistence repository.
 */
public class PripDoubleFilter extends PripRangeValueFilter<Double> {

	public PripDoubleFilter(String fieldName) {
		super(fieldName);
	}

	public PripDoubleFilter(String fieldName, RelationalOperator operator, Double value) {
		super(fieldName, operator, value);
	}

	private PripDoubleFilter(String fieldName, RelationalOperator operator, Double value, boolean nested, String path) {
		super(fieldName, operator, value, nested, path);
	}

	// --------------------------------------------------------------------------

	@Override
	public PripDoubleFilter copy() {
		return new PripDoubleFilter(this.getFieldName(), this.getRelationalOperator(), this.getValue(), this.isNested(), this.getPath());
	}

}
