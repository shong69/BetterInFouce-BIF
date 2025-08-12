package com.sage.bif.todo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class AiTaskParseResponse {

    private String title;
    private String type;
    @JsonProperty("has_order")
    private boolean hasOrder;
    @JsonProperty("sub_tasks")
    private List<String> subTasks;
    private String time;
    private String date;
    @JsonProperty("repeat_frequency")
    private String repeatFrequency;
    @JsonProperty("repeat_days")
    private List<String> repeatDays;

}
