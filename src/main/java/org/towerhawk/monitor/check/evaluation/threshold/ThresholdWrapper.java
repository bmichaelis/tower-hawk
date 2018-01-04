package org.towerhawk.monitor.check.evaluation.threshold;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.evaluation.threshold.constant.SuccessThreshold;

@Setter
@Getter
@Accessors(chain = true)
public class ThresholdWrapper{

	private boolean addContext;
	private boolean setMessage;
	@JsonUnwrapped
	private Threshold threshold;

	public ThresholdWrapper() {
		this(new SuccessThreshold());
	}

	public ThresholdWrapper(Threshold threshold) {
		this.threshold = threshold;
	}

	public void evaluate(CheckRun.Builder builder, Object value) throws Exception {
		threshold.evaluate(builder, value, addContext, setMessage);
	}
}
