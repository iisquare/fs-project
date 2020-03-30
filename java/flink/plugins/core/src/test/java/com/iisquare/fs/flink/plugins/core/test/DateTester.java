package com.iisquare.fs.flink.plugins.core.test;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTester {

    @Test
    public void numTest() {
        int length = 3;
        int index = 0;
        for (int j = 0; j < 10; j++) {
            index = index < length ? index : 0;
            System.out.println(index++);
        }
    }

    @Test
    public void calendarTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE);
        System.out.println(dateFormat.format(calendar.getTime()));
    }

    @Test
    public void parseTest() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z", Locale.ENGLISH);
        Date date = dateFormat.parse("01/Aug/2018:18:33:10 +0800");
        System.out.println(date);
    }

}
