package org.towerhawk.monitor.check.evaluation.transform.numeric;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.towerhawk.monitor.check.evaluation.transform.Transform;
import org.towerhawk.serde.resolver.TransformType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@TransformType("long")
public class LongTransform implements Transform<Long> {

	@Override
	public Long transform(Object value) throws Exception {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		} else {
			return (Long.valueOf(value.toString()));
		}
	}
}
