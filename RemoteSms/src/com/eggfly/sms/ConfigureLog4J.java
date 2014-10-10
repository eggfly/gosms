
package com.eggfly.sms;

import java.io.File;

import org.apache.log4j.Level;

import android.os.Environment;
import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * @author eggfly
 */
public class ConfigureLog4J {
    public static void configure() {
        final LogConfigurator logConfigurator = new LogConfigurator();

        File logDir = new File(Environment.getExternalStorageDirectory(), "/MIUI/debug_log/RemoteSms/");
        if (!logDir.isDirectory()) {
            logDir.mkdirs();
        }

        logConfigurator.setFileName(new File(logDir, "sms.log").getAbsolutePath());
        logConfigurator.setRootLevel(Level.DEBUG);
        // "%d - [%p::%c] - %m%n"
        logConfigurator.setFilePattern("[%p::%c] - %m%n");
        // Set log level of a specific logger
        // logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();
    }
}
