package com.iisquare.etl.spark.utils;

import java.util.List;

public class StatisticsUtil {

	public static double sum(List<Number> data) {
		double sum = 0;
		for (Number item : data) {
			sum = sum + item.doubleValue();
		}
		return sum;
	}

	public static double average(List<Number> data) {
		return sum(data) / data.size();
	}

	public static double squareSum(List<Number> data) {
		double sqrsum = 0.0;
		for (Number item : data) {
			sqrsum = sqrsum + item.doubleValue() * item.doubleValue();
		}
		return sqrsum;
	}

	/**
	 * 方差
	 */
	public static double variance(List<Number> data) {
		double sqrsum = squareSum(data);
		double average = average(data);
		int length = data.size();
		return (sqrsum - length * average * average) / length;
	}

	/**
	 * 标准差
	 */
	public static double standardDiviation(List<Number> data) {
		return Math.sqrt(Math.abs(variance(data)));
	}
	
}
