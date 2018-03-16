package org.towerhawk.monitor.check.evaluation.transform;

import org.towerhawk.serde.resolver.TowerhawkType;

@TowerhawkType("string")
public class StringTransform implements Transform<String> {

	@Override
	public String transform(Object value) throws Exception {
		return value.toString();
	}
}
