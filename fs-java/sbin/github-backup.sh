PROJECT_ROOT=$(dirname $(readlink -f $0))
PROJECT_ROOT=$(dirname $(dirname $PROJECT_ROOT))

echo "PROJECT_ROOT: ${PROJECT_ROOT}"

TARGET_FILES=$(ls ${PROJECT_ROOT})
for PROJECT_NAME in ${TARGET_FILES[@]}
do
  echo "PROJECT_NAME: ${PROJECT_NAME}"
  DIR_NAME=$PROJECT_ROOT/$PROJECT_NAME
  if [ -d "$DIR_NAME" ];then
    echo "DIR_NAME: ${DIR_NAME}"
    cd $DIR_NAME
    git pull
  fi
done

echo "done!"
