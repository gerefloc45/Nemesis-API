package com.myname.nemesisapi.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for composite behavior tree nodes.
 * Nodes can have children and implement different execution strategies.
 * 
 * @author Nemesis-API Framework
 * @version 1.0.0
 */
public abstract class BehaviorNode implements Behavior {
    protected final List<Behavior> children;
    private boolean started = false;

    /**
     * Creates a new behavior node with no children.
     */
    public BehaviorNode() {
        this.children = new ArrayList<>();
    }

    /**
     * Adds a child behavior to this node.
     *
     * @param child The child behavior to add
     * @return This node for method chaining
     */
    public BehaviorNode addChild(Behavior child) {
        children.add(child);
        return this;
    }

    /**
     * Gets all children of this node.
     *
     * @return List of child behaviors
     */
    public List<Behavior> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Checks if this node has been started.
     *
     * @return True if started
     */
    protected boolean isStarted() {
        return started;
    }

    /**
     * Marks this node as started.
     */
    protected void markStarted() {
        this.started = true;
    }

    /**
     * Resets the started flag.
     */
    protected void reset() {
        this.started = false;
    }

    @Override
    public void onStart(BehaviorContext context) {
        reset();
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        reset();
    }
}
