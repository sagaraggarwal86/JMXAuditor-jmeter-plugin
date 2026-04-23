package io.github.sagaraggarwal86.jmeter.jauditor.export.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HtmlTemplate {

    private static final String BASE = "/io/github/sagaraggarwal86/jmeter/jauditor/report/";

    private HtmlTemplate() {
    }

    public static String render(Map<String, String> tokens) throws IOException {
        String tpl = loadResource(BASE + "report-template.html");
        String css = loadResource(BASE + "report-styles.css");
        String out = tpl.replace("{{styles}}", css);
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
