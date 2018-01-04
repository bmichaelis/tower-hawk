package org.towerhawk.monitor.app;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.filter.CheckFilter;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.CheckRunAggregator;
import org.towerhawk.monitor.check.run.CheckRunner;
import org.towerhawk.monitor.check.run.DefaultCheckRunAggregator;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.spring.config.Configuration;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE) //remove the need to specify a type
public class App extends DefaultCheck implements CheckExecutor {

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	protected CheckRunAggregator aggregator = new DefaultCheckRunAggregator();
	protected Map<String, Check> checks = new LinkedHashMap<>();
	protected Long defaultCacheMs;
	protected Long defaultTimeoutMs;
	protected Byte defaultPriority;
	protected Duration defaultAllowedFailureDuration;
	protected CheckRunner checkRunner;
	protected String delimiter;

	public App() {
		this(null);
	}

	public App(Map<String, Check> checks) {
		if (checks != null && !checks.isEmpty()) {
			this.checks.putAll(checks);
		}
		setType("app");
	}

	public String predicateKey() {
		return "predicate";
	}

	public Check getCheck(String checkId) {
		return getChecks().get(checkId);
	}

	public Collection<String> getCheckNames() {
		return getChecks().keySet();
	}

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext runContext) throws Exception {
		Object mapPredicate = runContext.getContext().get(predicateKey());
		Collection<Check> checksToRun;
		if (mapPredicate instanceof CheckFilter) {
			checksToRun = getChecks().values().stream().filter(((CheckFilter) mapPredicate)::filter).collect(Collectors.toList());
			runContext.getContext().remove(predicateKey());
			if (checksToRun.size() != getChecks().size()) {
				runContext.setSaveCheckRun(false);
			}
		} else {
			checksToRun = getChecks().values();
		}
		RunContext context = runContext.duplicate().setSaveCheckRun(true);
		List<CheckRun> checkRuns = checkRunner.runChecks(checksToRun, context);
		aggregateChecks(builder, checkRuns);
		return null;
	}

	protected void aggregateChecks(CheckRun.Builder builder, List<CheckRun> checkRuns) {
		aggregator.aggregate(builder, checkRuns, "OK", delimiter);
		checkRuns.forEach(checkRun -> builder.addContext(checkRun.getCheck().getId(), checkRun));
	}

	@Override
	public void init(Check check, Configuration configuration, App app, String id) throws Exception {
		setExecutor(this);
		super.init(check, configuration, app, id);
		if (checks == null) {
			throw new IllegalStateException("App " + id + " must have at least one check");
		}
		if (defaultCacheMs == null) {
			defaultCacheMs = configuration.getDefaultCacheMs();
		}
		if (defaultTimeoutMs == null) {
			defaultTimeoutMs = configuration.getDefaultTimeoutMs();
		}
		if (defaultPriority == null) {
			defaultPriority = configuration.getDefaultPriority();
		}
		if (defaultAllowedFailureDuration == null) {
			defaultAllowedFailureDuration = Duration.ofMillis(configuration.getDefaultAllowedFailureDurationMs());
		}
		delimiter = configuration.getLineDelimiter();
		App previousApp = (App) check;
		getChecks().forEach((checkId, c) -> {
			try {
				c.init(previousApp == null ? null : previousApp.getCheck(checkId), configuration, this, checkId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		checks = Collections.unmodifiableMap(getChecks());
		//an App should never be cached so override any cache settings
		this.setCacheMs(0L);
	}

	@Override
	public void close() throws Exception {
		super.close();
		getChecks().values().forEach(c -> {
			try {
				c.close();
			} catch (Exception e) {
				log.error("Check {} failed to close with exception", c.getFullName(), e);
			}
		});
	}
}
