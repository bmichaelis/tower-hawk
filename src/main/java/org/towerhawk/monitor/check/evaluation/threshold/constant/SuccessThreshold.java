package org.towerhawk.monitor.check.evaluation.threshold.constant;

import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.evaluation.threshold.Threshold;
import org.towerhawk.serde.resolver.ThresholdType;

@ThresholdType("success")
public class SuccessThreshold implements Threshold {

	@Override
	public void evaluate(CheckRun.Builder builder, Object value, boolean addContext, boolean setMessage) throws Exception {
		builder.succeeded();
		if (addContext) {
			builder.addContext("reason","always succeeds");
		}
		if (setMessage) {
			builder.message("Always succeeds");
		}
	}
}
