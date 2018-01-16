package org.towerhawk.monitor.check.evaluation.transform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class StringTransform implements Transform<String> {

	@Override
	public String transform(Object value) throws Exception {
		return value.toString();
	}
}
