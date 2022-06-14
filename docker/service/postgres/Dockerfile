ARG POSTGRES_VERSION

FROM postgres:${POSTGRES_VERSION}

ADD postgresql.conf /etc/postgresql/postgresql.conf

EXPOSE 5432

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
