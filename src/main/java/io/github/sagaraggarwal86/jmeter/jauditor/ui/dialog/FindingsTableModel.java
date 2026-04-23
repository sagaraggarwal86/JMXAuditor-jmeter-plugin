package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import io.github.sagaraggarwal86.jmeter.jauditor.model.Category;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Finding;
import io.github.sagaraggarwal86.jmeter.jauditor.model.Severity;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class FindingsTableModel extends AbstractTableModel {

    public static final Comparator<Finding> ORDER = Comparator
            .comparingInt((Finding f) -> f.category().ordinal())
            .thenComparingInt(f -> severityRank(f.severity()))
            .thenComparingInt(Finding::treeDepth);
    private final List<Finding> all = new ArrayList<>();
    private final List<Finding> view = new ArrayList<>();
    private final EnumSet<Category> allowedCategories = EnumSet.allOf(Category.class);
    private Filter filter = Filter.ALL;

    private static boolean severityMatches(Severity s, Filter f) {
        return switch (f) {
            case ERROR -> s == Severity.ERROR;
            case WARN -> s == Severity.WARN;
            case INFO -> s == Severity.INFO;
            case ALL -> true;
        };
    }

    private static int severityRank(Severity s) {
        return switch (s) {
            case ERROR -> 0;
            case WARN -> 1;
            case INFO -> 2;
        };
    }

    public void setFindings(List<Finding> findings) {
        all.clear();
        all.addAll(findings);
        all.sort(ORDER);
        rebuildView();
        fireTableDataChanged();
    }

    public void setFilter(Filter f) {
        this.filter = f;
        rebuildView();
        fireTableDataChanged();
    }

    public void setAllowedCategories(Set<Category> cats) {
        allowedCategories.clear();
        allowedCategories.addAll(cats);
        rebuildView();
        fireTableDataChanged();
    }

    public Finding at(int row) {
        return view.get(row);
    }

    @Override
    public int getRowCount() {
        return view.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int r, int c) {
        return view.get(r);
    }

    @Override
    public String getColumnName(int c) {
        return "Finding";
    }

    public long countAll() {
        return all.stream().filter(f -> allowedCategories.contains(f.category())).count();
    }

    public long countSeverity(Severity s) {
        return all.stream().filter(f -> f.severity() == s && allowedCategories.contains(f.category())).count();
    }

    private void rebuildView() {
        view.clear();
        for (Finding f : all) {
            if (!allowedCategories.contains(f.category())) continue;
            if (filter == Filter.ALL || severityMatches(f.severity(), filter)) view.add(f);
        }
    }

    public enum Filter {ALL, ERROR, WARN, INFO}
}
