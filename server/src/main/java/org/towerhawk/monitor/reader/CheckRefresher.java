package org.towerhawk.monitor.reader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.towerhawk.monitor.app.App;
import org.towerhawk.plugin.TowerhawkPluginManager;
import org.towerhawk.serde.resolver.ExtensibleAPI;
import org.towerhawk.serde.resolver.TowerhawkIgnore;
import org.towerhawk.serde.resolver.TowerhawkType;
import org.towerhawk.spring.config.Configuration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Named
public class CheckRefresher {
	@Getter
	private Path definitionsDir;
	private ObjectMapper mapper;
	private TowerhawkPluginManager tpm;

	@Inject
	public CheckRefresher(Configuration configuration, TowerhawkPluginManager towerhawkPluginManager) {
		this(configuration.getCheckDefinitionDir(), towerhawkPluginManager);
	}

	public CheckRefresher(String definitionsDir, TowerhawkPluginManager towerhawkPluginManager) {
		this.tpm = towerhawkPluginManager;
		this.definitionsDir = Paths.get(definitionsDir);

		mapper = initalizeObjectMapper();

		SimpleModule module =
				new SimpleModule("TowerhawkDeserializerModule",
						new Version(1, 0, 0, null, null, null));

		PluginManager pluginManager = tpm.getPluginManager();

		FastClasspathScanner fastScanner = new FastClasspathScanner("org.towerhawk");
		for (PluginWrapper p : pluginManager.getPlugins()) {
			fastScanner.addClassLoader(p.getPluginClassLoader());
		}

		Map<Class, TowerhawkDeserializer> deserializerMap = new HashMap<>();

		ExtensibleAPI.CLASSES.forEach(apiClass -> {
			TowerhawkDeserializer deserializer = new TowerhawkDeserializer<>(apiClass);
			deserializerMap.put(apiClass, deserializer);
			fastScanner.matchClassesImplementing(apiClass, c -> {
				int mod = c.getModifiers();
				if (!Modifier.isInterface(mod) && !Modifier.isAbstract(mod) && !shouldIgnoreClass(c, apiClass)) {
					TowerhawkType t = c.getAnnotation(TowerhawkType.class);
					if (t != null) {
						for (String v : t.value()) {
							deserializer.register(v, c);
						}
					}
					deserializer.register(c.getSimpleName(), c);
					deserializer.register(c.getCanonicalName(), c);
				}
			});
		});

		String debug = System.getenv("DEBUG");
		if (debug == null) {
			debug = System.getProperty("debug");
		}
		boolean shouldDebug = debug != null && !debug.isEmpty() && "true".equals(debug.toLowerCase());
		fastScanner.verbose(shouldDebug);
		fastScanner.scan();
		log.info("Finished scanning for classes");

		deserializerMap.forEach((k, v) -> {
			v.defaultName("default");
			module.addDeserializer(k, v);
		});

		mapper.registerModule(module);
		ObjectMapper injectableMapper = initalizeObjectMapper();
		injectableMapper.registerModule(module);
		mapper.setInjectableValues(new InjectableValues.Std().addValue(ObjectMapper.class, injectableMapper));
	}

	private ObjectMapper initalizeObjectMapper() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
		mapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
		mapper.enable(JsonParser.Feature.IGNORE_UNDEFINED);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
		return mapper;
	}

	private boolean shouldIgnoreClass(Class<?> c, Class<?> apiClass) {
		if (c == null || Object.class.equals(c)) {
			return false;
		}
		TowerhawkIgnore ignore = c.getAnnotation(TowerhawkIgnore.class);
		if (ignore != null) {
			for (Class compare : ignore.value()) {
				if (c.equals(compare)) {
					return true;
				} else if (apiClass.equals(compare)) {
					return true;
				}
			}
		}
		for (Class i : c.getInterfaces()) {
			if (shouldIgnoreClass(i, apiClass)) {
				return true;
			}
		}
		return shouldIgnoreClass(c.getSuperclass(), apiClass);
	}

	public static boolean validFile(File file) {
		return file.toString().endsWith(".yaml") || file.toString().endsWith(".yml") || file.toString().endsWith(".json");
	}

	@Synchronized
	public CheckDTO readDefinitions() {
		List<CheckDTO> checkDTOs = new ArrayList<>();
		for (File file : definitionsDir.toFile().listFiles()) {
			if (validFile(file)) {
				try {
					log.info("Refreshing file {}", file);
					checkDTOs.add(mapper.readValue(file, CheckDTO.class));
				} catch (JsonMappingException e) {
					if (e.getMessage().startsWith("No content to map")) {
						log.warn("Excluding file {} because no input is available due to exception {}", file.toString(), e.getMessage());
					} else {
						throw new IllegalArgumentException("Unable to deserialize yaml file " + file.toString() + " to object structure", e);
					}
				} catch (Exception e) {
					log.error("Failed to deserialize yaml file {}", file.toString(), e);
					throw new IllegalArgumentException("Failed to deserialize yaml file " + file.toString(), e);
				}
			}
		}
		return mergeChecks(checkDTOs);
	}

	private CheckDTO mergeChecks(List<CheckDTO> checkDTOS) {
		Map<String, App> apps = new LinkedHashMap<>();
		checkDTOS.stream().forEachOrdered(dtoConsumer -> apps.putAll(dtoConsumer.getApps()));
		return new CheckDTO(apps);
	}
}
