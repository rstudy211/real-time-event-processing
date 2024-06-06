package com.example.flink_job;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

//import static jdk.management.jfr.MBeanUtils.parseTimestamp;

public class FlinkJob {
    private static final String jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";
    private static final String username = "postgres";
    private static final String password = "postgres";
    public static void main(String[] args) throws Exception {
        // Create a StreamExecutionEnvironment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Define the Kafka topic name
        String topicName = "events_topic_1";

        // Create a Kafka source using KafkaSource
        KafkaSource<Event> kafkaConsumer =  KafkaSource.<Event> builder()
                .setBootstrapServers("localhost:9092")
                .setTopics(topicName)
                .setGroupId("my-group-id")
                .setStartingOffsets(OffsetsInitializer.earliest())
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
        )).name("Create Events Table Sink");

        // Insert Data to Events table
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

        // Overall Time-spent on website per user
        eventStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS time_spent (" +
                        "user_id VARCHAR(255), " +
                        "session_duration INTEGER " +
                        ")",
                (JdbcStatementBuilder<Event>) (preparedStatement, event) -> {

                },
                execOptions,
                connOptions
        )).name("Create Events Table Sink");

          eventStream.map(event -> {
            String userId = event.getUserId();
            Integer timeSpent = event.getSessionDuration();
            return new TimeSpentPerUser(userId,timeSpent);
        }
        ).keyBy(TimeSpentPerUser::getUserId)
                .reduce((timeSpentPerUser, t1) -> {
                            timeSpentPerUser.setTimeSpent(timeSpentPerUser.getTimeSpent() + t1.getTimeSpent());
                            return timeSpentPerUser;
                        }).addSink(JdbcSink.sink(
                        "insert into time_spent (user_id, session_duration) values (?,?)",
                        (JdbcStatementBuilder<TimeSpentPerUser>) (preparedStatement, timeSpentPerUser) -> {
                            preparedStatement.setString(1,timeSpentPerUser.getUserId());
                            preparedStatement.setInt(2, timeSpentPerUser.getTimeSpent());
                        },
                        execOptions,
                        connOptions
                )).name("Insert into events table Sink");
        eventStream.print();
        env.execute("Flink Kafka to JDBC Example");

    }


}
