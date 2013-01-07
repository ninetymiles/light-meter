#!/bin/sh

TARGET=$1
PWD=$(pwd)
PROJECT_NAME=$(cat build.xml | grep "project name" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat AndroidManifest.xml | grep "versionName" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat local.properties | grep "version.name" | awk -F"=" '{print $2}')
VERSION_CODE=$(cat local.properties | grep "version.code" | awk -F"=" '{print $2}')
VERSION_CODE=$(git log | grep commit | wc -l)

echo "ProjectName:${PROJECT_NAME}"
echo "VersionName:${VERSION_NAME}"
echo "VersionCode:${VERSION_CODE}"
echo ""

if [ x"$TARGET" = "x" ]; then
    echo "Usage:";
    echo "    bulid.sh TARGET";
    echo "";
    exit 1;
fi

if [ -z "$(grep "version.code" local.properties)" ]; then
    echo "version.code=$VERSION_CODE" >> local.properties
else
    sed -i "s/version\.code=.*/version\.code=$VERSION_CODE/" local.properties
fi

if [ x"$TARGET" = "xrelease" -o x"$TARGET" = "xall" ]; then
    sed -i "/versionName/s/\".*\"/\"${VERSION_NAME}\"/" AndroidManifest.xml
    ant clean release
    git checkout AndroidManifest.xml
fi

