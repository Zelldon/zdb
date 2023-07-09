#!/bin/bash
#
# Copyright © 2021 Christopher Kujawa (zelldon91@gmail.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#



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

git checkout main
git pull origin main

mvn versions:set "-DnewVersion=$version" -DgenerateBackupPoms=false
mvn clean install -DskipTests

git commit -am "build: release version $version"
git tag "$version"

mvn versions:set "-DnewVersion=$nextVersion-SNAPSHOT" -DgenerateBackupPoms=false

git commit -am "build: prepare for next version"

cp cli/target/cli-*-jar-with-dependencies.jar zdb.jar

git push origin main
git push origin "$version"
