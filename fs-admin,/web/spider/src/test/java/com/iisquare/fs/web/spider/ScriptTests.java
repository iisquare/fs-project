package com.iisquare.fs.web.spider;

import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class ScriptTests {

    ScriptEngineManager scriptEngineManager;

    public ScriptTests() {
        scriptEngineManager = new ScriptEngineManager();
    }

    @Test
    public void allTest() {
        List<ScriptEngineFactory> engines = scriptEngineManager.getEngineFactories();
        for (ScriptEngineFactory f : engines) {
            System.out.println(f.getLanguageName() + " " + f.getEngineName() + " " + f.getNames());
        }
    }

    @Test
    public void jsTest() throws ScriptException {
        ScriptEngine engine = scriptEngineManager.getEngineByName("js");
        String script = "1 + 1";
        Object result = engine.eval(script);
        System.out.println(result);
    }

}
