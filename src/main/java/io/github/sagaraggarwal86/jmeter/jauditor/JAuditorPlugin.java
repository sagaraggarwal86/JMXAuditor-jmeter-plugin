package io.github.sagaraggarwal86.jmeter.jauditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class JAuditorPlugin {

    public static final String NAME = "JAuditor";
    public static final String ACTION_ID = "jauditor.audit";

    private static final Properties VERSION = load();

    private JAuditorPlugin() {
    }

    public static String version() {
        return VERSION.getProperty("version", "0.0.0");
    }

    public static String buildTimestamp() {
        return VERSION.getProperty("buildTimestamp", "");
    }

    private static Properties load() {
        Properties p = new Properties();
        String res = "/io/github/sagaraggarwal86/jmeter/jauditor/version.properties";
        try (InputStream in = JAuditorPlugin.class.getResourceAsStream(res)) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // fall through with defaults
        }
        return p;
    }
}
