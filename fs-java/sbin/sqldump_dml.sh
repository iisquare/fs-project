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
OUTPUT_FILE="${OUTPUT_DIR}/fs_project_${DUMP_NAME}_dml.sql"

# dump名称 -> 表名列表，"all"表示导出该模块前缀下的全部表
declare -A DUMP_TABLES
DUMP_TABLES["member"]="fs_member_application fs_member_menu fs_member_resource"
DUMP_TABLES["spider"]="all"

if [ -z "${DUMP_TABLES[$DUMP_NAME]}" ]; then
  echo "没有找到 ${DUMP_NAME} 对应的导出配置。"
  exit 1
fi

if [ "${DUMP_TABLES[$DUMP_NAME]}" = "all" ]; then
  TABLE_PREFIX="fs_${DUMP_NAME}_"
  TABLES=$(mysql -h $DB_HOST -u $DB_USER -p$DB_PASS -D $DB_NAME -e "SHOW TABLES LIKE '${TABLE_PREFIX}%';" | awk '{print $1}' | grep -v '^Tables_in_')
else
  TABLES="${DUMP_TABLES[$DUMP_NAME]}"
fi

if [ -z "$TABLES" ]; then
  echo "没有找到需要导出的表。"
  exit 1
fi

# 仅导出数据，并在导入时先清空目标表
{
    echo "SET FOREIGN_KEY_CHECKS=0;"
    for TABLE in $TABLES; do
        echo "TRUNCATE TABLE \`${TABLE}\`;"
    done
    echo "SET FOREIGN_KEY_CHECKS=1;"
    mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME $TABLES --no-create-info --skip-extended-insert --set-gtid-purged=OFF
} > "$OUTPUT_FILE"

# 检查导出是否成功
if [ $? -eq 0 ]; then
  echo "导出成功，文件保存在: $OUTPUT_FILE"
  echo "sudo docker-compose exec -T mysql mysql -u root -padmin888 ${DB_NAME} < $OUTPUT_FILE"
  echo "生成的备份文件包含截断语句，请谨慎执行！"
else
  echo "导出失败。"
  exit 1
fi
