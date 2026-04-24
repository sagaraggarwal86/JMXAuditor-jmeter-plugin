package io.github.sagaraggarwal86.jmeter.jauditor.export.html;

/**
 * HTML entity escaper for the five characters that can break out of HTML text or
 * attribute contexts: {@code <}, {@code >}, {@code &}, {@code "}, {@code '}. Used
 * defensively around every piece of user-controlled text emitted by
 * {@link HtmlReportWriter} — rule ids, node paths, finding titles/descriptions,
 * suggestions, and jmx file names.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
