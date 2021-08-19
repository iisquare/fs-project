package com.iisquare.fs.base.web.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public abstract class ControllerBase {

    @Value("${server.servlet.encoding.charset}")
    protected String charset;

    protected String redirect(HttpServletResponse response, String url) throws Exception {
        response.sendRedirect(url);
        return null;
    }

    protected String displayText(HttpServletResponse response, String text) throws Exception {
        return displayText(response, text, true);
    }

    protected String displayText(HttpServletResponse response, String text, boolean withHeader) throws Exception {
        if (withHeader) {
            response.setCharacterEncoding(charset);
            response.setContentType("text/plain; charset=" + charset);
        }
        PrintWriter out = response.getWriter();
        out.print(text);
        out.flush();
        return null;
    }

    protected String displayText(ServletOutputStream stream, String text) throws Exception {
        PrintWriter writer = new PrintWriter(stream);
        writer.print(text);
        writer.flush();
        writer.close();
        return null;
    }

    protected String displayJSON(HttpServletResponse response, Object json) throws Exception {
        return displayJSON(response, json, true);
    }

    protected String displayJSON(HttpServletResponse response, Object json, boolean withHeader) throws Exception {
        if (withHeader) {
            response.setCharacterEncoding(charset);
            response.setContentType("application/json; charset=" + charset);
        }
        return displayText(response, DPUtil.stringify(json), false);
    }

}
