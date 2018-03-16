package org.towerhawk.monitor.schedule;

import com.coreoz.wisp.schedule.Schedule;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.util.Collections;
import java.util.Map;

@TowerhawkType({"empty", "noOp"})
public class EmptyScheduleCollector implements ScheduleCollector {

	@Override
	public Map<String, Schedule> getSchedules() {
		return Collections.emptyMap();
	}
}
