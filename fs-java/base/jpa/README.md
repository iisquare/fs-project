# jpa

JDBC关系型数据库

## 事物
- 采用MySQL作为主数据存储源，编码时优先保障主数据稳定。
- 避免在实体中配置关联关系，通过手动查询在程序中处理关联数据。
- 尽量避免在单应用中处理多事务，通过逻辑补偿保障数据最终一致性。
- 尽量避免分布式事务场景产生，优先保障单主应用的业务数据稳定性。
- 非关系型数据库避免引入ORM框架，通过自定义事务管理器支持切面注解。

## 参考
- [spring-data-neo4j](https://github.com/spring-projects/spring-data-neo4j/blob/main/src/main/java/org/springframework/data/neo4j/core/transaction/ReactiveNeo4jTransactionManager.java)
- [spring-data-mongodb](https://github.com/spring-projects/spring-data-mongodb/blob/b134e1916d/spring-data-mongodb/src/main/java/org/springframework/data/mongodb/ReactiveMongoTransactionManager.java)
- [Spring Data JPA - Reference Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
