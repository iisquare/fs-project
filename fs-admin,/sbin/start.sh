#!/bin/bash
source $(dirname $0)/env.sh
for pjt in ${PROJECT_APP[@]}
do
nohup $JAVA_HOME/bin/java -jar ${PROJECT_ROOT}/web/${pjt}/build/libs/${PROJECT_NAME}-web-${pjt}.jar > ${PROJECT_ROOT}/logs/${pjt}.log &
done
