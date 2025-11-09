package com.myname.nemesisapi.api.nodes;

import com.myname.nemesisapi.api.Behavior;
import com.myname.nemesisapi.api.BehaviorContext;
import com.myname.nemesisapi.api.BehaviorNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Parallel node - executes all children simultaneously.
 * Success policy determines when the node succeeds.
 * 
 * @author Nemesis-API Framework
 * @version 1.1.0
 */
public class ParallelNode extends BehaviorNode {
    
    /**
     * Policy for determining success of parallel execution.
     */
    public enum Policy {
        /** Require all children to succeed */
        REQUIRE_ALL,
        /** Require at least one child to succeed */
        REQUIRE_ONE
    }
    
    private final Policy successPolicy;
    private final List<Status> childStatuses;

    /**
     * Creates a new parallel node with REQUIRE_ALL policy.
     */
    public ParallelNode() {
        this(Policy.REQUIRE_ALL);
    }

    /**
     * Creates a new parallel node with specified policy.
     *
     * @param successPolicy The success policy
     */
    public ParallelNode(Policy successPolicy) {
        super();
        this.successPolicy = successPolicy;
        this.childStatuses = new ArrayList<>();
    }

    @Override
    public Status execute(BehaviorContext context) {
        if (children.isEmpty()) {
            return Status.SUCCESS;
        }

        // Initialize statuses on first run
        if (childStatuses.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                childStatuses.add(Status.RUNNING);
                children.get(i).onStart(context);
            }
        }

        int successCount = 0;
        int failureCount = 0;
        int runningCount = 0;

        // Execute all children
        for (int i = 0; i < children.size(); i++) {
            Status currentStatus = childStatuses.get(i);
            
            // Skip already completed children
            if (currentStatus != Status.RUNNING) {
                if (currentStatus == Status.SUCCESS) successCount++;
                else if (currentStatus == Status.FAILURE) failureCount++;
                continue;
            }

            // Execute child
            Behavior child = children.get(i);
            Status newStatus = child.execute(context);
            childStatuses.set(i, newStatus);

            if (newStatus == Status.SUCCESS) {
                child.onEnd(context, newStatus);
                successCount++;
            } else if (newStatus == Status.FAILURE) {
                child.onEnd(context, newStatus);
                failureCount++;
            } else {
                runningCount++;
            }
        }

        // Determine overall status based on policy
        switch (successPolicy) {
            case REQUIRE_ALL:
                if (successCount == children.size()) {
                    reset();
                    return Status.SUCCESS;
                }
                if (failureCount > 0) {
                    reset();
                    return Status.FAILURE;
                }
                break;
                
            case REQUIRE_ONE:
                if (successCount > 0) {
                    reset();
                    return Status.SUCCESS;
                }
                if (failureCount == children.size()) {
                    reset();
                    return Status.FAILURE;
                }
                break;
        }

        return Status.RUNNING;
    }

    @Override
    public void onStart(BehaviorContext context) {
        super.onStart(context);
        childStatuses.clear();
    }

    @Override
    public void onEnd(BehaviorContext context, Status status) {
        // Clean up any still-running children
        for (int i = 0; i < children.size(); i++) {
            if (childStatuses.size() > i && childStatuses.get(i) == Status.RUNNING) {
                children.get(i).onEnd(context, status);
            }
        }
        childStatuses.clear();
        super.onEnd(context, status);
    }
}
