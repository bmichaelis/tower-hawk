package org.towerhawk.monitor.check.evaluation;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.evaluation.threshold.Threshold;
import org.towerhawk.monitor.check.evaluation.threshold.constant.SuccessThreshold;
import org.towerhawk.monitor.check.evaluation.transform.IdentityTransform;
import org.towerhawk.monitor.check.evaluation.transform.Transform;
import org.towerhawk.monitor.check.run.CheckRun;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Setter
@Getter
public class EvaluationDAG {

	protected Transform transform = new IdentityTransform();
	protected Threshold threshold = new SuccessThreshold();
	protected boolean addContext;
	protected boolean setMessage;
	protected Map<String, EvaluationDAG> children = Collections.emptyMap();

	public void apply(CheckRun.Builder builder, String key, Object value) throws Exception {
		Object newValue = transform.transform(value);
		threshold.evaluate(builder, newValue, addContext, setMessage);
		for (EvaluationDAG t : children.values()) {
			t.apply(builder, key, newValue);
		}
	}
}

