package org.towerhawk.monitor.check.threshold;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.scripting.NashornScriptEvaluator;
import org.towerhawk.serde.resolver.ThresholdType;

@Getter
@Setter
@Slf4j
@ThresholdType("js")
public class JavaScriptThreshold implements Threshold {

	private boolean addContext;
	private boolean setMessage;

	private NashornScriptEvaluator evaluator;

	@JsonCreator
	public JavaScriptThreshold(
			@JsonProperty("name") String name,
			@JsonProperty("function") String function,
			@JsonProperty("script") String script,
			@JsonProperty("file") String file,
			@JsonProperty("addContext") Boolean addContext,
			@JsonProperty("setMessage") Boolean setMessage
	) {
		this.addContext = addContext;
		this.setMessage = setMessage;
		if (name == null || name.isEmpty()) {
			name = "jsThreshold";
		}
		if (function == null || function.isEmpty()) {
			function = "evaluate";
		}
		evaluator = new NashornScriptEvaluator(name, function, script, file);
	}

	@Override
	public void evaluate(CheckRun.Builder builder, Object value) throws Exception {
		try {
			evaluator.getEngine().invokeFunction(evaluator.getFunction(), builder, value, addContext, setMessage);
		} catch (Exception e) {
			log.warn("Caught exception while executing evaluation for " + evaluator.getName(), e);
			builder.critical().error(e);
		}
	}
}
