package org.towerhawk.plugin.cron;

import com.coreoz.wisp.schedule.Schedule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.pf4j.Extension;
import org.towerhawk.monitor.schedule.ScheduleCollector;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Extension
@TowerhawkType({"cron", "unixCron", "quartz", "quartzCron"})
public class CronSchedule implements ScheduleCollector {

	private String name;
	private String type;
	private String cron;
	private Map<String, Schedule> schedules;

	@JsonCreator
	public CronSchedule (
			@JsonProperty("cron") @NonNull String cron,
			@JsonProperty("type") @NonNull String type,
			@JsonProperty("name") String name
	) {
		this.cron = cron;
		this.type = type;
		this.name = name == null ? cron : name;
		Schedule schedule = parseCron(cron);
		schedules = Collections.singletonMap(this.name, schedule);
	}

	protected Schedule parseCron(String cron) {
		if (type != null && type.toLowerCase().startsWith("quartz")) {
			return com.coreoz.wisp.schedule.cron.CronSchedule.parseQuartzCron(cron);
		} else {
			return com.coreoz.wisp.schedule.cron.CronSchedule.parseUnixCron(cron);
		}
	}
}
