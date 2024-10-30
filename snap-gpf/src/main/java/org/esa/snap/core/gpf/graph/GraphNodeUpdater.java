package org.esa.snap.core.gpf.graph;

/**
 * A helper class used to command the update of the GraphNode object from SNAP Graph Builder by Graph Processing Framework (GPF)
 *
 * @author Adrian Drăghici
 */
public abstract class GraphNodeUpdater {

    /**
     * The commander method used to update the GraphNode object from SNAP Graph Builder by Graph Processing Framework (GPF) using the NodeContext object with the latest changes
     *
     * @param nodeContext the NodeContext object with the latest changes from Graph Processing Framework (GPF)
     * @throws GraphException if any error occurs during execution
     */
    public abstract void doUpdate(NodeContext nodeContext) throws GraphException;
}
