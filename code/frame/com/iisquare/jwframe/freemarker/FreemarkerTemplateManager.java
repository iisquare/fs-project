package com.iisquare.jwframe.freemarker;

/**
 * FreeMarker自定义函数管理器
 */
public class FreemarkerTemplateManager {
	
	public void setFreeMarkerConfigurer(FreeMarkerConfigurer freeMarkerConfigurer) {
		freemarker.template.Configuration fmConfiguration = freeMarkerConfigurer.getConfiguration();
		fmConfiguration.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
		fmConfiguration.setSharedVariable("millisToDateTime",
        		new FreemarkerMillisToDateTimeModel(freeMarkerConfigurer.getApplicationConfiguration()));
		fmConfiguration.setSharedVariable("empty", new FreemarkerEmptyModel());
		fmConfiguration.setSharedVariable("escapeHtml", new FreemarkerEscapeHtmlModel());
		fmConfiguration.setSharedVariable("unescapeHtml", new FreemarkerUnescapeHtmlModel());
		fmConfiguration.setSharedVariable("subStringWithByte", new FreemarkerSubStringWithByteModel());
		fmConfiguration.setSharedVariable("processPagination", new FreemarkerProcessPaginationModel());
	}
	
	public FreemarkerTemplateManager() {}
}
