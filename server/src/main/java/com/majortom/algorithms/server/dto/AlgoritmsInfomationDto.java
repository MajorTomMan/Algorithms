package com.majortom.algorithms.server.dto;

import lombok.Data;

@Data
public class AlgoritmsInfomationDto {
    private String id;
    private String moduleId;
    private String version;
    private String inputType;
    private String outputType;
    private String capabilities;
}
