package com.example.flink_job;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

// Custom deserializer for Event objects (replace if using different message format)
public class EventDeserializer implements DeserializationSchema<Event> {

    @Override
    public Event deserialize(byte[] message) throws IOException {
        String json = new String(message);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Event.class);
    }

    @Override
    public boolean isEndOfStream(Event event) {
        return false;
    }


    @Override
    public TypeInformation<Event> getProducedType() {
        return null;
    }
}
