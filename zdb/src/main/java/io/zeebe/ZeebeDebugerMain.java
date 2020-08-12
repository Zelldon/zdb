package io.zeebe;

import io.zeebe.impl.ZeebeStatusImpl;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeebeDebugerMain
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeDebugerMain.class);

    public static void main(String[] args) throws Exception
    {
       LOGGER.info("Zeebe Debug and inspection tool");

       // parse given parameters - exit with error code if necessary
        final var partitionPath = Path.of(args[0]);

        // call corresponding command
        final var result = new ZeebeStatusImpl().status(partitionPath);

        // print result
        LOGGER.info(result);
    }
}
