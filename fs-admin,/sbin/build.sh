#!/bin/bash
source $(dirname $0)/env.sh
cd $PROJECT_ROOT
gradle clean
for pjt in ${PROJECT_APP[@]}
do
gradle :web:${pjt}:bootJar
done
cd -
