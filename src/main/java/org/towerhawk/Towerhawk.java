package org.towerhawk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.towerhawk.plugin.TowerhawkPluginManager;

@SpringBootApplication
public class Towerhawk {

	public static void main(String[] args) {
		TowerhawkPluginManager.create(resolvePluginDir(args));
		SpringApplication.run(Towerhawk.class, args);
	}

	/**
	 * Respects args first then system properties then environment variables
	 *
	 * @param args
	 * @return A string representing the path to the plugins directory
	 */
	private static String resolvePluginDir(String[] args) {
		String dir;
		if (args.length > 0) {
			return args[0];
		} else if ((dir = System.getProperty("towerhawk.plugins.dir")) != null) {
			return dir;
		} else if ((dir = System.getenv("TOWERHAWK_PLUGINS_DIR")) != null) {
			return dir;
		} else {
			return "config/plugins";
		}
	}
}
