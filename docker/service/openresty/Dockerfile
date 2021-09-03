ARG OPENRESTY_VERSION

FROM openresty/openresty:${OPENRESTY_VERSION}

RUN curl -L -o /root/lua-resty-kafka-0.09.tar.gz https://github.com/doujiang24/lua-resty-kafka/archive/refs/tags/v0.09.tar.gz
RUN tar -C /root -xvf /root/lua-resty-kafka-0.09.tar.gz
RUN mv /root/lua-resty-kafka-0.09/lib/resty/kafka/ /usr/local/openresty/lualib/resty/
RUN rm -rf /root/lua-*

RUN sed -i 's/^http {/http {\n    resolver      127.0.0.11;/' /usr/local/openresty/nginx/conf/nginx.conf

EXPOSE 80
