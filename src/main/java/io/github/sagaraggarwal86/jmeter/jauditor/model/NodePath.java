package io.github.sagaraggarwal86.jmeter.jauditor.model;

import java.util.List;

/**
 * Immutable list of tree-node names from the root down to a finding's element.
 * {@link #breadcrumb()} joins segments with U+203A (›) for display in the dialog,
 * HTML report, and JSON output.
 */
public record NodePath(List<String> segments) {

    public NodePath {
        segments = List.copyOf(segments);
    }

    public String breadcrumb() {
        return String.join(" › ", segments);
    }

    public int depth() {
        return segments.size();
    }
}
