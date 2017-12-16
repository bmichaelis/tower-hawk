package org.towerhawk.monitor.check.transform;

import org.towerhawk.serde.resolver.TransformType;

@TransformType("double")
public class DoubleTransform implements Transform {

	@Override
	public Object transform(Object value) throws Exception {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else {
			return (Double.valueOf(value.toString()));
		}
	}
}
