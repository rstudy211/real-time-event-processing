package com.example.flink_job;
import com.example.flink_job.Event;
import com.google.gson.Gson;
import lombok.val;
import org.apache.flink.api.common.serialization.DeserializationSchema;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

//import static jdk.management.jfr.MBeanUtils.parseTimestamp;

public class FlinkJob {
    public static void main(String[] args) throws Exception {
        // Create a StreamExecutionEnvironment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka consumer configuration
        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("bootstrap.servers", "localhost:9092"); // Replace with your Kafka broker address
        kafkaProperties.setProperty("group.id", "user_activity_analysis");

        // Define the Kafka topic name
        String topicName = "events_topic";

        // Create a Kafka source using FlinkKafkaConsumer
        FlinkKafkaConsumer<Event> kafkaConsumer = new FlinkKafkaConsumer<>(
                topicName,
                new EventDeserializer(),
                kafkaProperties);

        // Create a DataStream from the Kafka source
        DataStream<Event> eventStream = env.addSource(kafkaConsumer);



        DataStream<Tuple2<String, Integer>> processedStream = eventStream
                .map(event -> new Event())
//                .map(event -> new Gson().fromJson(event,Event))
                .returns(TypeInformation.of(Event.class))
                .keyBy(Event::getUserId)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
//                .timeWindow(Time.minutes(1))
                .reduce((e1, e2) -> {
                    e1.setSessionDuration(e1.getSessionDuration() + e2.getSessionDuration());
                    return e1;
                })
                .map(event -> new Tuple2<>(event.getUserId(), event.getSessionDuration()));



//        JdbcSink<Tuple2<String, Integer>> jdbcSink = JdbcSink.sink(
//                "INSERT INTO user_sessions (userId, sessionDuration) VALUES (?, ?) ON CONFLICT (userId) DO UPDATE SET sessionDuration = EXCLUDED.sessionDuration",
//                (statement, tuple) -> {
//                    statement.setString(1, tuple.f0);
//                    statement.setInt(2, tuple.f1);
//                },
//                JdbcExecutionOptions.builder()
//                        .withBatchSize(1000)
//                        .withBatchIntervalMs(200)
//                        .withMaxRetries(5)
//                        .build(),
//                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//                        .withUrl(System.getenv("JDBC_URL"))
//                        .withDriverName("org.postgresql.Driver")
//                        .withUsername(System.getenv("JDBC_USERNAME"))
//                        .withPassword(System.getenv("JDBC_PASSWORD"))
//                        .build()
//        );

        //parse Timestamps
//        DataStream<Event> timestampedStream = eventStream
//                .map(event -> Event.builder()
//                        .userId(event.getUserId())
//                        .sessionDuration(event.getSessionDuration())
//                        .timestamp(parseTimestamp(String.valueOf(event.getTimestamp()))) // Call timestamp parsing function
//                        .build());
//        processedStream.addSink(jdbcSink);
        processedStream.print();
        env.execute("Flink Kafka to JDBC Example");

    }

    private static Long parseTimestamp(String timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        try {
            Date timestampDate = sdf.parse(timestamp);
            return timestampDate.getTime();
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing timestamp: " + e.getMessage());
        }
    }
}
