package com.iisquare.fs.base.core.tool;

import com.iisquare.fs.base.core.util.DPUtil;

import java.util.*;

/**
 * 路径化请求参数
 * 格式说明：
 * - 键值对：key_value
 * - 多参数：k1_v1,k2_v2,k3_v3
 * - 多值：key_v1,key_v2,key_v3
 * - 负数值：key_-371775328
 * - 值区间：keyBegin_valueBegin,keyEnd_valueEnd
 * - 关键词查询：忽略处理，建议直接作为?QueryParam请求参数
 * - 保留符号：英文逗号，下划线
 * - 最佳实践：为提高SEO收录效果，建议参数顺序保持一致；屏蔽爬虫来源的关键词查询；
 */
public class ArgumentBuilder {

    private boolean withInclude = false;
    private boolean withExclude = false;
    private boolean withEmptyField = true;
    private Set<String> includes = new HashSet<>();
    private Set<String> excludes = new HashSet<>();
    private Map<String, String> abbreviation = new LinkedHashMap<>(); // 变量（缩写=>全称）
    private SortedMap<String, SortedSet<String>> args = new TreeMap<>();

    public static ArgumentBuilder newInstance() {
        return new ArgumentBuilder();
    }

    public static ArgumentBuilder newInstance(Map<String, String> abbreviation, String uri) {
        return new ArgumentBuilder().abbreviation(abbreviation).parse(uri);
    }

    public ArgumentBuilder abbreviation(Map<String, String> abbreviation) {
        if (null != abbreviation) this.abbreviation.putAll(abbreviation);
        return this;
    }

    public ArgumentBuilder abbreviation(String brief, String complete) {
        this.abbreviation.put(brief, complete);
        return this;
    }

    public Map<String, String> abbreviation(boolean bRevert) {
        if (!bRevert) return this.abbreviation;
        Map<String, String> abbreviation= new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.abbreviation.entrySet()) {
            abbreviation.put(entry.getValue(), entry.getKey());
        }
        return abbreviation;
    }

    public ArgumentBuilder withInclude(boolean withInclude) {
        this.withInclude = withInclude;
        return this;
    }

    public ArgumentBuilder withExclude(boolean withExclude) {
        this.withExclude = withExclude;
        return this;
    }

    public ArgumentBuilder withEmptyField(boolean withEmptyField) {
        this.withEmptyField = withEmptyField;
        return this;
    }

    public ArgumentBuilder includeAdd(String... parameters) {
        for (String parameter : parameters) {
            includes.add(parameter);
        }
        return this;
    }

    public ArgumentBuilder includeRemove(String... parameters) {
        for (String parameter : parameters) {
            includes.remove(parameter);
        }
        return this;
    }

    public ArgumentBuilder includeRemoveAll() {
        includes = new HashSet<>();
        return this;
    }

    public ArgumentBuilder excludeAdd(String... parameters) {
        for (String parameter : parameters) {
            excludes.add(parameter);
        }
        return this;
    }

    public ArgumentBuilder excludeRemove(String... parameters) {
        for (String parameter : parameters) {
            excludes.remove(parameter);
        }
        return this;
    }

    public ArgumentBuilder excludeRemoveAll() {
        excludes = new HashSet<>();
        return this;
    }

    public ArgumentBuilder add(String parameter, String... values) {
        SortedSet<String> data = args.get(parameter);
        if (null == data) {
            args.put(parameter, data = new TreeSet<>());
        }
        for (String value : values) {
            if (!DPUtil.empty(value)) data.add(value);
        }
        return this;
    }

    public ArgumentBuilder remove(String parameter, String... values) {
        SortedSet<String> data = args.get(parameter);
        if (null == data) return this;
        for (String value : values) {
            data.remove(value);
        }
        return this;
    }

    public ArgumentBuilder delete(String... parameters) {
        for (String parameter : parameters) {
            args.remove(parameter);
        }
        return this;
    }

    public ArgumentBuilder removeAll(String... parameters) {
        for (String parameter : parameters) {
            if (args.containsKey(parameter)) {
                args.put(parameter, new TreeSet<>());
            }
        }
        return this;
    }

    public SortedMap<String, String> arg() {
        SortedMap<String, SortedSet<String>> args = args();
        SortedMap<String, String> arg = new TreeMap<>();
        for (Map.Entry<String, SortedSet<String>> entry : args.entrySet()) {
            SortedSet<String> data = entry.getValue();
            arg.put(entry.getKey(), data.size() > 0 ? data.first() : "");
        }
        return arg;
    }

    public SortedMap<String, SortedSet<String>> args() {
        SortedMap<String, SortedSet<String>> args = new TreeMap<>();
        for (Map.Entry<String, SortedSet<String>> entry : this.args.entrySet()) {
            String key = entry.getKey();
            SortedSet<String> items = entry.getValue();
            if (withInclude && !includes.contains(key)) continue;
            if (withExclude && excludes.contains(key)) continue;
            if (!withEmptyField && items.size() < 1) continue;
            args.put(key, items);
        }
        return args;
    }

    public String uri() {
        SortedMap<String, SortedSet<String>> args = args();
        Map<String, String> abbreviation = this.abbreviation(true);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, SortedSet<String>> entry : args.entrySet()) {
            String key = entry.getKey();
            if (abbreviation.containsKey(key)) key = abbreviation.get(key);
            SortedSet<String> items = entry.getValue();
            for (String item : items) {
                sb.append(",").append(key).append("_").append(item);
            }
        }
        return sb.toString().substring(1);
    }

    public ArgumentBuilder parse(String uri) {
        if (DPUtil.empty(uri)) return this;
        String[] strings = uri.split("");
        String key = "";
        String value = "";
        boolean fielding = true;
        for (String str : strings) {
            switch (str) {
                case ",":
                    if (!fielding) {
                        add(abbreviation.containsKey(key) ? abbreviation.get(key) : key, value);
                    }
                    fielding = true;
                    key = "";
                    value = "";
                    break;
                case "_":
                    fielding = false;
                    value = "";
                    break;
                default:
                    if (fielding) {
                        key += str;
                    } else {
                        value += str;
                    }
            }
        }
        if (!fielding) add(abbreviation.containsKey(key) ? abbreviation.get(key) : key, value);
        return this;
    }

}
