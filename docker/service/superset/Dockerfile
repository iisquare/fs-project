ARG SUPERSET_VERSION

FROM apache/superset:${SUPERSET_VERSION}

USER root

RUN pip install pyhive
RUN pip install mysqlclient
RUN pip install psycopg2
RUN pip install trino

USER superset

EXPOSE 8088
