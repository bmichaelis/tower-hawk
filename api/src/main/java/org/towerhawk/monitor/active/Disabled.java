package org.towerhawk.monitor.active;

import org.towerhawk.serde.resolver.TowerhawkType;

@TowerhawkType("disabled")
public class Disabled implements Active {

	@Override
	public boolean isActive() {
		return false;
	}
}
