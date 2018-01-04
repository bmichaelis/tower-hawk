package org.towerhawk.monitor.check.execution.system.util;

import oshi.SystemInfo;

public class OSHIUtil {

	private static SystemInfo systemInfo = new SystemInfo();

	public static SystemInfo getSystemInfo() {
		return systemInfo;
	}

}
