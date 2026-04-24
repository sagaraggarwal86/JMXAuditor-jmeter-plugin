package io.github.sagaraggarwal86.jmeter.jauditor.export.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads the HTML skeleton, inline CSS, and inline xlsx-js-style bundle from the classpath
 * and performs string-based token substitution. Sentinels use a double-underscore form
 * to avoid collisions with legitimate block comments; per-token markers use the
 * Mustache-ish {@code {{name}}} form. All values are passed through {@link HtmlEscaper}
 * at the writer layer before reaching this class.
 */
public final class HtmlTemplate {

    private static final String BASE = "/io/github/sagaraggarwal86/jmeter/jauditor/report/";

    private HtmlTemplate() {
    }

    public static String render(Map<String, String> tokens) throws IOException {
        String tpl = loadResource(BASE + "report-template.html");
        String css = loadResource(BASE + "report-styles.css");
        String xlsx = loadResource(BASE + "xlsx-style.bundle.js");
        // Sentinels use double-underscore to avoid accidental collision with
        // legitimate CSS/JS block comments like /*STYLES*/ or /*XLSX*/.
        String out = tpl.replace("/*__STYLES__*/", css).replace("/*__XLSX__*/", xlsx);
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream in = HtmlTemplate.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing report resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
