package org.towerhawk.monitor.check.transform;

import org.towerhawk.serde.resolver.TransformType;

@TransformType("string")
public class StringTransform implements Transform<String> {

	@Override
	public String transform(Object value) throws Exception {
		return value.toString();
	}
}
