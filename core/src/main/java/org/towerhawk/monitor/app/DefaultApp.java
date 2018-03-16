package org.towerhawk.monitor.app;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.active.Active;
import org.towerhawk.monitor.active.Enabled;
import org.towerhawk.monitor.descriptors.Activatable;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.CheckRunner;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.monitor.check.run.ordered.SynchronousCheckRunner;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.time.Duration;
import java.util.*;

@Slf4j
@Getter
@Setter
@TowerhawkType("default")
public class DefaultApp implements App, Activatable {

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
	protected String fullName;
	protected String alias;
	protected String type;
	protected Set<String> tags;

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
	public CheckRun runCheck(String checkId, RunContext runContext) {
		List<CheckRun> checkRuns = checkRunner.runChecks(Collections.singletonList(checks.get(checkId)), runContext);
		return checkRuns.get(0);
	}

	@Override
	public void init(App previousApp, Config config, String id) throws Exception {
		this.id = id;
		this.config = config;
		if (checks == null) {
			throw new IllegalStateException("App " + getId() + " must have at least one check");
		}

		if (defaultCacheMs == null) {
			defaultCacheMs = config.getLong("defaultCacheMs", 0L);
		}
		if (defaultTimeoutMs == null) {
			defaultTimeoutMs = config.getLong("defaultTimeoutMs", 10000L);
		}
		if (defaultPriority == null) {
			defaultPriority = config.getByte("defaultPriority", (byte) 0);
		}
		if (defaultAllowedFailureDuration == null) {
			defaultAllowedFailureDuration = Duration.ofMillis(config.getLong("defaultAllowedFailureDurationMs", 0L));
		}

		Check previousCheck = null;
		if (previousApp instanceof DefaultApp) {
			previousCheck = ((DefaultApp) previousApp).getContainingCheck();
		}
		DefaultCheck containingCheck = new DefaultCheck();
		containingCheck.setActive(active);
		containingCheck.setEvaluator(new AppEvaluator());
		containingCheck.setExecutor(new AppExecutor(this, checkRunner == null ? new SynchronousCheckRunner() : checkRunner, predicateKey()));
		containingCheck.setTimeoutMs(timeoutMs == null ? 30000 : timeoutMs);
		containingCheck.setPriority(priority == null ? 0 : priority);
		containingCheck.setCacheMs(0L);
		containingCheck.setUnknownIsCritical(true);
		containingCheck.init(previousCheck, config, this, "defaultApp");
		this.containingCheck = containingCheck;

		//Must go at the end of app initialization
		getChecks().forEach((checkId, c) -> {
			try {
				c.init(previousApp == null ? null : previousApp.getCheck(checkId), config, this, checkId);
			} catch (Exception e) {
				log.error("Check {} in app {} errored during initalization", checkId, getId(), e);
			}
		});
		checks = Collections.unmodifiableMap(getChecks());
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
