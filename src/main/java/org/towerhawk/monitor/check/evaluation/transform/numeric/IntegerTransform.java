package org.towerhawk.monitor.check.evaluation.transform.numeric;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.towerhawk.monitor.check.evaluation.transform.Transform;
import org.towerhawk.serde.resolver.TransformType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@TransformType("integer")
public class IntegerTransform implements Transform<Integer> {

	@Override
	public Integer transform(Object value) throws Exception {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			return (Integer.valueOf(value.toString()));
		}
	}
}
