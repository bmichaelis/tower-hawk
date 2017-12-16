package org.towerhawk.monitor.check.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.towerhawk.serde.resolver.TransformType;

import java.util.List;

@Slf4j
@TransformType("jq-multi")
public class JqMultiTransform implements Transform<List<JsonNode>> {

	protected transient ObjectMapper mapper = new ObjectMapper();
	@Getter
	protected transient JsonQuery jsonQuery;
	@Getter
	protected String query;

	public void setQuery(String query) {
		this.query = query;
		try {
			jsonQuery = JsonQuery.compile(query);
		} catch (JsonQueryException e) {
			log.error("Unable to compile jq query '{}'", query, e);
			throw new RuntimeException("Unable to compile jq query " + query, e);
		}
	}

	@JsonIgnore //Required to get Jackson to not throw
	public ObjectMapper getObjectMapper() {
		return mapper;
	}

	@Override
	public List<JsonNode> transform(Object value) throws Exception {
		JsonNode in = mapper.readTree(value.toString());
		List<JsonNode> result = jsonQuery.apply(in);
		return result;
	}
}
