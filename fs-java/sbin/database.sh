#!/bin/bash

DB_USER=root
DB_PASS=admin888
DB_NAME=fs-project
DB_HOST=127.0.0.1
DB_PORT=3306
DB_TABLE=fs_im_message
DB_SQL='

'

declare -A DB_TABLES=()
DB_TABLES[messfs_im_messageage]=3

if [ -n "$DB_TABLE" ]; then
  SIZE=${DB_TABLES[$DB_TABLE]}
  DB_TABLES=([$DB_TABLE]=${SIZE})
fi

for TABLE in $(echo ${!DB_TABLES[*]})
do
  SIZE=${DB_TABLES[$TABLE]}
  echo "*** ${TABLE}:${SIZE}"
  for((i=0; i<$SIZE; i++));
  do
    SQL=${DB_SQL/${TABLE}/${TABLE}_${i}}
    echo "${i} -> ${SQL}"
    mysql -u${DB_USER} -p${DB_PASS} -h ${DB_HOST} -P ${DB_PORT} -D ${DB_NAME} -e "${SQL}"
    RESULT=$?
    if [ $RESULT -ne 0 ]; then
      echo "exit:${RESULT}"
      exit $RESULT
    fi
  done
done

echo "done!"
