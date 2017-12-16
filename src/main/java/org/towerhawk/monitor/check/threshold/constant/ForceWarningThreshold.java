package org.towerhawk.monitor.check.threshold.constant;

import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.threshold.Threshold;
import org.towerhawk.serde.resolver.ThresholdType;

@ThresholdType("warning")
public class ForceWarningThreshold implements Threshold {
	@Override
	public boolean isAddContext() {
		return false;
	}

	@Override
	public boolean isSetMessage() {
		return false;
	}

	@Override
	public void evaluate(CheckRun.Builder builder, Object value) throws Exception {
		builder.forceWarning();
	}
}