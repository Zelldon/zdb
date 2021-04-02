#!/usr/bin/env bash

trap 'echo Terminated $0; exit' INT;

set -x

version=*

java -jar zdb/target/zdb-$version.jar &&
java -jar zdb/target/zdb-$version.jar status --path=database/data/raft-partition/partitions/1/runtime &&

java -jar zdb/target/zdb-$version.jar blacklist --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-$version.jar blacklist --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685307 &&

java -jar zdb/target/zdb-$version.jar incident --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-$version.jar incident --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685269 &&

java -jar zdb/target/zdb-$version.jar workflow --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-$version.jar workflow --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685305 &&

java -jar zdb/target/zdb-$version.jar log status --path=database/data/raft-partition/partitions/1/ &&
java -jar zdb/target/zdb-$version.jar log consistency --path=database/data/raft-partition/partitions/1 &&
java -jar zdb/target/zdb-$version.jar log search --path=database/data/raft-partition/partitions/1 -pos=4294967296
