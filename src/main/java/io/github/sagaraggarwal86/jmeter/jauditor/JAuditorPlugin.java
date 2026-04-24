package io.github.sagaraggarwal86.jmeter.jauditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Plugin identity constants and version resolution. {@link #version()} prefers the
 * Maven-filtered {@code version.properties} entry; falls back to the JAR manifest
 * {@code Implementation-Version}; falls back to {@code "dev"}. The fallback chain
 * defends against the unfiltered resource (which still contains the literal
 * {@code ${project.version}} token) reaching the UI when running from an IDE
 * without a full Maven build.
 */
public final class JAuditorPlugin {

    public static final String NAME = "JAuditor";
    public static final String ACTION_ID = "jauditor.audit";

    private static final Properties VERSION = load();

    private JAuditorPlugin() {
    }

    public static String version() {
        String v = VERSION.getProperty("version", "");
        if (!v.isEmpty() && !v.contains("${")) return v;
        String manifest = JAuditorPlugin.class.getPackage().getImplementationVersion();
        if (manifest != null && !manifest.isBlank()) return manifest;
        return "dev";
    }

    public static String buildTimestamp() {
        String t = VERSION.getProperty("buildTimestamp", "");
        return (t.contains("${")) ? "" : t;
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
