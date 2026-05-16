package com.iisquare.fs.app.crawler.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class Intercept {

    private String id;
    private String name;
    private String description;
    private int code;
    private String parser;
    private String assistor;
    private String mapper;
    private String output;

    public Intercept(JsonNode intercept) {
        this.id = UUID.randomUUID().toString();
        this.name = intercept.at("/name").asText(this.id);
        this.description = intercept.at("/description").asText("");
        this.code = intercept.at("/code").asInt(0);
        this.parser = intercept.at("/parser").asText("");
        this.assistor = intercept.at("/assistor").asText("");
        this.mapper = intercept.at("/mapper").asText("");
        this.output = intercept.at("/output").asText("{}");
    }

}
