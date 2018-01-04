package org.towerhawk.monitor.check.evaluation.transform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.towerhawk.serde.resolver.TransformType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@TransformType("string")
public class StringTransform implements Transform<String> {

	@Override
	public String transform(Object value) throws Exception {
		return value.toString();
	}
}
