package io.github.sagaraggarwal86.jmeter.jauditor.ui.dialog;

import java.awt.*;

/**
 * FlowLayout that reports the correct preferred size when its content wraps, so the
 * parent container can grow vertically to show every row instead of clipping the ones
 * below the first. Without this, a FlowLayout in BorderLayout.CENTER silently hides
 * components that don't fit on the single row its preferredLayoutSize reports.
 */
final class WrapLayout extends FlowLayout {

    WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    private static void addRow(Dimension dim, int rowWidth, int rowHeight, int vgap) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) dim.height += vgap;
        dim.height += rowHeight;
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension min = layoutSize(target, false);
        min.width -= (getHgap() + 1);
        return min;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                Container c = target;
                while (c.getParent() != null && c.getSize().width == 0) c = c.getParent();
                targetWidth = c.getSize().width;
            }
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontal = insets.left + insets.right + hgap * 2;
            int maxWidth = targetWidth - horizontal;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int n = target.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight, vgap);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) rowWidth += hgap;
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(dim, rowWidth, rowHeight, vgap);

            dim.width += horizontal;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }
}
