#!/bin/bash

PROJECT_ROOT=$(dirname $(readlink -f $0))
PROJECT_ROOT=$(dirname $PROJECT_ROOT)
DUMP_NAME=$1

# 数据库配置
DB_HOST="wsl"
DB_USER="root"
DB_PASS="admin888"
DB_NAME="fs_project"

# 导出文件路径
OUTPUT_DIR="${PROJECT_ROOT}/docs"
OUTPUT_FILE="${OUTPUT_DIR}/fs_project_${DUMP_NAME}.sql"

# 表前缀
TABLE_PREFIX="fs_${DUMP_NAME}_"

# 获取所有表名
TABLES=$(mysql -h $DB_HOST -u $DB_USER -p$DB_PASS -D $DB_NAME -e "SHOW TABLES LIKE '${TABLE_PREFIX}%';" | awk '{print $1}' | grep -v '^Tables_in_')

# 检查是否有匹配的表
if [ -z "$TABLES" ]; then
  echo "没有找到以 '${TABLE_PREFIX}' 为前缀的表。"
  exit 1
fi

# 仅导出表结构
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME $TABLES --no-data > $OUTPUT_FILE

# 检查导出是否成功
if [ $? -eq 0 ]; then
  echo "导出成功，文件保存在: $OUTPUT_FILE"
else
  echo "导出失败。"
  exit 1
fi
