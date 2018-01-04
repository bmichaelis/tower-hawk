package org.towerhawk.monitor.check.evaluation.threshold.constant;

import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.evaluation.threshold.Threshold;
import org.towerhawk.serde.resolver.ThresholdType;

@ThresholdType("error")
public class ErrorThreshold implements Threshold {

	@Override
	public void evaluate(CheckRun.Builder builder, Object value, boolean addContext, boolean setMessage) throws Exception {
		throw new Exception("Always thrown");
	}
}
