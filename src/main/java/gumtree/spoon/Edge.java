package gumtree.spoon;

import org.jgrapht.graph.DefaultEdge;
import com.github.gumtreediff.tree.Tree;

public class Edge extends
        DefaultEdge
{
    private Tree src;
    private Tree dst;
    private String label;


    /**
     * Constructs a relationship edge
     *
     * @param label the label of the new edge.
     *
     */
    public Edge(Tree src, Tree dst, String label)
    {
        this.src = src;
        this.dst = dst;
        this.label = label;
    }

    /**
     * Gets the label associated with this edge.
     *
     * @return edge label
     */
    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return "(" + getSource() + " : " + getTarget() + " : " + label + ")";
    }
}
