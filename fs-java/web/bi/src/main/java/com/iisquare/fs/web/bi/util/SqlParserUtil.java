package com.iisquare.fs.web.bi.util;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 JSqlParser 的SQL解析工具。
 */
public final class SqlParserUtil {

    private SqlParserUtil() {
    }

    public static Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    /**
     * 解析SQL并返回其中包含的全部表名。
     * 表名去除引号，未加引号的标识符统一转为小写；带catalog或schema的表名以英文点号分隔。
     */
    public static List<String> tableNames(String sql) throws JSQLParserException {
        return tableNames(parse(sql));
    }

    /**
     * 返回SQL语句中包含的全部表名，见 {@link #tableNames(String)}。
     */
    public static List<String> tableNames(Statement statement) {
        TableNameFinder finder = new TableNameFinder();
        finder.getTables(statement);
        return new ArrayList<>(finder.names);
    }

    private static class TableNameFinder extends TablesNamesFinder<Void> {
        private final Set<String> aliases = new HashSet<>();
        private final Set<String> names = new LinkedHashSet<>();

        @Override
        public <S> Void visit(WithItem<?> withItem, S context) {
            aliases.add(withItem.getAlias().getName());
            return super.visit(withItem, context);
        }

        @Override
        public <S> Void visit(ParenthesedSelect select, S context) {
            if (null != select.getAlias()) {
                aliases.add(select.getAlias().getName());
            }
            return super.visit(select, context);
        }

        @Override
        public <S> Void visit(LateralSubSelect lateralSubSelect, S context) {
            if (null != lateralSubSelect.getAlias()) {
                aliases.add(lateralSubSelect.getAlias().getName());
            }
            return super.visit(lateralSubSelect, context);
        }

        @Override
        public <S> Void visit(Table table, S context) {
            if (!aliases.contains(table.getFullyQualifiedName())) {
                names.add(fullName(table));
            }
            return null;
        }
    }

    private static String fullName(Table table) {
        String name = canonicalPart(table.getName(), table.getUnquotedName());
        String schema = table.getSchemaName();
        if (null != schema) {
            name = canonicalPart(schema, table.getUnquotedSchemaName()) + "." + name;
        }
        String catalog = table.getCatalogName();
        if (null != catalog) {
            name = canonicalPart(catalog, table.getUnquotedCatalogName()) + "." + name;
        }
        return name;
    }

    private static String canonicalPart(String raw, String unquoted) {
        if (raw.equals(unquoted)) {
            return unquoted.toLowerCase(Locale.ENGLISH);
        }
        return unquoted;
    }

}
