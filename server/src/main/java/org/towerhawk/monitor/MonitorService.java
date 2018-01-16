package org.towerhawk.monitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.active.Enabled;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.app.DefaultApp;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.run.CheckRunner;
import org.towerhawk.monitor.check.run.concurrent.AsynchronousCheckRunner;
import org.towerhawk.monitor.check.run.context.DefaultRunContext;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.monitor.reader.CheckDTO;
import org.towerhawk.monitor.reader.CheckRefresher;
import org.towerhawk.serde.resolver.TowerhawkIgnore;
import org.towerhawk.spring.config.Configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@TowerhawkIgnore(ignore = MonitorService.class)
@Named
public class MonitorService extends DefaultApp {

	private final CheckRunner checkCheckRunner;
	private final RunContext runContext = new DefaultRunContext();
	@Getter
	private ZonedDateTime lastRefresh = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
	private Configuration configuration;
	private CheckRefresher refresher;
	private Map<String, App> apps = new LinkedHashMap<>();

	@Inject
	public MonitorService(Configuration configuration,
												CheckRefresher refresher,
												AsynchronousCheckRunner checkCheckRunner,
												AsynchronousCheckRunner appCheckRunner) {
		super();
		this.id = "monitorService";
		this.configuration = configuration;
		this.refresher = refresher;
		setConfig(configuration);
		setCheckRunner(appCheckRunner);
		this.checkCheckRunner = checkCheckRunner;
		this.timeoutMs = configuration.getHardTimeoutMsLimit();
	}

	@PostConstruct
	public void postConstruct() {
		try {
			init(null, configuration, "monitorService");
		} catch (Exception e) {
			log.error("Unable to initialize {}", this.getClass().getSimpleName(), e);
		}
		boolean refreshed = refreshDefinitions();
		if (!refreshed && configuration.isShutdownOnInitializationFailure()) {
			log.error("##################################################################");
			log.error("##########  Unable to initialize checks. Shutting down  ##########");
			log.error("##################################################################");
			System.exit(1);
		}
		if (refreshed && configuration.isRunChecksOnStartup() && !configuration.isRunChecksOnRefresh()) {
			containingCheck.run(runContext);
		}
	}

	public boolean refreshDefinitions() {
		try {
			CheckDTO newDefs = refresher.readDefinitions();
			postProcess(newDefs);
			Map<String, App> oldApps = apps;
			Map<String, Check> newChecks = new LinkedHashMap<>();
			newDefs.getApps().forEach((k, v) -> newChecks.put(k, v.getContainingCheck()));

			apps = Collections.unmodifiableMap(newDefs.getApps());
			checks = Collections.unmodifiableMap(newChecks);

			oldApps.forEach((id, app) -> {
				try {
					app.close();
				} catch (Exception e) {
					log.error("App {} failed to close with exception", app.getId(), e);
				}
			});

			if (configuration.isRunChecksOnRefresh()) {
				containingCheck.run(runContext);
			}
			lastRefresh = ZonedDateTime.now();
		} catch (RuntimeException e) {
			log.error("Failed to load new checks", e);
			return false;
		}
		return true;
	}

	public App getApp(String appId) {
		return apps.get(appId);
	}

	@Override
	public String predicateKey() {
		return "appPredicate";
	}

	public String appPredicateKey() {
		return super.predicateKey();
	}

	private void postProcess(CheckDTO checkDTO) {
		Collection<App> appsToClose = new ArrayList<>(apps.size());
		//initialize all apps first
		checkDTO.getApps().forEach((id, app) -> {
			app.setCheckRunner(checkCheckRunner);
			App previousApp = apps.get(app.getId());
			try {
				app.init(previousApp, configuration, id);
			} catch (Exception e) {
				log.error("Unable to initialize app {}", getId());
			}
		});
	}

}
