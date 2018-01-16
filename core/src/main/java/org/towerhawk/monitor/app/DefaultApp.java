package org.towerhawk.monitor.app;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.active.Active;
import org.towerhawk.monitor.active.Enabled;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.CheckRunner;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.monitor.check.run.ordered.SynchronousCheckRunner;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Getter
@Setter
@TowerhawkType("default")
public class DefaultApp implements App {

	protected DefaultCheck containingCheck;
	protected Map<String, Check> checks = new LinkedHashMap<>();
	protected Long defaultCacheMs = 0L;
	protected Long defaultTimeoutMs;
	protected Byte defaultPriority;
	protected Duration defaultAllowedFailureDuration;
	protected CheckRunner checkRunner;
	protected Config config;
	protected String id;
	protected Active active = new Enabled();
	protected Long timeoutMs;
	protected Byte priority;

	public DefaultApp() {
		this(null);
	}

	public DefaultApp(Map<String, Check> checks) {
		if (checks != null && !checks.isEmpty()) {
			this.checks.putAll(checks);
		}
	}

	public void setDefaultAllowedFailureDurationMs(long defaultAllowedFailureDurationMs) {
		defaultAllowedFailureDuration = Duration.ofMillis(defaultAllowedFailureDurationMs);
	}

	public String predicateKey() {
		return "predicate";
	}

	public Check getCheck(String checkId) {
		return getChecks().get(checkId);
	}

	public Set<String> getCheckNames() {
		return getChecks().keySet();
	}

	@Override
	public void init(App previousApp, Config config, String id) throws Exception {
		this.id = id;
		this.config = config;
		if (checks == null) {
			throw new IllegalStateException("App " + getId() + " must have at least one check");
		}

		getChecks().forEach((checkId, c) -> {
			try {
				c.init(previousApp == null ? null : previousApp.getCheck(checkId), config, this, checkId);
			} catch (Exception e) {
				log.error("Unable to initialize checks!", e);
			}
		});
		checks = Collections.unmodifiableMap(getChecks());

		if (defaultCacheMs == null) {
			defaultCacheMs = config.getLong("defaultCacheMs");
			if (defaultCacheMs == null) {
				defaultCacheMs = 0L;
			}
		}
		if (defaultTimeoutMs == null) {
			defaultTimeoutMs = config.getLong("defaultTimeoutMs");
			if (defaultTimeoutMs == null) {
				defaultTimeoutMs = 10000L;
			}
		}
		if (defaultPriority == null) {
			defaultPriority = config.getByte("defaultPriority");
			if (defaultPriority == null) {
				defaultPriority = 0;
			}
		}
		if (defaultAllowedFailureDuration == null) {
			defaultAllowedFailureDuration = Duration.ofMillis(config.getLong("defaultAllowedFailureDurationMs"));
			if (defaultAllowedFailureDuration == null) {
				defaultAllowedFailureDuration = Duration.ZERO;
			}
		}

		Check previousCheck = null;
		if (previousApp instanceof DefaultApp) {
			previousCheck = ((DefaultApp) previousApp).getContainingCheck();
		}
		DefaultCheck containingCheck = new DefaultCheck();
		containingCheck.setActive(active);
		containingCheck.setEvaluation(new AppEvaluator());
		containingCheck.setExecutor(new AppExecutor(this, checkRunner == null ? new SynchronousCheckRunner() : checkRunner,predicateKey()));
		containingCheck.setTimeoutMs(timeoutMs == null ? 30000 : timeoutMs);
		containingCheck.setPriority(priority == null ? 0 : priority);
		containingCheck.setCacheMs(0L);
		containingCheck.setUnknownIsCritical(true);
		containingCheck.init(previousCheck, config, this, "defaultApp");
		this.containingCheck = containingCheck;
	}

	@Override
	public CheckRun run(RunContext runContext) {
		return containingCheck.run(runContext);
	}

	@Override
	public void close() throws Exception {
		for (Check check : checks.values()) {
			try {
				check.close();
			} catch (Exception e) {
				log.warn("Unable to close check {}", check.getFullName());
			}
		}
	}
}
