//package org.towerhawk.serde.deserializer;
//
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
//
//import java.io.IOException;
//import java.util.regex.Pattern;
//
//public class PatternDeserializer extends StdDeserializer<Pattern> {
//
//	public PatternDeserializer() {
//		super(Pattern.class);
//	}
//
//	@Override
//	public Pattern deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
//		return Pattern.compile(p.readValueAs(String.class));
//	}
//}
