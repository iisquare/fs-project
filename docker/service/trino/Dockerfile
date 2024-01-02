ARG TRINO_VERSION

FROM trinodb/trino:${TRINO_VERSION}

ARG TRINO_VERSION

# RUN curl -o /usr/lib/trino/bin/trino-cli "https://repo1.maven.org/maven2/io/trino/trino-cli/${TRINO_VERSION}/trino-cli-${TRINO_VERSION}-executable.jar"
RUN curl -L -o /usr/lib/trino/bin/trino-cli "https://maven.aliyun.com/repository/public/io/trino/trino-cli/${TRINO_VERSION}/trino-cli-${TRINO_VERSION}-executable.jar"
RUN chmod +x /usr/lib/trino/bin/trino-cli

EXPOSE 8080
