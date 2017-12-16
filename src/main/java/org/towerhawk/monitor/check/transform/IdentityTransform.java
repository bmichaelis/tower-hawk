package org.towerhawk.monitor.check.transform;

import org.towerhawk.serde.resolver.TransformType;

@TransformType("identity")
public class IdentityTransform implements Transform {
	@Override
	public Object transform(Object value) throws Exception {
		return value;
	}
}
