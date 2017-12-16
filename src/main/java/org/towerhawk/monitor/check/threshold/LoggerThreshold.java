package org.towerhawk.monitor.check.threshold;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.serde.resolver.ThresholdType;

@Getter
@Setter
@Slf4j
@ThresholdType("logger")
public class LoggerThreshold implements Threshold {

	private boolean addContext;
	private boolean setMessage;

	@Override
	public void evaluate(CheckRun.Builder builder, Object value) throws Exception {
		log.info("{}: {}", builder.getCheck().getFullName(), value.toString());
		builder.succeeded();
		if (isAddContext()) {
			builder.addContext("value", value.toString());
		}
	}
}
