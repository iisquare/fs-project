package com.iisquare.jwframe.freemarker;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * 解码HTML特殊字符
 */
public class FreemarkerUnescapeHtmlModel implements TemplateMethodModelEx {
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List list) throws TemplateModelException {
		return StringEscapeUtils.unescapeHtml(list.get(0).toString());
	}
}