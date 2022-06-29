ARG SUPERSET_VERSION

FROM apache/superset:${SUPERSET_VERSION}

USER root

RUN pip install mysqlclient
RUN pip install psycopg2
RUN pip install sqlalchemy-trino

USER superset

EXPOSE 8088
