#!/bin/bash

PROJECT_ROOT=$(dirname $(readlink -f $0))
PROJECT_ROOT=$(dirname $PROJECT_ROOT)
ACTION=$1

# 数据库配置
DB_HOST="wsl"
DB_USER="root"
DB_PASS="admin888"
DB_NAME="fs_site"

# 导出文件路径
OUTPUT_DIR="${PROJECT_ROOT}/docs"

case $ACTION in
sql)
# 导出表结构和数据
DUMP_OPT="--add-drop-table"
DUMP_OPT="${DUMP_OPT} --no-data"
OUTPUT_FILE="${OUTPUT_DIR}/fs_site.sql"
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME $TABLES ${DUMP_OPT} > $OUTPUT_FILE
;;
*)
echo "action:"
echo "sql - dump table struct."
;;
esac
