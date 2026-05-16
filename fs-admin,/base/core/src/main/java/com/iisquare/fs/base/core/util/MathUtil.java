package com.iisquare.fs.base.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MathUtil {

    public static List<Map.Entry<String, Integer>> topN(Map<String, Integer> map, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list = list.stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        return list.subList(0, Math.min(n, list.size()));
    }

    public static List<Map.Entry<String, Integer>> topM(Map<String, Integer> map, int m) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list = list.stream()
                .sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
        return list.subList(0, Math.min(m, list.size()));
    }

    public static double similarity(List<Double> va, List<Double> vb) {
        double similarity = 0.0;
        if (va.size() != vb.size()) return similarity;
        int size = va.size();
        double numSum = 0.0;
        double aPowSum = 0.0;
        double bPowSum = 0.0;
        for (int i = 0; i < size; i++) {
            double a = va.get(i), b = vb.get(i);
            numSum += a * b;
            aPowSum += Math.pow(a, 2);
            bPowSum += Math.pow(b, 2);
        }
        double den = Math.sqrt(aPowSum) * Math.sqrt(bPowSum);
        if (den != 0.0) similarity = numSum / den;
        return similarity;
    }

}
