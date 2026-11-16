package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.dao.DataThemeDao;
import com.iisquare.fs.web.bi.entity.DataTheme;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataThemeServiceBatchTest {

    @Test
    public void infosLoadsThemesOnceAndKeepsRequestedOrder() {
        DataThemeDao dao = mock(DataThemeDao.class);
        DatasetService datasetService = mock(DatasetService.class);
        DataThemeService service = new DataThemeService();
        service.dataThemeDao = dao;
        service.datasetService = datasetService;
        service.rbacService = mock(DefaultRbacService.class);
        when(dao.findAllById(any())).thenReturn(List.of(
                theme(2, "客户主题"), theme(1, "销售主题")));
        when(datasetService.fillInfos(any(), eq("datasetIds")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ids", List.of(1, 2, 1, 3));

        Map<String, Object> result = service.infos(param);

        Assert.assertTrue(ApiUtil.succeed(result));
        ObjectNode data = ApiUtil.data(result, ObjectNode.class);
        Assert.assertEquals(2, data.at("/total").asInt());
        Assert.assertEquals(1, data.at("/rows/0/id").asInt());
        Assert.assertEquals("销售主题", data.at("/rows/0/name").asText());
        Assert.assertEquals(1, data.at("/rows/0/content/datasetIds/0").asInt());
        Assert.assertEquals(2, data.at("/rows/1/id").asInt());
        verify(dao, times(1)).findAllById(any());
    }

    private DataTheme theme(int id, String name) {
        return DataTheme.builder()
                .id(id)
                .name(name)
                .description(name + "描述")
                .content("{\"datasetIds\":[" + id + "],\"relations\":[]}")
                .labels("")
                .roleIds("")
                .status(1)
                .build();
    }
}
