package io.zell.zdb

import java.io.File
import java.nio.file.Path

class ZeebePaths {

    companion object {
        fun getRuntimePath(dataPath : File, partition : String) : Path {
            return dataPath.toPath()
                .resolve("raft-partition")
                .resolve("partitions")
                .resolve(partition)
                .resolve("runtime")
        }

        fun getLogPath(dataPath : File, partition : String) : Path {
            return dataPath.toPath()
                .resolve("raft-partition")
                .resolve("partitions")
                .resolve(partition)
        }
    }
}
