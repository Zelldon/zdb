package io.zeebe;

import java.nio.file.Path;
import java.util.List;

public interface EntityInspection {
  List<String> list(final PartitionState partitionState);
  String entity(final PartitionState partitionState, final long key);
}
