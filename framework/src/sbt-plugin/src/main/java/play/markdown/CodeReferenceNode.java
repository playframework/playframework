package play.markdown;

import org.pegdown.ast.AbstractNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.Visitor;

import java.util.Collections;
import java.util.List;

/**
 * A code reference node
 */
public class CodeReferenceNode extends AbstractNode {

    private final String label;
    private final String source;

    public CodeReferenceNode(String label, String source) {
        this.label = label;
        this.source = source;
    }

    public String getLabel() {
        return label;
    }

    public String getSource() {
        return source;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<Node> getChildren() {
        return Collections.emptyList();
    }
}
