package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.dao.DatasetDao;
import com.iisquare.fs.web.bi.entity.Dataset;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatasetServiceBatchTest {

    @Test
    public void columnsKeepsSingleDatasetContract() {
        DatasetDao dao = mock(DatasetDao.class);
        DatasetService service = new DatasetService();
        service.datasetDao = dao;
        when(dao.findById(1)).thenReturn(Optional.of(dataset(1, "orders", "id")));
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", 1);

        Map<String, Object> result = service.columns(param);

        ObjectNode data = ApiUtil.data(result, ObjectNode.class);
        Assert.assertEquals(1, data.at("/id").asInt());
        Assert.assertEquals("orders", data.at("/name").asText());
        Assert.assertEquals("id", data.at("/columns/0/name").asText());
        Assert.assertFalse(data.has("rows"));
    }

    @Test
    public void batchLoadsDatasetsOnceAndKeepsRequestedOrder() {
        DatasetDao dao = mock(DatasetDao.class);
        DatasetService service = new DatasetService();
        service.datasetDao = dao;
        when(dao.findAllById(any())).thenReturn(List.of(
                dataset(2, "customers", "customer_id"),
                dataset(1, "orders", "id")));
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ids", List.of(1, 2, 1, 3));

        Map<String, Object> result = service.columnsBatch(param);

        Assert.assertTrue(ApiUtil.succeed(result));
        ObjectNode data = ApiUtil.data(result, ObjectNode.class);
        Assert.assertEquals(2, data.at("/total").asInt());
        Assert.assertEquals(1, data.at("/rows/0/id").asInt());
        Assert.assertEquals("orders", data.at("/rows/0/name").asText());
        Assert.assertEquals("id", data.at("/rows/0/columns/0/name").asText());
        Assert.assertEquals(2, data.at("/rows/1/id").asInt());
        Assert.assertEquals("customers", data.at("/rows/1/name").asText());
        verify(dao, times(1)).findAllById(any());
    }

    private Dataset dataset(int id, String name, String field) {
        return Dataset.builder()
                .id(id)
                .name(name)
                .description(name + " description")
                .fields("[{\"name\":\"" + field + "\",\"title\":\"" + field
                        + "\",\"type\":\"string\",\"comment\":\"field\"}]")
                .status(1)
                .build();
    }
}
