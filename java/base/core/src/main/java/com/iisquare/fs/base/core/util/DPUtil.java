package com.iisquare.fs.base.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataProcess数据处理类
 */
public class DPUtil {

    public static final String regexLong = "^-?\\d+";
    public static final String regexDouble = "^-?\\d+(\\.\\d+)*";
    public static final String regexSafeImplode = "^[\\w_]+$";
    public static final ObjectMapper mapper = new ObjectMapper();


    /**
     * 获取随机整数字符串，最长为16位
     */
    public static String random(int length) {
        if (length > 16) length = 16;
        String str = Math.random() + "";
        return str.substring(str.length() - length);
    }

    public static int random(int min, int max) {
        return (int) (min + Math.floor(Math.random() * (max - min + 1)));
    }

    public static boolean empty(Object object) {
        if (null == object) return true;
        if (object instanceof Boolean) {
            return !(Boolean) object;
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).isEmpty();
        }
        if (object.getClass().isArray()) {
            return 0 == Array.getLength(object);
        }
        String str = object.toString();
        if (str.length() < 1) return true;
        return false;
    }

    public static boolean parseBoolean(Object object) {
        if (empty(object)) return false;
        String str = object.toString().toLowerCase();
        if ("1".equals(str)) return true;
        if ("true".equals(str)) return true;
        return false;
    }

    /**
     * 转换为int类型
     */
    public static int parseInt(Object object) {
        Integer result = parseInt(object, null);
        return null == result ? 0 : result;
    }

    public static Integer parseInt(Object object, Integer defaultValue) {
        if (null == object) return defaultValue;
        String str = object.toString();
        if ("".equals(str)) return defaultValue;
        str = firstMatcher(regexLong, str);
        if (null == str) return defaultValue;
        return Integer.parseInt(str);
    }

    public static List<Integer> parseIntList(Object object) {
        List<Integer> result = new ArrayList<>();
        if (null == object) return result;
        if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;
            for (Object item : collection) {
                result.add(parseInt(item));
            }
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.add(parseInt(entry.getValue()));
            }
        } else if (object.getClass().isArray()) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                result.add(parseInt(item));
            }
        } else {
            result.add(parseInt(object));
        }
        return result;
    }

    public static List<String> parseStringList(Object object) {
        List<String> result = new ArrayList<>();
        if (null == object) return result;
        if (object instanceof Collection) {
            Collection collection = (Collection) object;
            for (Object item : collection) {
                result.add(parseString(item));
            }
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry entry : map.entrySet()) {
                result.add(parseString(entry.getValue()));
            }
        } else if (object.getClass().isArray()) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                result.add(parseString(item));
            }
        } else {
            result.add(object.toString());
        }
        return result;
    }

    /**
     * 转换为long类型
     */
    public static long parseLong(Object object) {
        Long result = parseLong(object, null);
        return null == result ? 0L : result;
    }

    public static Long parseLong(Object object, Long defaultValue) {
        if (null == object) return defaultValue;
        String str = object.toString();
        if ("".equals(str)) return defaultValue;
        str = firstMatcher(regexLong, str);
        if (null == str) return defaultValue;
        return Long.parseLong(str);
    }

    /**
     * 转换为double类型
     */
    public static double parseDouble(Object object) {
        Double result = parseDouble(object, null);
        return null == result ? 0.0 : result;
    }

    public static Double parseDouble(Object object, Double defaultValue) {
        if (null == object) return defaultValue;
        String str = object.toString();
        if ("".equals(str)) return defaultValue;
        str = firstMatcher(regexDouble, str);
        if (null == str) return defaultValue;
        return Double.parseDouble(str);
    }

    /**
     * 转换为float类型
     */
    public static float parseFloat(Object object) {
        Float result = parseFloat(object, null);
        return null == result ? 0.0f : result;
    }

    public static Float parseFloat(Object object, Float defaultValue) {
        if (null == object) return defaultValue;
        String str = object.toString();
        if ("".equals(str)) return defaultValue;
        str = firstMatcher(regexDouble, str);
        if (null == str) return defaultValue;
        return Float.parseFloat(str);
    }

    /**
     * 转换为String类型
     */
    public static String parseString(Object object) {
        if (null == object) return "";
        return String.valueOf(object);
    }

    /**
     * 比较两个对象是否相等
     */
    public static boolean equals(Object object1, Object object2) {
        if (null == object1) {
            if (null == object2) return true;
        } else {
            return object1.equals(object2);
        }
        return false;
    }

    /**
     * 毫秒转换为格式化日期
     */
    public static String millis2dateTime(long millis, String format) {
        if (empty(millis)) return null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date(millis));
    }

    /**
     * 格式化日期转换为毫秒
     */
    public static long dateTime2millis(Object dateTime, String format) {
        if (DPUtil.empty(dateTime)) return 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        try {
            return dateFormat.parse(parseString(dateTime)).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    /**
     * 格式化日期转换为毫秒
     *
     * @param dateTime    日期
     * @param format      日期格式
     * @param defaultDays 默认值，距当前时间天数
     * @return 毫秒时间戳
     */
    public static long dateTime2millis(Object dateTime, String format, int defaultDays) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        try {
            return dateFormat.parse(parseString(dateTime)).getTime();
        } catch (ParseException e) {
            format = "yyyy-MM-dd";
            return dateTime2millis(dateTime(format), format) + 86400000L * defaultDays;
        }
    }

    /**
     * 获取当前日期
     */
    public static String dateTime(String format) {
        return millis2dateTime(System.currentTimeMillis(), format);
    }

    /**
     * 获取当前秒数
     */
    public static int seconds() {
        return (int) System.currentTimeMillis() / 1000;
    }


    /**
     * 获取正则匹配字符串
     *
     * @param regex  正则表达式
     * @param str    匹配字符串
     * @param bGroup 将捕获组作为结果返回
     * @return
     */
    public static List<String> matcher(String regex, String str, boolean bGroup) {
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            if (bGroup) {
                int count = matcher.groupCount();
                for (int i = 0; i <= count; i++) {
                    list.add(matcher.group(i));
                }
            } else {
                list.add(matcher.group());
            }
        }
        return list;
    }

    /**
     * 获取第一个匹配的字符串
     */
    public static String firstMatcher(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 判断字符串是否与表达式匹配
     */
    public static boolean isMatcher(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    public static String trim(String str) {
        return trim(str, " ");
    }

    /**
     * 去除字符串两边的指定字符
     *
     * @param str     源字符串
     * @param trimStr 需要去除的字符
     * @return
     */
    public static String trim(String str, String trimStr) {
        if (null == str) return "";
        String regexLeft = "^(" + trimStr + ")*";
        str = str.replaceFirst(regexLeft, "");
        String regexRight = "(" + trimStr + ")*$";
        str = str.replaceFirst(regexRight, "");
        return str;
    }

    public static String trimLeft(String str) {
        return trimLeft(str, "");
    }

    /**
     * 去除字符串左边的指定字符
     *
     * @param str
     * @param trimStr
     * @return
     */
    public static String trimLeft(String str, String trimStr) {
        if (null == str) return "";
        String regexLeft = "^(" + trimStr + ")*";
        str = str.replaceFirst(regexLeft, "");
        return str;
    }

    public static String trimRight(String str) {
        return trimRight(str, "");
    }

    /**
     * 去除字符串右边的指定字符
     *
     * @param str
     * @param trimStr
     * @return
     */
    public static String trimRight(String str, String trimStr) {
        if (null == str) return "";
        String regexRight = "(" + trimStr + ")*$";
        str = str.replaceFirst(regexRight, "");
        return str;
    }

    /**
     * 采用指定表达式分隔字符串
     *
     * @param separator 表达式
     * @param string     带分割字符串
     * @param trimStr    对子项进行trim操作
     * @return 分隔后的字符串数组
     */
    public static String[] explode(String separator, String string, String trimStr, boolean filterEmpty) {
        List<String> list = new ArrayList<>(0);
        if (empty(string)) {
            return new String[]{};
        }
        if ("\\".equals(separator)) separator = "\\\\";
        for (String str : string.split(separator, -1)) {
            if (filterEmpty && empty(str)) continue;
            if (null != trimStr) {
                list.add(DPUtil.trim(str));
            } else {
                list.add(str);
            }
        }
        return toArray(String.class, list);
    }

    public static String[] explode(String separator, String string) {
        return explode(separator, string, " ", true);
    }

    public static String implode(String separator, Object[] array) {
        if (null == array) return "";
        return implode(separator, array, 0, array.length);
    }

    public static String implode(String separator, Object[] array, int start, int end) {
        if (null == array) return "";
        int size = array.length;
        if (1 > size) return "";
        size = Math.min(size, end);
        start = Math.max(0, start);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < size; i++) {
            Object value = array[i];
            if (null == value) continue;
            if (value instanceof Collection) {
                Object[] objects = toArray(Object.class, (Collection<Object>) value);
                sb.append(implode(separator, objects, 0, objects.length));
            } else if (value instanceof Map) {
                Object[] objects = toArray(Object.class, ((Map<Object, Object>) value).values());
                sb.append(implode(separator, objects, 0, objects.length));
            } else {
                sb.append(value);
            }
            if (i + 1 < size) sb.append(separator);
        }
        return sb.toString();
    }

    public static String implode(String separator, Collection<?> list) {
        if (null == list) return "";
        return implode(separator, list.toArray(new Object[0]));
    }

    public static String implode(String separator, Collection<?> list, int start, int end) {
        if (null == list) return "";
        return implode(separator, list.toArray(new Object[0]), start, end);
    }

    /**
     * 过滤数组
     */
    public static String[] filterArray(Object[] array, String wrap, boolean bTrim, boolean bEmpty, boolean bSafe, boolean bDuplicate) {
        if (empty(array)) return new String[]{};
        List<String> list = new ArrayList<>();
        for (Object object : array) {
            String str = parseString(object);
            if (bTrim) str = trim(str);
            if (bEmpty && empty(str)) continue;
            if (bSafe && null == ValidateUtil.filterRegex(regexSafeImplode, str, bTrim, 0, null, null)) continue;
            if (bDuplicate && list.contains(str)) continue;
            list.add(null == wrap ? str : (wrap + str + wrap));
        }
        return toArray(String.class, list);
    }

    public static JsonNode filterByKey(JsonNode json, boolean in2exclude, String... filters) {
        if (filters.length == 0) return json;
        ObjectNode result = DPUtil.objectNode();
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            for (String filter : filters) {
                if (key.matches(filter) == in2exclude) {
                    result.replace(key, value);
                    break;
                }
            }
        }
        return result;
    }

    public static <T> ArrayList<T> array2list(T[] array) {
        if (null == array) return new ArrayList<>(0);
        return new ArrayList<>(Arrays.asList(array));
    }

    public static <T> T[] toArray(Class<T> classType, Collection<T> collection) {
        if (null == collection) array(classType, 0);
        return collection.toArray(array(classType, 0));
    }

    public static <T> T[] array(Class<T> classType, int length) {
        return (T[]) Array.newInstance(classType, length);
    }

    /**
     * 将String数组转换为Integer数组
     */
    public static Integer[] integerArray(Object[] array) {
        Integer[] intArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = DPUtil.parseInt(array[i]);
        }
        return intArray;
    }

    /**
     * 将List转换为Set
     */
    public static <T> Set<T> list2set(List<T> list) {
        Set<T> set = new LinkedHashSet<>(0);
        set.addAll(list);
        return set;
    }

    /**
     * 将Set转换为List
     */
    public static <T> List<T> set2list(Set<T> set) {
        List<T> list = new ArrayList<T>(0);
        list.addAll(set);
        return list;
    }

    /**
     * 将字符串首字母小写
     */
    public static String lowerCaseFirst(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * 将字符串首字母大写
     */
    public static String upperCaseFirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 将大写字母转换为下划线加小写字母的形式
     */
    public static String addUnderscores(String name) {
        StringBuilder buf = new StringBuilder(name.replace('.', '_'));
        for (int i = 1; i < buf.length() - 1; i++) { // 此处需要实时获取长度
            if (
                    Character.isLowerCase(buf.charAt(i - 1)) &&
                            Character.isUpperCase(buf.charAt(i)) &&
                            Character.isLowerCase(buf.charAt(i + 1))
            ) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase();
    }

    /**
     * 将下划线加小写字母转换为驼峰形式
     */
    public static String upUnderscores(String name) {
        StringBuilder buf = new StringBuilder(name);
        int length = buf.length();
        for (int i = 1; i < length; i++) {
            if ('_' == buf.charAt(i - 1)) {
                buf.replace(i, i + 1, String.valueOf(buf.charAt(i)).toUpperCase());
            }
        }
        return buf.toString().replaceAll("_", "");
    }

    /**
     * 判断下标是否在数组范围内
     */
    public static boolean isIndexExist(Object[] array, int index) {
        if (null == array) return false;
        if (index < 0) return false;
        return array.length > index;
    }

    /**
     * 判断下标是否在集合范围内
     */
    public static boolean isIndexExist(Collection<?> collection, int index) {
        if (null == collection) return false;
        if (index < 0) return false;
        return collection.size() > index;
    }

    /**
     * 判断元素是否包含在数组中
     */
    public static boolean isItemExist(Object[] array, Object item) {
        if (null == array) return false;
        for (Object object : array) {
            if (DPUtil.equals(item, object)) return true;
        }
        return false;
    }

    /**
     * 安全获取数组中对应下标的值
     */
    public static Object byIndex(Object[] array, int index) {
        if (isIndexExist(array, index)) return array[index];
        return null;
    }

    /**
     * 安全获取集合中对应下标的值
     */
    public static Object byIndex(Collection<?> collection, int index) {
        if (isIndexExist(collection, index)) {
            Iterator<?> iterator = collection.iterator();
            for (int i = 0; i < index; i++) {
                iterator.next();
            }
            return iterator.next();
        }
        return null;
    }

    /**
     * 合并多个数组
     */
    public static <T> T[] merge(Class<T> classType, T[]... arrays) {
        List<T> list = new ArrayList<>();
        for (T[] array : arrays) {
            if (null == array) continue;
            list.addAll(Arrays.asList(array));
        }
        return toArray(classType, list);
    }

    public static <T> T[] push(Class<T> classType, T[] array, T item) {
        if (null == array) return null;
        T[] result = array(classType, array.length + 1);
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        result[array.length] = item;
        return result;
    }

    public static <T> T[] pop(Class<T> classType, T[] array) {
        if (null == array) return null;
        if (array.length < 1) return array(classType, 0);
        int length = array.length - 1;
        T[] result = array(classType, length);
        for (int i = 0; i < length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static <T> T[] unshift(Class<T> classType, T[] array, T item) {
        if (null == array) return null;
        T[] result = array(classType, array.length + 1);
        result[0] = item;
        for (int i = 0; i < array.length; i++) {
            result[i + 1] = array[i];
        }
        return result;
    }

    public static <T> T[] shift(Class<T> classType, T[] array) {
        if (null == array) return null;
        if (array.length < 1) return array(classType, 0);
        T[] result = array(classType, array.length - 1);
        for (int i = 1; i < array.length; i++) {
            result[i - 1] = array[i];
        }
        return result;
    }

    /**
     * 安全截取字符串，正向从下标0开始，逆向从-1开始
     */
    public static String substring(String str, int start) {
        if (null == str) return "";
        int length = str.length();
        if (start < 0) start += length; // 转换开始下标到正向位置
        if (start < 0) start = 0; // 处理开始下标正向最小范围溢出
        if (start > length - 1) return ""; // 处理开始下标正向最大范围溢出
        return str.substring(start);
    }

    /**
     * 安全截取字符串，正向从下标0开始，逆向从-1开始
     */
    public static String substring(String str, int start, int size) {
        if (null == str) return "";
        int length = str.length();
        if (start < 0) start += length; // 转换开始下标到正向位置
        int end = start + size; // 转换结束下标到正向位置
        if (end < start) {
            int temp = start;
            start = end;
            end = temp;
        }
        if (start < 0) start = 0; // 处理开始下标正向最小范围溢出
        if (start > length - 1) return ""; // 处理开始下标正向最大范围溢出
        if (end < 0) return ""; // 处理结束下标正向最小范围溢出
        if (end > length) end = length; // 处理结束下标正向最大范围溢出
        /* beginIndex - 起始索引（包括），endIndex - 结束索引（不包括） */
        return str.substring(start, end);
    }

    /**
     * 获取初始化填充数组
     */
    public static <T> T[] fillArray(T object, int length) {
        T[] array = (T[]) DPUtil.array(object.getClass(), length);
        Arrays.fill(array, object);
        return array;
    }

    /**
     * 截取List
     *
     * @param list   待截取List
     * @param start  开始位置
     * @param length 截取长度
     * @return 截取List
     */
    public static <T> List<T> sublist(List<T> list, int start, int length) {
        List<T> result = new ArrayList<>();
        int size = list.size();
        int end = Math.min(size, start + length);
        for (int index = start; index < end; index++) {
            result.add(list.get(index));
        }
        return result;
    }

    public static ArrayNode subarray(JsonNode array, int start, int length) {
        ArrayNode result = DPUtil.arrayNode();
        int size = array.size();
        int end = Math.min(size, start + length);
        for (int index = start; index < end; index++) {
            result.add(array.get(index));
        }
        return result;
    }

    /**
     * 创建HashMap
     */
    public static Map<Object, Object> buildMap(Object[] keyArray, Object[] valueArray) {
        int length = keyArray.length;
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        for (int i = 0; i < length; i++) {
            map.put(keyArray[i], valueArray[i]);
        }
        return map;
    }

    /**
     * 创建HashMap
     */
    public static <K, V> Map<K, V> buildMap(K[] keyArray, V[] valueArray, Class<K> kType, Class<V> vType) {
        int length = keyArray.length;
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            map.put(keyArray[i], valueArray[i]);
        }
        return map;
    }

    public static Map<Object, Object> buildMap(Object... kvs) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put(kvs[i], kvs[i + 1]);
        }
        return map;
    }

    public static <K, V> Map<K, V> buildMap(Class<K> kType, Class<V> vType, Object... kvs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((K) kvs[i], (V) kvs[i + 1]);
        }
        return map;
    }

    public static String[] suffix(String[] array, String suffix) {
        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] + suffix;
        }
        return result;
    }

    public static String stringify(Object object) {
        if (null == object) return "";
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    public static ArrayNode arrayNode() {
        return mapper.createArrayNode();
    }

    public static ArrayNode arrayNode(JsonNode nodes) {
        if (nodes.isArray()) return (ArrayNode) nodes;
        ArrayNode array = arrayNode();
        Iterator<JsonNode> iterator = nodes.elements();
        while (iterator.hasNext()) {
            array.add(iterator.next());
        }
        return array;
    }

    public static ObjectNode array2object(JsonNode array, String field) {
        ObjectNode nodes = DPUtil.objectNode();
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String key = node.at("/" + field).asText("");
            nodes.replace(key, node);
        }
        return nodes;
    }

    public static Map<String, Integer> indexes(ArrayNode array, String field) {
        Map<String, Integer> map = new LinkedHashMap<>();
        int index = 0;
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String key = node.at("/" + field).asText("");
            map.put(key, index++);
        }
        return map;
    }

    public static int remove(ArrayNode array, List<Integer> indexes) {
        Collections.sort(indexes, Collections.reverseOrder());
        int total = 0;
        for (Integer index : indexes) {
            if (array.has(index)) array.remove(index);
        }
        return total;
    }

    public static List<String> fields(JsonNode json) {
        List<String> list = new ArrayList<>();
        if (null == json || !json.isObject()) return list;
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        while (iterator.hasNext()) {
            list.add(iterator.next().getKey());
        }
        return list;
    }

    public static <T> List<T> formatRelation(List<?> data, Class<T> requiredType, String parentKey, Object parentValue, String idKey, String childrenKey) {
        List<T> list = new ArrayList<>();
        for (Object item : data) {
            if (!parentValue.equals(ReflectUtil.getPropertyValue(item, parentKey))) continue;
            list.add((T) item);
            List<T> children = formatRelation(data, requiredType, parentKey, ReflectUtil.getPropertyValue(item, idKey), idKey, childrenKey);
            if (children.size() < 1) continue;
            ReflectUtil.setPropertyValue(item, childrenKey, new Class[]{List.class}, new Object[]{children});
        }
        return list;
    }

    public static <T> List<T> fillValues(List<T> list, String[] properties, String suffix, Map<?, ?> map) {
        return fillValues(list, properties, suffix(properties, suffix), fillArray(map, properties.length));
    }

    public static <T> List<T> fillValues(List<T> list, String[] froms, String[] tos, Map<?, ?>... maps) {
        if (null == list) return null;
        for (Object item : list) {
            for (int i = 0; i < froms.length; i++) {
                ReflectUtil.setPropertyValue(item, tos[i], null, new Object[]{
                        maps[i].get(ReflectUtil.getPropertyValue(item, froms[i]))
                });
            }
        }
        return list;
    }

    /**
     * 获取对应字段的值列表
     */
    public static <T> Set<T> values(Collection<?> list, Class<T> tClass, String... properties) {
        Set<T> valueList = new LinkedHashSet<>();
        if (null == list || list.size() < 1 || properties.length < 1) return valueList;
        for (Object object : list) {
            for (String property : properties) {
                Object value = ReflectUtil.getPropertyValue(object, property);
                if (null == value) continue;
                valueList.add((T) value);
            }
        }
        return valueList;
    }

    /**
     * 获取键值对映射
     */
    public static <K, V> Map<K, V> values(Map<K, ?> map, Class<V> tClass, String property) {
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, ?> entry : map.entrySet()) {
            Object value = ReflectUtil.getPropertyValue(entry.getValue(), property);
            result.put(entry.getKey(), (V) value);
        }
        return result;
    }

    public static <T> Set<T> values(JsonNode array, Class<T> tClass, String... properties) {
        Set<T> valueList = new LinkedHashSet<>();
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            for (String property : properties) {
                if (!node.has(property)) continue;
                T value = DPUtil.toJSON(node.get(property), tClass);
                if (null == value) continue;
                valueList.add(value);
            }
        }
        return valueList;
    }

    public static ArrayNode fillValues(ArrayNode array, String[] froms, String[] tos, JsonNode... nodes) {
        return fillValues(array, false, froms, tos, nodes);
    }

    public static ArrayNode fillValues(ArrayNode array, boolean withEmptyObject, String[] froms, String[] tos, JsonNode... nodes) {
        if (null == array) return null;
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            ObjectNode item = (ObjectNode) iterator.next();
            for (int i = 0; i < froms.length; i++) {
                JsonNode node = nodes[i].at("/" + item.at("/" + froms[i]).asText());
                if (withEmptyObject && !node.isObject()) node = DPUtil.objectNode();
                item.replace(tos[i], node);
            }
        }
        return array;
    }

    /**
     * 将List数据格式化为以对应字段值为下标的Map
     */
    public static <K, V> Map<K, V> list2map(List<?> list, Class<K> kType, Class<V> vType, String property) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Object item : list) {
            map.put((K) ReflectUtil.getPropertyValue(item, property), (V) item);
        }
        return map;
    }

    public static <K, V> Map<K, V> list2map(List<V> list, Class<K> kType, String property) {
        Map<K, V> map = new LinkedHashMap<>();
        for (V item : list) {
            map.put((K) ReflectUtil.getPropertyValue(item, property), item);
        }
        return map;
    }

    public static <K1, K2, V> Map<K1, Map<K2, V>> list2mm(List<V> list, Class<K1> k1Type, String k1, Class<K2> k2Type, String k2) {
        Map<K1, Map<K2, V>> map = new LinkedHashMap<>();
        for (V item : list) {
            K1 key1 = (K1) ReflectUtil.getPropertyValue(item, k1);
            K2 key2 = (K2) ReflectUtil.getPropertyValue(item, k2);
            map.computeIfAbsent(key1, k -> new LinkedHashMap<>()).put(key2, item);
        }
        return map;
    }

    public static <K, V> Map<K, List<V>> list2ml(List<V> list, Class<K> kType, String property) {
        Map<K, List<V>> map = new LinkedHashMap<>();
        for (V item : list) {
            K key = (K) ReflectUtil.getPropertyValue(item, property);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    /**
     * 解析JSON字符串
     */
    public static JsonNode parseJSON(String json) {
        if (null == json) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T toJSON(Object obj, Class<T> classType) {
        return mapper.convertValue(obj, classType);
    }

    /**
     * 默认为JsonNode以兼容数组和对象
     */
    public static JsonNode toJSON(Object obj) {
        return mapper.convertValue(obj, JsonNode.class);
    }

    public static JsonNode value(JsonNode json, String path) {
        String[] paths = DPUtil.explode("\\.",path, null, false);
        return value(json, new LinkedList<>(Arrays.asList(paths)));
    }

    public static JsonNode value(JsonNode json, LinkedList<String> paths) {
        if (null == json) return toJSON(null);
        if (json.isNull() || paths.size() < 1) return json;
        return value(json.at("/" + paths.poll()), paths);
    }

    public static boolean value(JsonNode json, String path, JsonNode value) {
        String[] paths = DPUtil.explode("\\.",path, null, false);
        return value(json, new LinkedList<>(Arrays.asList(paths)), value);
    }

    public static boolean value(JsonNode json, LinkedList<String> paths, JsonNode value) {
        if (null == json || json.isNull() || paths.size() < 1) return false;
        String field = paths.poll();
        JsonNode node = json.at("/" + field);
        if (paths.size() > 0) return value(node, paths, value);
        if (json.isObject()) {
            ((ObjectNode) json).replace(field, value);
            return true;
        }
        if (json.isArray()) {
            if (!field.matches("\\d+")) return false;
            ((ArrayNode) json).set(Integer.valueOf(field), value);
        }
        return false;
    }

}
