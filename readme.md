# Real-time Event Processing with Apache Flink

This project implements a real-time event processing pipeline using Apache Flink, Spring Boot, Kafka, and PostgreSQL. It consists of two microservices:

- **Event Generation Service (Spring Boot)**:
  - Generates simulated events (you can replace this with your actual event source).
  - Publishes events to a Kafka topic.

- **Flink Streaming Job**:
  - Consumes events from the Kafka topic.
  - Processes the events (e.g., filtering, aggregation, transformation).
  - Persists the processed data to a PostgreSQL database.

## Prerequisites

- Java 11 or later
- Apache Maven 3.x ([Download Maven](https://maven.apache.org/download.cgi))
- Docker Desktop ([Download Docker](https://www.docker.com/products/docker-desktop))
- Apache Flink 1.18.1 ([Download Flink](https://flink.apache.org/downloads/))
- PostgreSQL ([Download PostgreSQL](https://www.postgresql.org/download/))

## Running the Project

### Configure Kafka and PostgreSQL

If you have Docker Desktop installed, you can simply run the Docker Compose setup which is already included in the repository:

1. Navigate to the project directory in your terminal.
2. Start the Docker Compose setup:

    ```bash
    docker-compose up -d
    ```

   This will set up Kafka, Zookeeper, and PostgreSQL.

### Configure PostgreSQL

1. Connect to your PostgreSQL instance and create the `processed_events` table:

    ```sql
    CREATE DATABASE events_db;
    \c events_db;
    CREATE TABLE processed_events (
      id SERIAL PRIMARY KEY,
      event_data JSONB NOT NULL
    );
    ```

### Set Up Apache Flink Locally

1. Download Apache Flink version 1.18.1 from the [Flink Download Page](https://flink.apache.org/downloads/).
2. Extract the downloaded archive to your preferred directory.
3. Navigate to the `bin` directory inside the extracted Flink folder.
4. Start the Flink cluster locally:

    ```bash
    ./start-cluster.sh
    ```

5. Verify that the Flink web interface is running at [http://localhost:8081](http://localhost:8081).

6. Submit the Flink job:

    ```bash
    ./flink run -c com.example.flinkjob.YourFlinkJobClass /path/to/your/flink-job-1.0.0-SNAPSHOT.jar
    ```

   Replace `com.example.flinkjob.YourFlinkJobClass` with the fully qualified name of your Flink job class and `/path/to/your/flink-job-1.0.0-SNAPSHOT.jar` with the path to your built Flink job JAR file.

### Verify the Results

1. Check the Kafka topic (`events`) to see incoming messages generated by the Spring Boot service.
2. Use a PostgreSQL client to connect to your database and query the `processed_events` table to see the processed data.

### Build and Run the Project

1. Open a terminal in the project directory.
2. Navigate to flink-job directory Build the project with Maven:

    ```bash
    mvn clean package
    ```

3. Run the Spring Boot event generation service:

    ```bash
    java -jar event-generation-service/flink-job/target/flink-job-1.0.0-SNAPSHOT.jar
    ```

4. Start the Flink job as described in the Set Up Apache Flink Locally section.
