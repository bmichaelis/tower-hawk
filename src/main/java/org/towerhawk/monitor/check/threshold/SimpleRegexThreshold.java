package org.towerhawk.monitor.check.threshold;

import lombok.Getter;
import lombok.Setter;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.serde.resolver.ThresholdType;

@Getter
@Setter
@ThresholdType("simpleRegex")
public class SimpleRegexThreshold implements Threshold {

	private String regex;
	private boolean noMatchIsCritical = true;
	private boolean addContext;
	private boolean setMessage;

	@Override
	public void evaluate(CheckRun.Builder builder, Object value) throws Exception {
		boolean matches = value.toString().matches(regex);
		if (matches) {
			builder.succeeded();
			if (setMessage) {
				builder.message(value + " matches " + regex);
			}
		} else {
			if (noMatchIsCritical) {
				builder.critical();
			} else {
				builder.warning();
			}
			if (setMessage) {
				builder.message(value + " does not match " + regex);
			}
		}
		if (addContext) {
			builder.addContext("regex", regex)
					.addContext("value", value)
					.addContext("matches", matches);
		}
	}
}
