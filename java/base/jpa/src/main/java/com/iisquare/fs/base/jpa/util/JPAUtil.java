package com.iisquare.fs.base.jpa.util;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.mvc.DaoBase;
import org.springframework.data.domain.Sort;

import java.util.*;

public class JPAUtil {
    public static final Map<String, Sort.Direction> ORDER_DIRECTION = new LinkedHashMap<>();
    static {
        ORDER_DIRECTION.put("asc", Sort.Direction.ASC);
        ORDER_DIRECTION.put("desc", Sort.Direction.DESC);
    }

    public static <T> T findById(DaoBase dao, Object id, Class<T> classType) {
        Optional<T> info = dao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public static Sort sort(String sort, Collection<String> fields) {
        if (DPUtil.empty(sort)) return null;
        List<Sort.Order> oders = new ArrayList<>();
        String[] sorts = DPUtil.explode(sort, ",", " ", true);
        for (String item : sorts) {
            String[] explode = DPUtil.explode(item, "\\.", " ", true);
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : null;
            if (!ORDER_DIRECTION.containsKey(direction)) direction = null;
            oders.add(new Sort.Order(null == direction ? Sort.DEFAULT_DIRECTION : ORDER_DIRECTION.get(order), order));
        }
        if (oders.size() < 1) return null;
        return Sort.by(oders);
    }
}
