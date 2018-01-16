package org.towerhawk.monitor.app;

import org.towerhawk.monitor.check.evaluation.Evaluation;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.CheckRunAggregator;

import java.util.List;

public class AppEvaluator implements Evaluation {

	protected String delimiter = ",";
	protected CheckRunAggregator aggregator;

	@Override
	public void evaluate(CheckRun.Builder builder, String key, ExecutionResult result) {

	}

	protected void aggregateChecks(CheckRun.Builder builder, List<CheckRun> checkRuns) {
		aggregator.aggregate(builder, checkRuns, "OK", delimiter);
		checkRuns.forEach(checkRun -> builder.addContext(checkRun.getCheck().getId(), checkRun));
	}
}
