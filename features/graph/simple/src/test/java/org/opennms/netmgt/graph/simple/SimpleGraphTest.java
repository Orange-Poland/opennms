package org.opennms.netmgt.graph.simple;

import static junit.framework.TestCase.assertEquals;
import static org.opennms.netmgt.graph.simple.TestObjectCreator.createEdge;
import static org.opennms.netmgt.graph.simple.TestObjectCreator.createVertex;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.netmgt.graph.api.generic.GenericGraph;

public class SimpleGraphTest {

    /**
     * Convert a SimpleGraph into a GenericGraph and back. All properties should be kept but we should have copies
     * of the elements in the graph - not the same objects. */
    @Test
    public void simpleGraphShouldBeAbleToBeConvertedIntoAGenericGraphAndBack() {

        // set up:
        String namespace = SimpleGraphTest.class.getSimpleName();
        SimpleGraph originalGraph = new SimpleGraph(namespace);
        originalGraph.setLabel("labelGraph");
        originalGraph.setNamespace(TestObjectCreator.NAMESPACE);
        SimpleVertex vertex1 = createVertex(namespace, UUID.randomUUID().toString());
        SimpleVertex vertex2 = createVertex(namespace, UUID.randomUUID().toString());
        SimpleVertex vertex3 = createVertex(namespace, UUID.randomUUID().toString());
        SimpleEdge edge1 = createEdge(vertex1, vertex2);
        SimpleEdge edge2 = createEdge(vertex1, vertex3);
        originalGraph.addVertex(vertex1);
        originalGraph.addVertex(vertex2);
        originalGraph.addVertex(vertex3);
        originalGraph.addEdge(edge1);
        originalGraph.addEdge(edge2);

        // convert:
        GenericGraph genericGraph = originalGraph.asGenericGraph();
        SimpleGraph copyGraph = new SimpleGraph(genericGraph); // copy constructor

        // test:
        assertEquals(originalGraph.getLabel(), copyGraph.getLabel());
        assertEquals(originalGraph.getNamespace(), copyGraph.getNamespace());
        equalsButNotSame(originalGraph, copyGraph);
        equalsButNotSame(originalGraph.getVertex(vertex1.getId()), copyGraph.getVertex(vertex1.getId()));
        equalsButNotSame(originalGraph.getVertex(vertex2.getId()), copyGraph.getVertex(vertex2.getId()));
        equalsButNotSame(originalGraph.getVertex(vertex3.getId()), copyGraph.getVertex(vertex3.getId()));
        equalsButNotSame(originalGraph.getEdge(edge1.getId()), copyGraph.getEdge(edge1.getId()));
        equalsButNotSame(originalGraph.getEdge(edge2.getId()), copyGraph.getEdge(edge2.getId()));

    }

    private void equalsButNotSame(Object original, Object copy){
        Assert.assertEquals(original, copy);
        Assert.assertNotSame(original, copy);
    }

}
