package com.example.flink_job;

//import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
//@Entity
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Event {
    private Long timestamp;
    private String userId;
    private String eventType;
    private String productId;
    private int sessionDuration;

    // Getters and Setters

}