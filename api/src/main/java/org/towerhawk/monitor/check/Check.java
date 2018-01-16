package org.towerhawk.monitor.check;

import org.pf4j.ExtensionPoint;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.active.Active;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.check.evaluation.Evaluation;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.Status;
import org.towerhawk.monitor.check.run.context.RunContext;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

public interface Check extends ExtensionPoint, Comparable<Check>, AutoCloseable {

	/**
	 * The id for this check. This should match the dictionary key in the config yaml.
	 * This must be unique within an App.
	 *
	 * @return the Id of this check
	 */
	String getId();

	default String getFullName() {
		return getApp().getId() + ":" + getId();
	}

	/**
	 * This is where the real work happens. A CheckRun is returned containing information
	 * about how the check went. This can be a synchronized method to ensure that multiple
	 * runs don't happen concurrently. Logic should be in place in this method to see if
	 * a check can run (see canRun()) and if this method gets called concurrently
	 * the second invocation can return the results of the first invocation.
	 *
	 * @param runContext
	 * @return The CheckRun representing the results of this run().
	 */
	CheckRun run(RunContext runContext);

	/**
	 * This determines whether the check is active right now or not. This allows different
	 * strategies to be implemented like daily or weekly schedules. This can also be used
	 * to effectively disable a check.
	 *
	 * @return true if the check can run, false otherwise.
	 */
	default boolean isActive() {
		return true;
	}

	void setActive(Active active);

	/**
	 * Return how long the check should be cached for. If the last run is less than
	 * getCacheMs() milliseconds ago then the check can return a cached CheckRun.
	 *
	 * @return cache time in milliseconds
	 */
	long getCacheMs();

	/**
	 * How long to let a check run before interrupting it. If a check is running for
	 * more than getTimeoutMs() milliseconds then it should be cancelled and return a
	 * CheckRun with an Error and the isTimedOut() method returning true.
	 *
	 * @return timeout in milliseconds
	 */
	long getTimeoutMs();

	/**
	 * Returns how many milliseconds this check has to finish running. Useful when using
	 * other libraries that have timeouts so they can timeout before the check does to do
	 * better error handling.
	 *
	 * @param throwException Whether or not to throw an IllegalStateException indicating that
	 *                       this Check has timed out.
	 * @return the number of milliseconds remaining before timeout
	 */
	public long getMsRemaining(boolean throwException);

	/**
	 * If the most recent CheckRun is in a failed state, this should tell when this check
	 * entered a failed state. It should keep a consistent time until the check successfully
	 * completes.
	 *
	 * @return The first time this check started failing recently.
	 */
	ZonedDateTime getFailingSince();

	/**
	 * The amount of time this check needs to fail before it will actually be reported.
	 *
	 * @return A duration representing how long this check needs to be failing for
	 * before it is reported. If the duration has not passed, the ${@link CheckRun} will
	 * be set to ${@link Status#SUCCEEDED}
	 */
	Duration getAllowedFailureDuration();

	/**
	 * @return If this check has been set to restarting. If it is restarting, then it will
	 * not return anything other than ${@link Status#SUCCEEDED} until it has actually
	 * completed successfully
	 */
	boolean isRestarting();

	/**
	 * @param restarting true if this check is restarting, false otherwise
	 */
	void setRestarting(boolean restarting);

	/**
	 * Checks with higher priorities must be run first. This method can also be called to
	 * run checks with a certain priority.
	 *
	 * @return the priority of the check
	 */
	byte getPriority();

	/**
	 * An alias for this check. When looking up checks by id, this method should also be
	 * consulted which allows for migration. This should be unique within an App.
	 *
	 * @return
	 */
	String getAlias();

	/**
	 * The type of the check as defined in the yaml config. This is available so
	 * that all checks of a type can be run together.
	 *
	 * @return The type defined in the config yaml.
	 */
	String getType();

	/**
	 * This allows an execution to be called directly or to be handed to a new check.
	 *
	 * @return The execution that this check is using
	 */
	CheckExecutor getExecutor();

	void setExecutor(CheckExecutor executor);

	Evaluation getEvaluation();

	void setEvaluation(Evaluation evaluation);

	/**
	 * All checks belong to an App.
	 *
	 * @return The App this check belongs to.
	 */
	App getApp();

	/**
	 * Returns a set of tags that are used to be able to run a subset of checks.
	 *
	 * @return The tags defined in the config yaml
	 */
	Set<String> getTags();

	/**
	 * @return The most recent {@link CheckRun}. Equivalent to getting the last element of
	 * getRecentCheckRuns()
	 */
	CheckRun getLastCheckRun();

	/**
	 * @return A sorted list of check runs with the oldest first and most recent last.
	 */
	List<CheckRun> getRecentCheckRuns();

	/**
	 * Determines if this check is currently running.
	 *
	 * @return true if running, false otherwise
	 */
	boolean isRunning();

	/**
	 * Determines if this check will return a cached CheckRun instead of actually running.
	 *
	 * @return false if check will actually run, true if a cached CheckRun will be returned
	 */
	boolean isCached();

	/**
	 * Returns any checks that need to be run before this check. If all checks listed here
	 * are successful then this check can be run
	 *
	 * @return a collection if there are dependencies a null or empty collection will be
	 * ignored.
	 */
	List<Check> runAfterSuccess();

	/**
	 * Returns any checks that need to fail before this check should run. If any of the checks
	 * have a {@link Status} that != SUCCEEDED then
	 * this check can be run
	 *
	 * @return a collection if there are dependences that need to fail. A null or empty
	 * return value will be ignored.
	 */
	List<Check> runAfterFailure();

	/**
	 * Determines if the check can run right now. The default implementation returns true
	 * if isActive() is true and isRunning() is false and isCached() is false. isCached()
	 * should be called last since that is potentially the most expensive to compute. If this
	 * returns false then CheckRunner implementations can skip calling run() and return
	 * getLastCheckRun()
	 *
	 * @return
	 */
	default boolean canRun() {
		return isActive() && !isRunning() && !isCached();
	}

	/**
	 * Similar to a @PostConstruct. This method should be called by the deserialization
	 * framework to allow checks to initialize any state they need to, like caching a
	 * computation, setting default objects, or starting a background thread.
	 *
	 * @param previousCheck The previous check that was defined with the same App and Id
	 * @param app   The app that this check belongs to
	 * @param id    The name of this check used in returning values and in logging
	 */
	void init(Check previousCheck, Config config, App app, String id) throws Exception;

	/**
	 * Checks can be compared to one another. Checks with a higher getPriority() are
	 * first and when tied, checks with a lower getTimeoutMs() break that tie.
	 *
	 * @param check The check to compare this check to
	 * @return
	 */
	default int compareTo(Check check) {
		// Sort by priority
		int compare = -Integer.compare(getPriority(), check.getPriority());
		if (compare == 0) {
			// Then by timeout so that longest timeouts get submitted first
			compare = -Long.compare(getTimeoutMs(), check.getTimeoutMs());
		}
		return compare;
	}
}
