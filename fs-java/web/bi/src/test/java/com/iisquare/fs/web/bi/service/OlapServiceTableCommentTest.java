package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import org.junit.Assert;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OlapServiceTableCommentTest {

    @Test
    public void tablesIncludesRelationComment() throws Exception {
        TrinoService trinoService = mock(TrinoService.class);
        ResultSet tableRows = mock(ResultSet.class);
        when(tableRows.next()).thenReturn(true, false);
        when(tableRows.getString(1)).thenReturn("orders");
        when(tableRows.getString(2)).thenReturn("BASE TABLE");

        ResultSet commentRows = mock(ResultSet.class);
        when(commentRows.next()).thenReturn(true, false);
        when(commentRows.getString(1)).thenReturn("orders");
        when(commentRows.getString(2)).thenReturn("订单主表");

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            TrinoService.ResultSetConsumer consumer = invocation.getArgument(2);
            if (sql.contains("system.metadata.table_comments")) {
                consumer.accept(commentRows);
            } else {
                TrinoService.StatementBinder binder = invocation.getArgument(1);
                binder.bind(mock(PreparedStatement.class));
                consumer.accept(tableRows);
            }
            return null;
        }).when(trinoService).query(
                anyString(),
                any(TrinoService.StatementBinder.class),
                any(TrinoService.ResultSetConsumer.class));

        OlapService service = new OlapService();
        service.trinoService = trinoService;
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("catalog", "iceberg");
        param.put("schema", "sales");

        Map<String, Object> result = service.tables(param);

        Assert.assertTrue(ApiUtil.succeed(result));
        ObjectNode data = ApiUtil.data(result, ObjectNode.class);
        Assert.assertEquals("orders", data.at("/tables/0/name").asText());
        Assert.assertEquals("TABLE", data.at("/tables/0/type").asText());
        Assert.assertEquals("订单主表", data.at("/tables/0/remark").asText());
    }

    @Test
    public void parsesPrimaryKeysFromShowCreateTable() {
        String ddl = """
                CREATE TABLE mysql.sales.orders (
                   tenant_id bigint NOT NULL,
                   order_id varchar(64) NOT NULL
                )
                WITH (
                   primary_key = ARRAY['tenant_id', 'order_id']
                )
                """;

        List<String> pks = OlapService.primaryKeysFromCreate(ddl);

        Assert.assertEquals(List.of("tenant_id", "order_id"), pks);
    }
}
