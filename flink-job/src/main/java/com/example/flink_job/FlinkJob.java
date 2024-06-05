package com.example.flink_job;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.Transaction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//import static jdk.management.jfr.MBeanUtils.parseTimestamp;

public class FlinkJob {
    private static final String jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";
    private static final String username = "postgres";
    private static final String password = "postgres";
    public static void main(String[] args) throws Exception {
        // Create a StreamExecutionEnvironment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Define the Kafka topic name
        String topicName = "events_topic";

        // Create a Kafka source using KafkaSource
        KafkaSource<Event> kafkaConsumer =  KafkaSource.<Event> builder()
                .setBootstrapServers("localhost:9092")
                .setTopics(topicName)
                .setGroupId("my-group-id")
                .setValueOnlyDeserializer(new EventDeserializer())
                .build();


        DataStream<Event> eventStream = env.fromSource(kafkaConsumer, WatermarkStrategy.noWatermarks(), "Kafka source");

        JdbcExecutionOptions execOptions = new JdbcExecutionOptions.Builder()
                .withBatchSize(1000)
                .withBatchIntervalMs(200)
                .withMaxRetries(5)
                .build();

        JdbcConnectionOptions connOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(jdbcUrl)
                .withDriverName("org.postgresql.Driver")
                .withUsername(username)
                .withPassword(password)
                .build();

        //create Event table
        eventStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS events (" +
                        "timestamp TIMESTAMP,"+
                        "user_id VARCHAR(255), " +
                        "event_type VARCHAR(255), " +
                        "product_id VARCHAR(255), " +
                        "session_duration INTEGER " +
                        ")",
                (JdbcStatementBuilder<Event>) (preparedStatement, event) -> {

                },
                execOptions,
                connOptions
        )).name("Create Transactions Table Sink");

        eventStream.addSink(JdbcSink.sink(
                "insert into events (timestamp, user_id, event_type, product_id, session_duration) values (?,?,?,?,?)",
                (JdbcStatementBuilder<Event>) (preparedStatement, event) -> {
                    preparedStatement.setTimestamp(1,event.getTimestamp());
                    preparedStatement.setString(2,event.getUserId());
                    preparedStatement.setString(3, event.getEventType());
                    preparedStatement.setString(4, event.getProductId());
                    preparedStatement.setInt(5,event.getSessionDuration());
                },
                execOptions,
                connOptions
        )).name("Insert into events table Sink");
//
//        DataStream<Tuple2<String, Integer>> processedStream = eventStream
//                .map(event -> new Event())
////                .map(event -> new Gson().fromJson(event,Event))
//                .returns(TypeInformation.of(Event.class))
//                .keyBy(Event::getUserId)
//                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
////                .timeWindow(Time.minutes(1))
//                .reduce((e1, e2) -> {
//                    e1.setSessionDuration(e1.getSessionDuration() + e2.getSessionDuration());
//                    return e1;
//                })
//                .map(event -> new Tuple2<>(event.getUserId(), event.getSessionDuration()));
//
//        processedStream.print();


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
        eventStream.print();
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
