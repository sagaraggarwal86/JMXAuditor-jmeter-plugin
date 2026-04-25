package io.github.sagaraggarwal86.jmeter.jmxauditor.support;

import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JmxTestHarnessTest {

    @Test
    void emptyPlanRootIsTestPlan() {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        JMeterTreeNode root = (JMeterTreeNode) model.getRoot();
        // model.getRoot() is the synthetic invisible root; child(0) is the real TestPlan.
        JMeterTreeNode planNode = (JMeterTreeNode) root.getChildAt(0);
        assertThat(planNode.getTestElement()).isInstanceOf(TestPlan.class);
    }

    @Test
    void singleElementWrapsInTestPlan() {
        ThreadGroup tg = new ThreadGroup();
        tg.setName("My Thread Group");
        JmxTestHarness.NodeWithModel result = JmxTestHarness.singleElement(tg);
        assertThat(result.node().getTestElement()).isSameAs(tg);
        // The parent should be the TestPlan node.
        JMeterTreeNode parent = (JMeterTreeNode) result.node().getParent();
        assertThat(parent.getTestElement()).isInstanceOf(TestPlan.class);
    }

    @Test
    void newContextReturnsUsableContext() {
        JMeterTreeModel model = JmxTestHarness.emptyPlan();
        var ctx = JmxTestHarness.newContext(model);
        assertThat(ctx.tree()).isSameAs(model);
        assertThat(ctx.deadline().expired()).isFalse();
    }
}
