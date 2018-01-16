package org.towerhawk.monitor.active;

import org.towerhawk.serde.resolver.TowerhawkType;

@TowerhawkType(value = {"enabled", "default"})
public class Enabled implements Active {

	@Override
	public boolean isActive() {
		return true;
	}
}
