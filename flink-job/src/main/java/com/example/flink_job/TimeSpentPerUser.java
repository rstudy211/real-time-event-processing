package com.example.flink_job;

import lombok.*;

@Getter
@Setter
//@Entity
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSpentPerUser {
    public   String userId;
    public  Integer timeSpent;
}
