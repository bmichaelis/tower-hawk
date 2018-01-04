package org.towerhawk.monitor.check;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.active.Active;
import org.towerhawk.monitor.active.Enabled;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.check.evaluation.EvaluationDAG;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.logging.CheckMDC;
import org.towerhawk.monitor.check.recent.RecentCheckRun;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.Status;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckType;
import org.towerhawk.serde.resolver.TransformType;
import org.towerhawk.spring.config.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Getter
@CheckType("default")
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class DefaultCheck implements Check {

	/**
	 * The id for this check. This should match the dictionary key in the configuration yaml.
	 * This must be unique within an App.
	 */
	private String id = null;

	/**
	 * The app this Check is tied to.
	 */
	@JsonIgnore
	private App app = null;

	/**
	 * The type of check that this is, or more particularly the type of the
	 * execution that this check holds.
	 */
	@Setter
	private String type = "check";

	/**
	 * An alias for this check. When looking up checks by id, this method should also be
	 * consulted which allows for migration. This should be unique within an App.
	 */
	@Setter
	private String alias = null;

	/**
	 * The fullName of the check - which by default is ${App}:${Check}
	 */
	protected transient String fullName = null;

	/**
	 * The timestamp containing the last time the check finished executing.
	 * This is used for determining cache intervals
	 */
	private transient long runEndTimestamp = 0;

	/**
	 * The timestamp containing the last time the check started executing.
	 */
	private transient long runStartTimestamp = 0;

	/**
	 * This determines whether the check is active right now or not. This allows different
	 * strategies to be implemented like daily or weekly schedules. This can also be used
	 * to effectively disable a check.
	 */
	@Setter
	private Long cacheMs = null;

	/**
	 * How long to let a check run before interrupting it. If a check is running for
	 * more than getTimeoutMs() milliseconds then it should be cancelled and return a
	 * CheckRun with an Error and the isTimedOut() method returning true.
	 */
	@Setter
	private Long timeoutMs = null;

	/**
	 * Checks with higher priorities must be run first. This method can also be called to
	 * run checks with a certain priority.
	 */
	@Setter
	private Byte priority = null;

	/**
	 * The amount of time this check needs to fail before it will actually be reported.
	 *
	 * @return A duration representing how long this check needs to be failing for
	 * before it is reported. If the duration has not passed, the ${@link CheckRun} will
	 * be set to ${@link Status#SUCCEEDED}
	 */
	@Setter
	private Duration allowedFailureDuration = Duration.ZERO;

	/**
	 * If the most recent CheckRun is in a failed state, this should tell when this check
	 * entered a failed state. It should keep a consistent time until the check successfully
	 * completes.
	 */
	private transient ZonedDateTime failingSince = null;

	/**
	 * @return If this check has been set to restarting. If it is restarting, then it will
	 * not return anything other than ${@link Status#SUCCEEDED} until it has actually
	 * completed successfully
	 */
	@Setter
	private transient boolean restarting = false;

	/**
	 * Returns a set of tags that are used to be able to run a subset of checks.
	 */
	private Set<String> tags = new LinkedHashSet<>();

	/**
	 * This determines whether the check is active right now or not. This allows different
	 * strategies to be implemented like daily or weekly schedules. This can also be used
	 * to effectively disable a check.
	 *
	 * @return true if the check can run, false otherwise.
	 */
	@Setter(AccessLevel.PROTECTED)
	private Active active = new Enabled();

	/**
	 * A representation of CheckRuns so that historical runs can be inspected.
	 */
	@JsonIgnore
	private transient RecentCheckRun recentCheckRuns = new RecentCheckRun();

	/**
	 * Determines if this check is currently running.
	 * true if running, false otherwise
	 */
	private transient boolean running = false;

	/**
	 * How to deal with a ${@link CheckRun} that is UNKNOWN. By default it is treated
	 * as CRITICAL. Setting this to false will treat UNKOWN as WARNING.
	 */
	@Setter
	private boolean unknownIsCritical = true;

	/**
	 * A reference to the main Configuration so that Executors, Thresholds, and Transforms
	 * can dynamically configure themselves if necessary.
	 */
	@Setter
	@JsonIgnore
	private transient Configuration configuration;

	/**
	 * Keeps track of whether this check has been initialized to keep it from being initialized
	 * twice since initialization can be expensive.
	 */
	private transient boolean initialized = false;

	/**
	 * A map of transforms to be executed. Since each transform can have a threshold, it allows
	 * for the results of a single execution to have multiple checks run on it.
	 */
	private Map<Pattern, EvaluationDAG> evaluation = Collections.emptyMap();

	/**
	 * The ${@link CheckExecutor} used to run the checks.
	 */
	@Setter
	private CheckExecutor executor;

	@Override
	public final boolean isActive() {
		return initialized && active.isActive();
	}

	@Override
	public byte getPriority() {
		return priority;
	}

	@Override
	public long getTimeoutMs() {
		return timeoutMs;
	}

	@Override
	public long getCacheMs() {
		return cacheMs;
	}

	@Override
	@JsonIgnore
	public CheckRun getLastCheckRun() {
		return recentCheckRuns.getLastRun();
	}

	@Override
	public List<CheckRun> getRecentCheckRuns() {
		return recentCheckRuns.getRecentCheckRuns();
	}

	@Override
	public final boolean isCached() {
		return cachedFor() > 0;
	}

	@JsonProperty("evaluation")
	public void setEvaluation(Map<String, EvaluationDAG> transforms) {
		this.evaluation = Collections.unmodifiableMap(
				transforms.entrySet().stream().collect(
						Collectors.toMap(
								e -> Pattern.compile(e.getKey()),
								Map.Entry::getValue)
				)
		);
	}

	protected final long cachedFor() {
		return getCacheMs() - (System.currentTimeMillis() - runEndTimestamp);
	}

	public long getMsRemaining(boolean throwException) {
		long timeRemaining = getTimeoutMs() - (System.currentTimeMillis() - runStartTimestamp);
		if (timeRemaining < 0 && throwException) {
			throw new IllegalStateException("Check is timed out");
		}
		return timeRemaining;
	}

	protected final ZonedDateTime setFailingSince(long epochMillis) {
		return setFailingSince(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()));
	}

	protected final ZonedDateTime setFailingSince() {
		return setFailingSince(ZonedDateTime.now());
	}

	protected final ZonedDateTime setFailingSince(ZonedDateTime failingSince) {
		if (this.failingSince == null) {
			this.failingSince = failingSince;
		}
		return failingSince;
	}

	protected final void clearFailingSince() {
		failingSince = null;
	}

	protected final void setRecentCheckRunSize(int size) {
		recentCheckRuns.setSizeLimit(size);
	}

	private void maybeSuppressFailure(CheckRun.Builder builder, ZonedDateTime failingSince) {
		if (isRestarting()) {
			builder.forceSucceeded().addContext("restarting", true);
			return;
		}
		ZonedDateTime failingTime = failingSince.plus(getAllowedFailureDuration());
		if (failingTime.compareTo(ZonedDateTime.now()) > 0) {
			builder.forceSucceeded().addContext("suppressedFailureUntil", failingTime);
		}
	}

	/**
	 * Complex Transform matching has been implemented to allow flexibility and to allow
	 * "caching" of return results. For instance, if a relatively expensive rest call
	 * returns JSON, it can be checked for several different values without having to run
	 * the call each time since one of the purposes of Towerhawk is to be a lightweight as
	 * possible on the systems that it monitors.
	 * <p>
	 * Transforms are applied to the given value with the following rules:
	 * - If value is not a ExecutionResult it will be applied to transforms as if it were
	 * the "result" key of a ExecutionResult
	 * - If value is a ExecutionResult then they keys of the ExecutionResult are matched by regular
	 * expression which means the keys to the transform map are interpreted as regular
	 * expressions.
	 * - All children of top-level transforms are handed the results of preceding transforms
	 * as defined in ${@link EvaluationDAG#apply(CheckRun.Builder, String, Object)}
	 * - If a transform isn't matched by a returning value, a warning will be logged
	 *
	 * @param builder
	 * @param value
	 */
	protected void applyTransforms(CheckRun.Builder builder, ExecutionResult value) throws Exception {

		for (Map.Entry<Pattern, EvaluationDAG> entry : evaluation.entrySet()) {
			boolean matched = false;
			try {
				for (Map.Entry<String, Object> result : value.getResults().entrySet()) {
					if (entry.getKey().matcher(result.getKey()).matches()) {
						matched = true;
						entry.getValue().apply(builder, result.getKey(), result.getValue());
					}
				}
			} catch (Exception e) {
				String transformType = entry.getValue().getTransform().getClass().getAnnotation(TransformType.class).value();
				log.error("Unable to apply transform of type {} for check due to exception", transformType, e);
				throw e;
			} finally {
				if (!matched) {
					log.warn("Threshold {} did not match any values returned by execution", entry.getKey());
				}
			}
		}
	}

	/**
	 * This is where the real work happens. A ${@link CheckRun} is returned containing information
	 * about how the check went. This can be a synchronized method to ensure that multiple
	 * runs don't happen concurrently. Logic should be in place in this method to see if
	 * a check can run (see canRun()) and if this method gets called concurrently
	 * the second invocation can return the results of the first invocation.
	 *
	 * @param runContext
	 * @return The CheckRun representing the results of this run().
	 */
	@Override
	@Synchronized
	public final CheckRun run(RunContext runContext) {
		CheckMDC.put(this);
		CheckRun checkRun;
		if (!runContext.shouldRun() || !canRun()) {
			if (running) {
				log.debug("Check is already running");
			} else if (!initialized) {
				log.warn("Trying to run check but it is not initialized");
			} else if (!isActive()) {
				log.debug("Check is not active");
				CheckRun lastRun = getLastCheckRun();
				if (lastRun.getStatus() != Status.SUCCEEDED) {
					CheckRun.Builder copyRunBuilder = CheckRun.builder(lastRun);
					copyRunBuilder.forceSucceeded();
					copyRunBuilder.addContext("inactive", "Check is not active and was failing");
					recentCheckRuns.addCheckRun(copyRunBuilder.build());
				}
			} else if (isCached()) {
				log.debug("Check is cached for {} more ms", cachedFor());
			}
			return getLastCheckRun();
		}
		log.debug("Starting run()");
		running = true;
		CheckRun.Builder builder = CheckRun.builder(this).unknownIsCritical(isUnknownIsCritical());
		runStartTimestamp = builder.startTime();
		try {
			ExecutionResult result = executor.execute(builder, runContext);
			builder.result(result);
			applyTransforms(builder, result);
		} catch (InterruptedException e) {
			builder.timedOut(true).unknown().error(e);
			log.warn("Check got interrupted");
		} catch (Exception e) {
			builder.error(e).critical();
			log.error("execute() or applyTransforms() for check threw an exception", e);
		} finally {
			runEndTimestamp = builder.endTime();
			if (builder.getStatus() == Status.SUCCEEDED) {
				clearFailingSince();
				setRestarting(false);
			} else {
				maybeSuppressFailure(builder, setFailingSince(runStartTimestamp));
			}
			builder.failingSince(getFailingSince());
			checkRun = builder.build();
			if (runContext.saveCheckRun()) {
				recentCheckRuns.addCheckRun(checkRun);
			}
			running = false;
			log.debug("Ending run()");
			CheckMDC.remove();
		}
		return checkRun;
	}

	@Override
	public void init(Check check, @NonNull Configuration configuration, @NonNull App app, @NonNull String id) throws Exception {
		if (!initialized) {
			this.configuration = configuration;
			this.app = app;
			this.id = id; // In the case of an app the id needs to be set first.
			this.fullName = app.getId() + ":" + id;
			CheckMDC.put(this); //need fullname to be set
			if (check != null && !id.equals(check.getId())) {
				throw new IllegalArgumentException("Check ids must be equal to initalize from another getCheck");
			}

			if (executor == null) {
				throw new IllegalArgumentException("A execution must be set on check " + getFullName());
			}

			if (cacheMs == null) {
				cacheMs = app.getDefaultCacheMs();
			}
			if (timeoutMs == null) {
				timeoutMs = app.getDefaultTimeoutMs();
			}
			if (timeoutMs < 0) {
				RuntimeException e = new IllegalStateException("timeoutMs cannot be less than 0.");
				log.error("timeoutMs is set to {}", timeoutMs, e);
				throw e;
			}
			if (timeoutMs > configuration.getHardTimeoutMsLimit()) {
				timeoutMs = configuration.getHardTimeoutMsLimit();
			}
			if (cacheMs > configuration.getHardCacheMsLimit()) {
				cacheMs = configuration.getHardCacheMsLimit();
			}
			if (priority == null) {
				priority = app.getDefaultPriority();
			}
			if (alias == null) {
				alias = id;
			}
			String defaultCheckRunMessage;
			if (!active.isActive()) {
				defaultCheckRunMessage = "Check is not active";
			} else {
				defaultCheckRunMessage = "No checks run since initialization";
			}
			CheckRun defaultCheckRun = CheckRun.builder(this, null).succeeded().message(defaultCheckRunMessage).build();
			recentCheckRuns.setDefaultCheckRun(defaultCheckRun);
			if (recentCheckRuns.getSizeLimit() > configuration.getRecentChecksSizeLimit()) {
				setRecentCheckRunSize(configuration.getRecentChecksSizeLimit());
			}
			if (check != null) {
				//order must be preserved
				for (CheckRun checkRun : check.getRecentCheckRuns()) {
					recentCheckRuns.addCheckRun(checkRun);
				}
				setFailingSince(check.getFailingSince());
				setRestarting(check.isRestarting());
				executor.init(check.getExecutor(), this, configuration);
			} else {
				executor.init(null, this, configuration);
			}
			if (check instanceof DefaultCheck) {
				DefaultCheck defaultCheck = (DefaultCheck) check;
				runEndTimestamp = defaultCheck.runEndTimestamp;
				runStartTimestamp = defaultCheck.runStartTimestamp;
			}
			tags = Collections.unmodifiableSet(tags);
			initialized = true;
			if (isActive()) {
				log.info("Initialized {}", getType());
			}
		}
		CheckMDC.remove();
	}

	@Override
	public Collection<Check> runAfterSuccess() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Check> runAfterFailure() {
		return Collections.emptyList();
	}

	@Override
	public void close() throws Exception {
		log.debug("Closing check {}", id);
		recentCheckRuns = null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Check) { //implicit null check
			Check that = (Check) obj;
			if (id != null
					&& app != null
					&& that.getApp() != null
					&& id.equals(that.getId())
					&& app.getId().equals(that.getApp().getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return app.getId().hashCode() * 31 + id.hashCode();
	}

	/**
	 * Checks can be compared to one another. Checks with a higher getPriority() are
	 * first and when tied, checks with a higher getTimeoutMs() break that tie.
	 *
	 * @param check The check to compare this check to
	 * @return
	 */
	@Override
	public int compareTo(Check check) {
		// Sort by priority
		int compare = -Integer.compare(getPriority(), check.getPriority());
		if (compare == 0) {
			// Then by timeout so that longest timeouts get submitted first
			compare = -Long.compare(getTimeoutMs(), check.getTimeoutMs());
		}
		return compare;
	}
}
