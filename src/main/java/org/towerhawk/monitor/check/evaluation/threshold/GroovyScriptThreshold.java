package org.towerhawk.monitor.check.evaluation.threshold;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.scripting.GroovyScriptEvaluator;
import org.towerhawk.serde.resolver.ThresholdType;

@Getter
@Setter
@Slf4j

@ThresholdType("groovy")
public class GroovyScriptThreshold implements Threshold {

	private GroovyScriptEvaluator evaluator;

	@JsonCreator
	public GroovyScriptThreshold(
			@JsonProperty("name") String name,
			@JsonProperty("function") String function,
			@JsonProperty("script") String script,
			@JsonProperty("file") String file
	) {
		if (name == null || name.isEmpty()) {
			name = "jsThreshold";
		}
		if (function == null || function.isEmpty()) {
			function = "evaluate";
		}
		evaluator = new GroovyScriptEvaluator(name, function, script, file);
	}

	@Override
	public void evaluate(CheckRun.Builder builder, Object value, boolean addContext, boolean setMessage) throws Exception {
		evaluator.invoke(builder, value, addContext, setMessage);
	}
}
