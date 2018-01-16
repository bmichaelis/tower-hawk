package org.towerhawk.monitor.check.evaluation.transform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class LoggerTransform implements Transform {

	private String level = "INFO";
	private String name = "logger";
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public void setLevel(String level) {
		this.level = level.toUpperCase();
	}
	public void setName(String name) {
		this.name = name;
		log = LoggerFactory.getLogger(this.getClass().getCanonicalName() + "." + name);
	}

	@Override
	public Object transform(Object value) throws Exception {
		switch (level) {
			case "ERROR":
				log.error(value.toString());
				break;
			case "WARN":
				log.warn(value.toString());
				break;
			case "DEBUG":
				log.debug(value.toString());
				break;
			case "TRACE":
				log.trace(value.toString());
				break;
			default:
				log.info(value.toString());
		}
		return value;
	}
}
