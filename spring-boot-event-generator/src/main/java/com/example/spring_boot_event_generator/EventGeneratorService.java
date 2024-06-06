package com.example.spring_boot_event_generator;

import com.github.javafaker.Faker;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(EventGeneratorService.class);


    private final Faker faker = new Faker();

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Scheduled(fixedRate = 1000)
    public void generateEvent() {
        Event event = new Event();
        event.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        event.setUserId(faker.name().username());
        event.setEventType("pageView");
        event.setProductId(faker.commerce().productName());
        event.setSessionDuration(faker.number().numberBetween(10, 300));

        String eventJson = new Gson().toJson(event);
        logger.info("Generated Event: {}", eventJson);

        kafkaProducerService.sendMessage("events_topic_1", eventJson);
    }
}
