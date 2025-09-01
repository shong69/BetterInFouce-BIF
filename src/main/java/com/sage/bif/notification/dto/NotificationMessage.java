package com.sage.bif.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String title;
    private String body;
    private Long todoId;
    private String type;
    private Long timestamp;

}
