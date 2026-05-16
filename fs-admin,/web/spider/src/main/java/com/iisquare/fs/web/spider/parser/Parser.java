package com.iisquare.fs.web.spider.parser;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class Parser {

    public Parser unload() {
        return this;
    }

    public Parser load(String template) throws Exception {
        return this.unload();
    }

    public abstract JsonNode parse(String html, String baseUri) throws Exception;

    public JsonNode expression() {
        return null;
    }

}
