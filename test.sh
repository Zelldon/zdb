#!/usr/bin/env bash

trap 'echo Terminated $0; exit' INT;

set -x

java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar &&
java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar status --path=database/data/raft-partition/partitions/1/runtime &&

java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar blacklist --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar blacklist --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685307 &&

java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar incident --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar incident --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685269 &&

java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar workflow --path=database/data/raft-partition/partitions/1/runtime list &&
java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar workflow --path=database/data/raft-partition/partitions/1/runtime entity 2251799813685305 &&

java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar log status --path=database/data/raft-partition/partitions/1/ &&
java -jar zdb/target/zdb-0.1.0-SNAPSHOT.jar log consistency --path=database/data/raft-partition/partitions/1