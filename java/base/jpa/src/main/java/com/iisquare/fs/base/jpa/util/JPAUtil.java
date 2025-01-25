package com.iisquare.fs.base.jpa.util;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
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
        if (DPUtil.empty(sort) || null == fields || fields.size() == 0) return null;
        List<Sort.Order> orders = new ArrayList<>();
        String[] sorts = DPUtil.explode(",", sort);
        for (String item : sorts) {
            String[] explode = DPUtil.explode("\\.", item);
            String order = explode[0];
            if (!fields.contains(order)) continue;
            String direction = explode.length > 1 ? explode[1].toLowerCase() : null;
            if (!ORDER_DIRECTION.containsKey(direction)) direction = null;
            orders.add(new Sort.Order(null == direction ? Sort.DEFAULT_DIRECTION : ORDER_DIRECTION.get(direction), order));
        }
        if (orders.size() < 1) return null;
        return Sort.by(orders);
    }

    public static String tableName(EntityManager manager, Class cls) {
        EntityManagerFactory factory = manager.getEntityManagerFactory();
        MetamodelImplementor meta = (MetamodelImplementor) factory.getMetamodel();
        SingleTableEntityPersister persist = (SingleTableEntityPersister) meta.entityPersister(cls);
        return persist.getTableName();
    }

    public static String driverClassName(EntityManager manager) {
        HikariDataSource dataSource = (HikariDataSource) dataSource(manager);
        return dataSource.getDriverClassName();
    }

    public static DataSource dataSource(EntityManager manager) {
        EntityManagerFactoryInfo info = (EntityManagerFactoryInfo) manager.getEntityManagerFactory();
        return info.getDataSource();
    }

    public static Connection connection(EntityManager manager) {
        return DataSourceUtils.getConnection(dataSource(manager));
    }

    public static void release(EntityManager manager, Connection connection) {
        DataSourceUtils.releaseConnection(connection, dataSource(manager));
    }

}
