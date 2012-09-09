#!/bin/sh

TARGET=$1
PROJECT_NAME=$(cat build.xml | grep "project name" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat AndroidManifest.xml | grep "versionName" | awk -F"\"" '{print $2}')

echo "ProjectName:${PROJECT_NAME}"
echo "VersionName:${VERSION_NAME}"

exit 1;

if [ x"$TARGET" = "xrelease" -o x"$TARGET" = "xall" ]; then
    PROJECT="internal"
    ant -q clean
    ant -q release
    mv ${PROJECT_NAME}-release.apk ${PROJECT_NAME}_${VERSION_NAME}.apk
fi

if [ x"$TARGET" = "xlite" -o x"$TARGET" = "xall" ]; then
    PROJECT="lite"
    ant -q clean
    ant -q release
    mv ${PROJECT_NAME}-release.apk ${PROJECT_NAME}_${VERSION_NAME}_lite.apk
fi 

if [ x"$TARGET" = "xdebug" ]; then
    PROJECT="debug"
    ant clean
    ant debug
fi

if [ x"$PROJECT" = "x" ]; then
    echo "Unsupported TARGET"
fi

# Generate checksum
md5sum ${PROJECT_NAME}_${VERSION_NAME}*.apk > ${PROJECT_NAME}_${VERSION_NAME}.md5

