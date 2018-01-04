package org.towerhawk.monitor.check.evaluation.transform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.towerhawk.serde.resolver.TransformType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@TransformType("identity")
public class IdentityTransform implements Transform {
	@Override
	public Object transform(Object value) throws Exception {
		return value;
	}
}
