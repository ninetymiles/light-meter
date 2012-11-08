#!/bin/sh

TARGET=$1
PWD=$(pwd)
PROJECT_NAME=$(cat build.xml | grep "project name" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat AndroidManifest.xml | grep "versionName" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat local.properties | grep "version.name" | awk -F"=" '{print $2}')
VERSION_CODE=$(cat local.properties | grep "version.code" | awk -F"=" '{print $2}')

echo "ProjectName:${PROJECT_NAME}"
echo "VersionName:${VERSION_NAME}"
echo "VersionCode:${VERSION_CODE}"
echo ""

if [ x"$TARGET" = "xrelease" -o x"$TARGET" = "xall" ]; then
    sed -i "/versionName/s/\".*\"/\"${VERSION_NAME}\"/" AndroidManifest.xml
    ant clean release
    git checkout AndroidManifest.xml
fi

