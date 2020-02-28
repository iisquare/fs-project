package com.iisquare.jwframe.freemarker;

import java.util.List;

import com.iisquare.jwframe.utils.DPUtil;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * 按字节截取字符串
 */
public class FreemarkerSubStringWithByteModel implements TemplateMethodModelEx {
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List list) throws TemplateModelException {
		Object suffix = DPUtil.getByIndex(list, 2);
		Object encoding = DPUtil.getByIndex(list, 3);
		return DPUtil.subStringWithByte(list.get(0).toString(), DPUtil.parseInt(list.get(1)),
				null == suffix ? null : suffix.toString(), null == encoding ? null : encoding.toString());
	}
}