package com.example.spring_boot_event_generator;

//import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
//@Entity
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private String timestamp;
    private String userId;
    private String eventType;
    private String productId;
    private int sessionDuration;

    // Getters and Setters
}
