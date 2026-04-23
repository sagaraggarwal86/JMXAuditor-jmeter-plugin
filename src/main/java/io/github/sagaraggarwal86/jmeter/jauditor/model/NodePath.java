package io.github.sagaraggarwal86.jmeter.jauditor.model;

import java.util.List;

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
