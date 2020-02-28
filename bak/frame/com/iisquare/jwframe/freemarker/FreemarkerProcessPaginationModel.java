package com.iisquare.jwframe.freemarker;

import java.util.ArrayList;
import java.util.List;

import com.iisquare.jwframe.utils.DPUtil;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * 生成分页列表
 */
public class FreemarkerProcessPaginationModel implements TemplateMethodModelEx {
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List list) throws TemplateModelException {
		String commandName = list.get(0).toString();
		if("numberList".equals(commandName)) return getNumberList(list);
		if("staticUrl".equals(commandName)) return getStaticUrl(list);
		if("url".equals(commandName)) return getUrl(list);
		return null;
	}
	
	/**
	 * 获取静态化分页地址
	 * list[1] - 当前路径
	 * list[2] - 处理页码
	 */
	public String getStaticUrl(List<?> list) {
		String webUrl = list.get(1).toString(); // 当前路径
		int page = DPUtil.parseInt(DPUtil.getByIndex(list, 2)); // 处理页码
		return webUrl.replaceFirst("-\\d+\\.shtml", DPUtil.stringConcat("-", page, ".shtml"));
	}
	
	/**
	 * 获取分页地址
	 * list[1] - 当前路径
	 * list[2] - 处理页码
	 * list[3] - 分页标识，默认为page
	 */
	public String getUrl(List<?> list) {
		String webUrl = list.get(1).toString(); // 当前路径
		int page = DPUtil.parseInt(list.get(2)); // 处理页码
		String pageField = DPUtil.parseString(list.get(3)); // 分页标识
		if(DPUtil.empty(pageField)) pageField = "page";
		String regexPage = DPUtil.stringConcat("([\\?&])", pageField, "=\\d*");
		if(DPUtil.isMatcher(regexPage, webUrl)) {
			return webUrl.replaceFirst(regexPage, DPUtil.stringConcat("$1", pageField, "=", page));
		}
		return DPUtil.stringConcat(webUrl, -1 == webUrl.indexOf("?") ? "?" : "&", pageField, "=", page);
	}
	
	/**
	 * 获取页码列表
	 * list[1] - 当前页码
	 * list[2] - 分页大小
	 * list[3] - 总记录数
	 * list[4] - 分页列表大小
	 */
	public List<Integer> getNumberList(List<?> list) {
		int pageCurrent = DPUtil.parseInt(DPUtil.getByIndex(list, 1)); // 当前页码
		if(pageCurrent < 1) pageCurrent = 1;
		int pageSize = DPUtil.parseInt(DPUtil.getByIndex(list, 2)); // 分页大小
		if(pageSize < 1) pageSize = 1;
		int pageTotal = DPUtil.parseInt(DPUtil.getByIndex(list, 3)); // 总记录数
		if(pageTotal < 0) pageTotal = 0;
		int pageListCount = DPUtil.parseInt(DPUtil.getByIndex(list, 4)); // 分页列表大小
		if(pageListCount < 1) pageListCount = 1;
		int pageCount = (int) Math.ceil((double) pageTotal / (double) pageSize); // 总页数
		if(pageCount < 1) pageCount = 1;
		int pageListStart = pageCurrent - pageListCount / 2; // 开始页数
		int pageListEnd = pageListStart + pageListCount - 1; // 结束页数
		if(1 > pageListStart) { // 开始页数溢出处理
			pageListEnd -= pageListStart - 1;
			pageListStart = 1;
		}
		if(pageListEnd > pageCount) { // 结束页数溢出处理
			pageListStart -= pageListEnd - pageCount;
			if(pageListStart < 1) pageListStart = 1;
			pageListEnd = pageCount;
		}
		/* 生成分页列表 */
		List<Integer> numberList = new ArrayList<Integer>(pageListCount);
		for (int i = pageListStart; i <= pageListEnd; i++) {
			numberList.add(i);
		}
		return numberList;
	}
}