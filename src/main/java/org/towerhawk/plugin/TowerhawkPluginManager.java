package org.towerhawk.plugin;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.nio.file.Paths;

@Slf4j
public class TowerhawkPluginManager {

	private static PluginManager pluginManager;

	public static PluginManager create(String pluginsDir) {
		pluginManager = new DefaultPluginManager(Paths.get(pluginsDir));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> pluginManager.stopPlugins()));
		pluginManager.loadPlugins();
		pluginManager.startPlugins();
		return get();
	}

	public static PluginManager get() {
		return pluginManager;
	}
}
