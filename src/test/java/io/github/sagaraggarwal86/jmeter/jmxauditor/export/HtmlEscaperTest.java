package io.github.sagaraggarwal86.jmeter.jmxauditor.export;

import io.github.sagaraggarwal86.jmeter.jmxauditor.export.html.HtmlEscaper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEscaperTest {

    @Test
    void escapesFiveChars() {
        assertThat(HtmlEscaper.escape("<a href=\"x\">'&'</a>"))
                .isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
    }

    @Test
    void passesThroughPlainText() {
        assertThat(HtmlEscaper.escape("hello world")).isEqualTo("hello world");
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(HtmlEscaper.escape(null)).isEmpty();
    }
}
