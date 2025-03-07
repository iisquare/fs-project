package com.iisquare.fs.app.test.app;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortApp {

    @Builder
    @Data
    static class Bean {
        public Integer name;
        public Integer level;
        public Integer score;
    }

    public static void main(String[] args) {
        List<Bean> list = new ArrayList<>();
        list.add(Bean.builder().name(1).level(1).score(1).build());
        list.add(Bean.builder().name(2).level(1).score(1).build());
        list.add(Bean.builder().name(1).level(2).score(1).build());
        list.add(Bean.builder().name(1).level(1).score(2).build());
        list.add(Bean.builder().name(2).level(1).score(2).build());
        list.add(Bean.builder().name(2).level(1).score(3).build());
        List<Bean> collect = list.stream()
                .sorted(Comparator.comparingInt(Bean::getName).reversed()
                        .thenComparingInt(Bean::getLevel)
                        .thenComparing(Comparator.comparingInt(Bean::getScore).reversed()))
                .collect(Collectors.toList());
        for (Bean bean : collect) {
            System.out.println(bean);
        }
    }
}
