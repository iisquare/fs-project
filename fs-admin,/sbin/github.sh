PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
WORK_DIR=$(dirname $PROJECT_ROOT)

PROJECT_TARGET=fs-project
PROJECT_SYNC=("fs-java" "fs-admin", "fs-python")
TARGET_ROOT=$WORK_DIR/$PROJECT_TARGET

for PROJECT_NAME in ${PROJECT_SYNC[@]}
do
  SYNC_PROJECT=$WORK_DIR/$PROJECT_NAME
  SYNC_TARGET=$TARGET_ROOT/$PROJECT_NAME
  echo "SYNC_NAME:${PROJECT_NAME}"
  echo "SYNC_PROJECT:${SYNC_PROJECT}"
  echo "SYNC_TARGET:${SYNC_TARGET}"
  # 创建目标目录
  if [ ! -d $SYNC_TARGET ]; then
    mkdir $SYNC_TARGET
  fi
  # 清理镜像目录
  TARGET_FILES=$(ls ${SYNC_TARGET})
  for element in ${TARGET_FILES[@]}; do
      TARGET_FILE=${SYNC_TARGET}/$element
      echo "clear:${TARGET_FILE}"
      rm -rf ${TARGET_FILE}
  done
  # 导出项目文件
  cd $SYNC_PROJECT
  echo "change to $(pwd)"
  echo "export project source..."
  git archive --format tar --output "${SYNC_TARGET}/${PROJECT_NAME}.tar" main
  echo "extract project files..."
  tar -xvf ${SYNC_TARGET}/${PROJECT_NAME}.tar -C ${SYNC_TARGET}
  echo "delete archive file"
  rm -f ${SYNC_TARGET}/${PROJECT_NAME}.tar

done

# 清理敏感数据
echo "clear site..."
rm -rf ${TARGET_ROOT}/fs-java/site
rm -rf ${TARGET_ROOT}/fs-java/app/nlp

echo "clear xlab..."
rm -rf ${TARGET_ROOT}/fs-java/web/xlab

echo "clear config..."
find ${TARGET_ROOT}/fs-java/ -name *prod.yml | xargs rm -f

echo "clear python..."
rm -rf ${TARGET_ROOT}/fs-python/image-compare

# 清理执行脚本
echo "clear shell..."
rm -rf ${TARGET_ROOT}/fs-java/sbin/github.sh

echo "done!"
