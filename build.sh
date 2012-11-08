#!/bin/sh

TARGET=$1
PWD=$(pwd)
PROJECT_NAME=$(cat build.xml | grep "project name" | awk -F"\"" '{print $2}')
VERSION_NAME=$(cat AndroidManifest.xml | grep "versionName" | awk -F"\"" '{print $2}')

echo "ProjectName:${PROJECT_NAME}"
echo "VersionName:${VERSION_NAME}"
echo ""

if [ x"$TARGET" = "xrelease" -o x"$TARGET" = "xall" ]; then
    ant -q clean
    ant -q release
fi

