package org.towerhawk.monitor.check;

import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.app.App;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.spring.config.Configuration;

import java.util.Collections;

/**
 * This is a class that is meant to be fully functional for testing
 */
@Slf4j
public class TestCheck extends DefaultCheck {

	public TestCheck() {
		this("testCheck");
	}

	public TestCheck(String checkId) {
		this("testApp", checkId);
	}

	public TestCheck(String appId, String checkId) {
		App app = new App();
		app.setChecks(Collections.singletonMap(checkId, this));
		try {
			app.init(null, new Configuration(), app, appId);
		} catch (Exception e) {
			log.error("Unable to initalize TestCheck app", e);
		}
		try {
			init(null, app.getConfiguration(), app, checkId);
		} catch (Exception e) {
			log.error("Unable to initialize TestCheck");
		}
	}
}