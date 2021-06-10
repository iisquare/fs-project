package com.iisquare.fs.base.jpa.util;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.mvc.DaoBase;
import org.springframework.data.domain.Sort;

import java.util.*;

public class JPAUtil {

    public static final Map<String, Sort.Direction> ORDER_DIRECTION = new LinkedHashMap(){{
        put("asc", Sort.Direction.ASC);
        put("desc", Sort.Direction.DESC);
    }};

    public static <T> T findById(DaoBase dao, Object id, Class<T> classType) {
        Optional<T> info = dao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public static Sort sort(String sort, Collection<String> fields) {
        if (DPUtil.empty(sort)) return null;
        List<Sort.Order> orders = new ArrayList<>();
        String[] sorts = DPUtil.explode(sort);
        for (String item : sorts) {
            String[] explode = DPUtil.explode(item, "\\.");
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : null;
            if (!ORDER_DIRECTION.containsKey(direction)) direction = null;
            orders.add(new Sort.Order(null == direction ? Sort.DEFAULT_DIRECTION : ORDER_DIRECTION.get(direction), order));
        }
        if (orders.size() < 1) return null;
        return Sort.by(orders);
    }
}
