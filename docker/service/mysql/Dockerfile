ARG MYSQL_VERSION

FROM mysql:${MYSQL_VERSION}

ADD mysql.cnf /etc/mysql/conf.d/mysql.cnf
RUN chmod 644 /etc/mysql/conf.d/mysql.cnf

EXPOSE 3306
