package com.iisquare.fs.web.spider.parser;

import java.util.WeakHashMap;

public class ParserFactory {

    private final WeakHashMap<String, Parser> parsers;

    public ParserFactory() {
        this.parsers = new WeakHashMap<>();
    }

    public Parser parser(String name, String template) throws Exception {
        Parser parser = null == name ? null : parsers.get(name);
        if (null == parser) {
            if (template.startsWith(JsoupParser.PROTOCOL)) {
                parser = new JsoupParser().load(template.substring(JsoupParser.PROTOCOL.length()));
            } else if(template.startsWith(JsonParser.PROTOCOL)) {
                parser = new JsonParser().load(template.substring(JsonParser.PROTOCOL.length()));
            } else {
                return null;
            }
        }
        if (null != name) parsers.put(name, parser);
        return parser;
    }

}
