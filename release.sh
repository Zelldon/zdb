#!/bin/bash


set -exo pipefail

if [ -z "$1" ] 
then
  echo "Provide a version: ./release releaseVersion nextVersion"
  exit 1
fi


if [ -z "$2" ] 
then
  echo "Provide a version: ./release releaseVersion nextVersion"
  exit 1
fi

version=$1
nextVersion=$2

git checkout master
git pull origin master

mvn versions:set "-DnewVersion=$version" -DgenerateBackupPoms=false
mvn clean install -DskipTests

git commit -am "build: release version $version"
git tag "$version"

mvn versions:set "-DnewVersion=$nextVersion-SNAPSHOT" -DgenerateBackupPoms=false

git commit -am "build: prepare for next version"

cp cli/target/cli-*-jar-with-dependencies.jar zdb.jar


