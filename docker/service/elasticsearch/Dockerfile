ARG ELK_VERSION

FROM elasticsearch:${ELK_VERSION}

COPY analysis-ik-online-0.0.1.zip .
RUN unzip analysis-ik-online-0.0.1.zip -d /usr/share/elasticsearch/plugins/analysis-ik-online/
RUN rm analysis-ik-online-0.0.1.zip

ENV discovery.type single-node
ENV http.cors.enabled true
ENV http.cors.allow-origin *

EXPOSE 9200 9300
