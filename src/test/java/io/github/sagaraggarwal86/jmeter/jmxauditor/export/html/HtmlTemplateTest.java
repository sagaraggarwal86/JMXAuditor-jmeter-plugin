package io.github.sagaraggarwal86.jmeter.jmxauditor.export.html;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class HtmlTemplateTest {

    @Test
    void substitutesTokensAndInlinesSentinels() throws IOException {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("title", "Hello");
        tokens.put("jmxFileName", "audit.jmx");
        tokens.put("scanTimestamp", "2026-04-24");
        tokens.put("pluginVersion", "0.2.0");
        tokens.put("headerBanners", "");
        tokens.put("navTabs", "<button>Summary</button>");
        tokens.put("panels", "<div>panels</div>");

        String html = HtmlTemplate.render(tokens);

        assertThat(html).contains("Hello");
        assertThat(html).contains("audit.jmx");
        assertThat(html).contains("<button>Summary</button>");
        // Sentinels must be replaced — the marker comments themselves don't survive.
        assertThat(html).doesNotContain("/*__STYLES__*/");
        assertThat(html).doesNotContain("/*__XLSX__*/");
        // CSS and the xlsx bundle should be inlined; check for distinctive markers.
        assertThat(html).contains(".banner"); // from report-styles.css
        assertThat(html).contains("XLSX");    // from xlsx-style bundle
    }

    @Test
    void nullTokenRendersAsEmpty() throws IOException {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("title", null);
        tokens.put("jmxFileName", "x");
        tokens.put("scanTimestamp", "y");
        tokens.put("pluginVersion", "z");
        tokens.put("headerBanners", "");
        tokens.put("navTabs", "");
        tokens.put("panels", "");

        // No NPE; null becomes empty string.
        String html = HtmlTemplate.render(tokens);
        assertThat(html).doesNotContain("{{title}}");
    }
}
