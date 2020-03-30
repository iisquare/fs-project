#!/bin/bash
source $(dirname $0)/env.sh
for pjt in ${PROJECT_APP[@]}
do
nohup $JAVA_HOME/bin/java -jar ${PROJECT_ROOT}/${pjt}.jar > ${PROJECT_ROOT}/logs/${pjt}.log &
done
