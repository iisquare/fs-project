package com.iisquare.fs.app.flink.batch;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SourceSplit;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.core.io.InputStatus;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 有界JDBC数据源（FLIP-27）。
 *
 * Flink 2.x 的 BATCH 运行模式要求数据源声明为 BOUNDED，而官方 flink-connector-jdbc 目前最高只支持
 * Flink 1.20，故以单分片有界Source实现：查询结果由一个Reader流式读取（不整体载入内存）。
 *
 * 适用场景：BI 编排中的中小规模批处理读取；超大数据量建议使用 Spark 批处理。
 */
public class JDBCBoundedSource
        implements Source<Row, JDBCBoundedSource.JDBCSplit, JDBCBoundedSource.JDBCEnumeratorState> {

    private final String url;
    private final String driver;
    private final String username;
    private final String password;
    private final String sql;
    private final int fetchSize;
    private final RowTypeInfo typeInfo;

    public JDBCBoundedSource(String url, String driver, String username, String password,
                             String sql, int fetchSize, RowTypeInfo typeInfo) {
        this.url = url;
        this.driver = driver;
        this.username = username;
        this.password = password;
        this.sql = sql;
        this.fetchSize = fetchSize;
        this.typeInfo = typeInfo;
    }

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.BOUNDED;
    }

    @Override
    public SourceReader<Row, JDBCSplit> createReader(SourceReaderContext readerContext) {
        return new JDBCSourceReader(readerContext);
    }

    @Override
    public SplitEnumerator<JDBCSplit, JDBCEnumeratorState> createEnumerator(
            SplitEnumeratorContext<JDBCSplit> enumContext) {
        return new JDBCEnumerator(enumContext);
    }

    @Override
    public SplitEnumerator<JDBCSplit, JDBCEnumeratorState> restoreEnumerator(
            SplitEnumeratorContext<JDBCSplit> enumContext, JDBCEnumeratorState state) {
        // 批处理不依赖检查点恢复，重建枚举器后重新分配分片即可
        return new JDBCEnumerator(enumContext);
    }

    @Override
    public SimpleVersionedSerializer<JDBCSplit> getSplitSerializer() {
        return JDBCSplit.SERIALIZER;
    }

    @Override
    public SimpleVersionedSerializer<JDBCEnumeratorState> getEnumeratorCheckpointSerializer() {
        return JDBCEnumeratorState.SERIALIZER;
    }


    /**
     * 单分片：整条查询作为一个分片分配给首个Reader
     */
    public static class JDBCSplit implements SourceSplit {

        public static final String SPLIT_ID = "jdbc-single-split";
        public static final SimpleVersionedSerializer<JDBCSplit> SERIALIZER = new SimpleVersionedSerializer<JDBCSplit>() {
            @Override
            public int getVersion() {
                return 1;
            }

            @Override
            public byte[] serialize(JDBCSplit split) {
                return split.splitId().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public JDBCSplit deserialize(int version, byte[] serialized) {
                return new JDBCSplit(new String(serialized, StandardCharsets.UTF_8));
            }
        };

        private final String id;

        public JDBCSplit(String id) {
            this.id = id;
        }

        @Override
        public String splitId() {
            return id;
        }
    }

    public static class JDBCEnumeratorState implements Serializable {

        public static final SimpleVersionedSerializer<JDBCEnumeratorState> SERIALIZER = new SimpleVersionedSerializer<JDBCEnumeratorState>() {
            @Override
            public int getVersion() {
                return 1;
            }

            @Override
            public byte[] serialize(JDBCEnumeratorState state) {
                return new byte[0];
            }

            @Override
            public JDBCEnumeratorState deserialize(int version, byte[] serialized) {
                return new JDBCEnumeratorState();
            }
        };
    }

    /**
     * 分片枚举器：把唯一分片分配给首个注册/请求的Reader
     */
    public static class JDBCEnumerator implements SplitEnumerator<JDBCSplit, JDBCEnumeratorState> {

        private final SplitEnumeratorContext<JDBCSplit> context;
        private boolean assigned = false;

        public JDBCEnumerator(SplitEnumeratorContext<JDBCSplit> context) {
            this.context = context;
        }

        @Override
        public void start() {
        }

        @Override
        public void handleSplitRequest(int subtaskId, String requesterHostname) {
            assign(subtaskId);
        }

        @Override
        public void addReader(int subtaskId) {
            assign(subtaskId);
        }

        private void assign(int subtaskId) {
            if (!assigned) {
                context.assignSplit(new JDBCSplit(JDBCSplit.SPLIT_ID), subtaskId);
                assigned = true;
            }
            context.signalNoMoreSplits(subtaskId);
        }

        @Override
        public void addSplitsBack(List<JDBCSplit> splits, int subtaskId) {
            if (null != splits && !splits.isEmpty()) assigned = false;
        }

        @Override
        public JDBCEnumeratorState snapshotState(long checkpointId) {
            return new JDBCEnumeratorState();
        }

        @Override
        public void close() {
        }
    }


    /**
     * 读取器：首个pollNext时打开查询，逐行读取直至结果集耗尽
     */
    public class JDBCSourceReader implements SourceReader<Row, JDBCSplit> {

        private final SourceReaderContext context;
        private CompletableFuture<Void> availability = new CompletableFuture<>();
        private JDBCSplit split = null;
        private boolean noMoreSplits = false;
        private boolean finished = false;
        private Connection connection = null;
        private PreparedStatement statement = null;
        private ResultSet result = null;

        public JDBCSourceReader(SourceReaderContext context) {
            this.context = context;
        }

        @Override
        public void start() {
            context.sendSplitRequest();
        }

        @Override
        public InputStatus pollNext(ReaderOutput<Row> output) throws Exception {
            if (finished) return InputStatus.END_OF_INPUT;
            if (null == split) {
                if (noMoreSplits) {
                    finished = true;
                    return InputStatus.END_OF_INPUT;
                }
                return InputStatus.NOTHING_AVAILABLE;
            }
            if (null == result) open();
            if (result.next()) {
                TypeInformation<?>[] types = typeInfo.getFieldTypes();
                Row row = new Row(types.length);
                for (int index = 0; index < types.length; index++) {
                    row.setField(index, JDBCSourceNode.value(result, index + 1, types[index]));
                }
                output.collect(row);
                return InputStatus.MORE_AVAILABLE;
            }
            close();
            finished = true;
            return InputStatus.END_OF_INPUT;
        }

        private void open() throws Exception {
            JDBCSourceNode.load(driver);
            connection = DriverManager.getConnection(url, username, password);
            connection.setReadOnly(true);
            statement = connection.prepareStatement(sql);
            if (fetchSize > 0) statement.setFetchSize(fetchSize);
            result = statement.executeQuery();
        }

        @Override
        public List<JDBCSplit> snapshotState(long checkpointId) {
            if (null == split || finished) return Collections.emptyList();
            return Collections.singletonList(split);
        }

        @Override
        public CompletableFuture<Void> isAvailable() {
            if (null == split && !noMoreSplits) return availability;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void addSplits(List<JDBCSplit> splits) {
            if (null == splits || splits.isEmpty()) return;
            this.split = splits.get(0);
            if (availability.isDone()) availability = new CompletableFuture<>();
            availability.complete(null);
        }

        @Override
        public void notifyNoMoreSplits() {
            noMoreSplits = true;
            if (null == split && !availability.isDone()) availability.complete(null);
        }

        @Override
        public void close() throws IOException {
            JDBCSourceNode.close(result, statement, connection);
            result = null;
            statement = null;
            connection = null;
        }
    }

}
