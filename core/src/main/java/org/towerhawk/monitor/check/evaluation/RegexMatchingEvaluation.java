package org.towerhawk.monitor.check.evaluation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@TowerhawkType({"default","regexMatching"})
public class RegexMatchingEvaluation implements Evaluation {

	@JsonIgnore
	protected Map<Pattern, EvaluationDAG> evaluation = new LinkedHashMap<>();

	@JsonAnySetter
	public void setEvaluation(String pattern, EvaluationDAG evaluation) {
		this.evaluation.put(Pattern.compile(pattern), evaluation);
	}

	@Override
	public void evaluate(CheckRun.Builder builder, String key, ExecutionResult result) throws Exception {
		for (Map.Entry<Pattern, EvaluationDAG> entry : evaluation.entrySet()) {
			boolean matched = false;
			try {
				for (Map.Entry<String, Object> r : result.getResults().entrySet()) {
					if (entry.getKey().matcher(r.getKey()).matches()) {
						matched = true;
						entry.getValue().evaluate(builder, r.getKey(), result);
					}
				}
			} catch (Exception e) {
				String[] transformType = entry.getValue().getTransform().getClass().getAnnotation(TowerhawkType.class).value();
				log.error("Unable to evaluate transform of type {} for check due to exception", transformType != null ? transformType[0] : "Unresolvable", e);
				throw e;
			} finally {
				if (!matched) {
					log.warn("Threshold {} did not match any values returned by execution", entry.getKey());
				}
			}
		}
	}
}
