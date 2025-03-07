#!/bin/bash
source $(dirname $0)/env.sh
for pjt in ${PROJECT_APP[@]}
do
ps aux|grep "${pjt}.jar"|awk '{print $2}'|xargs kill -s TERM
done
