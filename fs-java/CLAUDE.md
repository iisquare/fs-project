# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

fs-java is a large-scale, modular Java enterprise application built with Spring Boot 3.5.4 and Gradle 7.6.5.

## Build System

### Gradle

The project uses Gradle 7.6.5 with a multi-module structure. All subprojects inherit common configuration:

- Java 17 source/target compatibility
- UTF-8 encoding for all source files
- Centralized dependency versions
- Aliyun Maven mirrors for faster builds in China
- Tests are disabled by default

### Key Build Commands

```bash
gradle :web:admin:bootJar
gradle :web:member:bootJar
gradle clean :web:admin:bootJar
gradle :web:admin:dependencies
gradle jars
gradle projects
```

### Build Scripts

- **sbin/build.sh** - Builds admin and member applications
- **sbin/start.sh** - Starts applications using nohup
- **sbin/env.sh** - Sets environment variables

## Running Applications

### Prerequisites

- JDK 17+
- Gradle 7.6.5
- MySQL 8.0+
- Redis
- Optional: MongoDB, Elasticsearch, Neo4j, Kafka, RabbitMQ

### Development (IDE)

1. Open project in IntelliJ IDEA
2. Settings > Build Tools > Gradle > Build and run using: IntelliJ IDEA
3. Settings > Build > Compiler > Annotation Processors > Enable annotation processing
4. Install Lombok plugin
5. Run application class directly

### Command Line

```bash
java -jar web/admin/build/libs/fs-project-web-admin.jar
java -jar web/admin/build/libs/fs-project-web-admin.jar --server.port=8080
java -jar web/admin/build/libs/fs-project-web-admin.jar --spring.profiles.active=dev,idev
```

### Configuration

Applications use Spring Boot profiles:
- application.yml - Base configuration
- application-dev.yml - Development overrides
- application-prod.yml - Production overrides
- application-test.yml - Test overrides

## Testing

### Test Framework

- JUnit 4 (defined in base:test module)
- Mock-based unit testing recommended
- Tests are disabled by default

### Running Tests

Enable tests in build.gradle:
```gradle
test {
    enabled = true
}
```

Then run:
```bash
gradle test
gradle :web:admin:test
gradle :web:admin:test --tests com.iisquare.fs.web.admin.controller.IndexControllerTest
gradle :web:admin:test --tests com.iisquare.fs.web.admin.controller.IndexControllerTest.testIndex
```

## Architecture

### Base Modules

**Core Infrastructure:**
- base:core - Core utilities (Jackson, Commons)
- base:web - Web MVC base classes (ControllerBase, ServiceBase, SSE)
- base:jpa - JPA/Hibernate base (DaoBase, JPAServiceBase, Druid)
- base:test - JUnit 4 test utilities

**Integration Modules:**
- base:swagger - Swagger/SpringFox
- base:redis - Redis (Jedis client)
- base:view - View layer utilities
- base:worker - Worker/job processing
- base:dag - DAG support
- base:elasticsearch - Elasticsearch
- base:mongodb - MongoDB
- base:neo4j - Neo4j
- base:calcite - Apache Calcite SQL parser
- base:minio - MinIO object storage
- base:zookeeper - ZooKeeper
- base:jsoup - HTML parsing

### Web Layer Architecture

**Shared Infrastructure (web:core):**
- Feign/RPC clients with OpenFeign and Resilience4j
- RBAC with Permission annotation and PermitInterceptor
- Custom FeignInterceptor
- Auto-fallback mechanism

**Base Classes:**
- ControllerBase - Base controller
- ServiceBase - Base service
- PermitControllerBase - Controller with RBAC
- RpcControllerBase - RPC endpoints
- RpcBase - Base RPC client
- FallbackBase / FallbackFactoryBase - Feign fallback handlers

**Data Layer:**
- DaoBase - Base DAO for JPA
- JPAServiceBase - Service base for JPA
- SpecificationHelper - JPA Specification builder
- SQLHelper - Raw SQL with named parameters

### Dependency Injection Pattern

All applications use custom BeanNameGenerator:

```java
new SpringApplicationBuilder(AdminApplication.class)
    .beanNameGenerator(new BeanNameGenerator())
    .run(args);
```

Component scanning configured per application with specific base packages.

### Database

- Druid connection pooling
- JPA/Hibernate ORM
- Custom naming strategies
- Raw SQL via SQLHelper with named parameters
- SQL schemas in docs/fs-project-*.sql

### Redis

- Jedis client (not Lettuce)
- Spring Session for distributed sessions
- RedisKey constants for cache keys

### Big Data

- app:spark - Spark batch/streaming (Spark 3.3.3, Scala 2.12)
- app:flink - Flink streaming (Flink 1.17.1)
- base:dag - DAG support
- Connectors: MongoDB Spark, Elasticsearch Spark, Delta Lake, Hudi, Parquet, ClickHouse

## Module Dependencies

Key relationships:

```
web:admin
  └── web:core
      ├── base:web
      │   ├── base:core
      │   └── spring-boot-starter-web
      ├── base:jpa
      │   ├── base:web
      │   └── spring-boot-starter-data-jpa
      └── spring-cloud-starter-openfeign

web:member
  ├── web:core
  ├── base:jpa
  ├── base:redis
  └── spring-session-data-redis

app:spark
  ├── base:dag
  ├── spark-core, spark-sql, spark-streaming
  ├── mongo-spark-connector
  ├── elasticsearch-spark
  └── delta-core

app:flink
  ├── base:dag
  ├── flink-core, flink-streaming
  └── flink-cdc-connectors
```

## Troubleshooting

**Gradle build fails:**
- Clear cache: rm -rf ~/.gradle/caches
- Rebuild: gradle clean build

**Annotation processing not working:**
- Enable in IDE: Settings > Build > Compiler > Annotation Processors
- Rebuild: Build > Rebuild Project

**Feign clients not found:**
- Ensure @EnableFeignClients in Application class
- Check basePackages matches RPC interface location
- Verify RPC interfaces in rpc package

**Database connection issues:**
- Check MySQL running and accessible
- Verify application-dev.yml database URL
- Check Druid config in base:jpa

**Redis connection issues:**
- Check Redis running (localhost:6379)
- Verify Jedis configuration
- Check RedisKey constants

## Notes

- Project uses Chinese naming in some areas
- Multiple database backends: MySQL, MongoDB, Elasticsearch, Neo4j
- Big data via Spark and Flink
- Micrometer/Prometheus monitoring
- Feign with circuit breaking
- SSE support via SsePlainEmitter, SsePlainEventBuilder, SsePlainRequestPool
