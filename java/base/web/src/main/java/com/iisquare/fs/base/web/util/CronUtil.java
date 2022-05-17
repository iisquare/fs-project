package com.iisquare.fs.base.web.util;

import com.iisquare.fs.base.core.util.DPUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CronUtil {

    public static void initialize(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-type","text/html;charset=utf-8");
    }

    public static void flushStringify(HttpServletResponse response, Object message, boolean withLine) throws IOException {
        flush(response, DPUtil.stringify(message), withLine);
    }

    public static void flush(HttpServletResponse response, String message, boolean withLine) throws IOException {
        PrintWriter printer = response.getWriter();
        if(withLine) {
            printer.println(message);
        } else {
            printer.print(message);
        }
        printer.flush();
    }

    public static void flushString(HttpServletResponse response, boolean withLine, String format, Object... args) throws IOException {
        flush(response, String.format(format, args), withLine);
    }

    public static void flush(HttpServletResponse response, Exception e, boolean withLine) {
        try {
            e.printStackTrace(response.getWriter());
        } catch (IOException ex) {
        }
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}
