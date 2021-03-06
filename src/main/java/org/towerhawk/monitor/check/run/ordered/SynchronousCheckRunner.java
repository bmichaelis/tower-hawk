package org.towerhawk.monitor.check.run.ordered;

import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.CheckRunner;
import org.towerhawk.monitor.check.run.context.RunContext;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SynchronousCheckRunner implements CheckRunner {

	@Override
	public List<CheckRun> runChecks(Collection<Check> checks, RunContext runContext) {
		return checks.stream().map(c -> c.run(runContext)).collect(Collectors.toList());
	}
}
