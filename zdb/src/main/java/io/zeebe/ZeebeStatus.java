package io.zeebe;

import java.nio.file.Path;

public interface ZeebeStatus {

  /**
   * Returns an well formated string which contains all status information about Zeebe, which is
   * located under the given path.
   *
   * <p>Contained information are:
   *
   * <p>* lastExportedPosition * lastProcessedPosition
   *
   * @param path the partition path
   * @return a well formated string which contains all information
   */
  public String status(Path path);
}
