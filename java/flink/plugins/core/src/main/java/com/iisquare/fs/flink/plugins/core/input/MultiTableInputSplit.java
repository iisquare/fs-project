package com.iisquare.fs.flink.plugins.core.input;

import lombok.Getter;
import org.apache.flink.core.io.InputSplit;

import java.util.Map;

@Getter
public class MultiTableInputSplit implements InputSplit {

    private int splitNumber;
    private Map<String, Object> config;

    public MultiTableInputSplit(int splitNumber, Map<String, Object> config) {
        this.splitNumber = splitNumber;
        this.config = config;
    }

    @Override
    public int getSplitNumber() {
        return splitNumber;
    }
}
