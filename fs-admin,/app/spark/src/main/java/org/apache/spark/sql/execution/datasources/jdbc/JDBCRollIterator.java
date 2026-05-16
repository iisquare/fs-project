package org.apache.spark.sql.execution.datasources.jdbc;

import com.iisquare.fs.app.spark.helper.JDBCHelper;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.Partition;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.Row;
import scala.Function0;
import scala.collection.AbstractIterator;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class JDBCRollIterator extends AbstractIterator<Row> implements Closeable {

    JDBCRollRDD rdd;
    Partition thePart;
    TaskContext context;

    boolean closed = false;
    ResultSet rs = null;
    PreparedStatement stmt = null;
    Connection conn;
    Row latest = null;
    String[] pks = new String[]{"id"};

    public JDBCRollIterator(JDBCRollRDD rdd, Partition thePart, TaskContext context) {
        this.rdd = rdd;
        this.thePart = thePart;
        this.context = context;
        conn = rdd.getConnection.apply(thePart.index());
        String roll = (String) rdd.options.parameters().getOrElse("roll", (Function0<String>) () -> "");
        String[] pks = DPUtil.explode(",", roll);
        if (pks.length > 0) {
            this.pks = pks;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        FileUtil.close(rs, stmt, conn);
        closed = true;
    }

    @Override
    public boolean hasNext() {
        try {
            if (null == rs || !rs.next()) {
                return take();
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Row next() {
        try {
            latest = JDBCHelper.row(rs, rdd.schema);
            return latest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean take() throws SQLException {
        FileUtil.close(rs, stmt);
        StringBuilder sb = new StringBuilder();
        sb.append("select * from ").append(rdd.options.tableOrQuery()).append(" where ");
        sb.append(JDBCHelper.pkWhere(latest, pks));
        sb.append(" order by ").append(DPUtil.implode(", ", pks)).append(" limit ").append(rdd.options.batchSize()).append(";");
        String sql = sb.toString();
        log.debug(sql);
        stmt = conn.prepareStatement(sql);
        stmt.setFetchSize(rdd.options.fetchSize());
        stmt.setQueryTimeout(rdd.options.queryTimeout());
        rs = stmt.executeQuery();
        return rs.next();
    }

}
