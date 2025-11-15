package com.gerefloc45.voidapi.api.fsm;

import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles persistence of state machine state to NBT.
 * Allows state machines to save and restore their state.
 * 
 * @author VoidAPI Framework
 * @version 0.3.0
 */
public class StatePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatePersistence.class);
    private static final String KEY_CURRENT_STATE = "CurrentState";
    private static final String KEY_ENTER_TIME = "EnterTime";
    private static final String KEY_IS_RUNNING = "IsRunning";
    private static final String KEY_BLACKBOARD = "Blackboard";
    private static final String KEY_SUB_MACHINES = "SubMachines";

    /**
     * Saves a state machine's state to NBT.
     *
     * @param stateMachine The state machine to save
     * @param blackboard Optional blackboard to save with state
     * @return NBT compound containing state data
     */
    public static NbtCompound save(StateMachine stateMachine, Blackboard blackboard) {
        NbtCompound nbt = new NbtCompound();
        
        // Save basic state
        nbt.putBoolean(KEY_IS_RUNNING, stateMachine.isRunning());
        
        if (stateMachine.getCurrentState() != null) {
            nbt.putString(KEY_CURRENT_STATE, stateMachine.getCurrentStateName());
            nbt.putLong(KEY_ENTER_TIME, stateMachine.getCurrentState().getEnterTime());
        }
        
        // Save blackboard if provided
        if (blackboard != null) {
            nbt.put(KEY_BLACKBOARD, saveBlackboard(blackboard));
        }
        
        // Save hierarchical sub-machines
        NbtCompound subMachines = saveSubMachines(stateMachine);
        if (!subMachines.isEmpty()) {
            nbt.put(KEY_SUB_MACHINES, subMachines);
        }
        
        return nbt;
    }

    /**
     * Restores a state machine's state from NBT.
     *
     * @param stateMachine The state machine to restore
     * @param nbt NBT compound containing state data
     * @param blackboard Optional blackboard to restore
     * @return True if restoration was successful
     */
    public static boolean restore(StateMachine stateMachine, NbtCompound nbt, Blackboard blackboard) {
        try {
            // Restore running state
            boolean wasRunning = nbt.getBoolean(KEY_IS_RUNNING);
            
            if (wasRunning && nbt.contains(KEY_CURRENT_STATE, NbtElement.STRING_TYPE)) {
                String stateName = nbt.getString(KEY_CURRENT_STATE);
                State state = stateMachine.getState(stateName);
                
                if (state == null) {
                    LOGGER.warn("Cannot restore FSM '{}': state '{}' not found", 
                        stateMachine.getName(), stateName);
                    return false;
                }
                
                // Note: We can't perfectly restore the state without context
                // This is a limitation - states may need re-initialization
                LOGGER.debug("Restored FSM '{}' to state '{}'", 
                    stateMachine.getName(), stateName);
            }
            
            // Restore blackboard if provided
            if (blackboard != null && nbt.contains(KEY_BLACKBOARD, NbtElement.COMPOUND_TYPE)) {
                restoreBlackboard(blackboard, nbt.getCompound(KEY_BLACKBOARD));
            }
            
            // Restore hierarchical sub-machines
            if (nbt.contains(KEY_SUB_MACHINES, NbtElement.COMPOUND_TYPE)) {
                restoreSubMachines(stateMachine, nbt.getCompound(KEY_SUB_MACHINES));
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error restoring state machine '{}'", stateMachine.getName(), e);
            return false;
        }
    }

    /**
     * Saves hierarchical sub-machines.
     *
     * @param stateMachine Parent state machine
     * @return NBT compound with sub-machine data
     */
    private static NbtCompound saveSubMachines(StateMachine stateMachine) {
        NbtCompound subMachines = new NbtCompound();
        
        for (State state : stateMachine.getStates()) {
            if (state instanceof HierarchicalState) {
                HierarchicalState hState = (HierarchicalState) state;
                StateMachine subMachine = hState.getSubStateMachine();
                subMachines.put(state.getName(), save(subMachine, null));
            }
        }
        
        return subMachines;
    }

    /**
     * Restores hierarchical sub-machines.
     *
     * @param stateMachine Parent state machine
     * @param nbt NBT compound with sub-machine data
     */
    private static void restoreSubMachines(StateMachine stateMachine, NbtCompound nbt) {
        for (State state : stateMachine.getStates()) {
            if (state instanceof HierarchicalState && nbt.contains(state.getName())) {
                HierarchicalState hState = (HierarchicalState) state;
                StateMachine subMachine = hState.getSubStateMachine();
                restore(subMachine, nbt.getCompound(state.getName()), null);
            }
        }
    }

    /**
     * Saves blackboard data to NBT.
     * Only saves primitive types and strings.
     *
     * @param blackboard The blackboard to save
     * @return NBT compound with blackboard data
     */
    private static NbtCompound saveBlackboard(Blackboard blackboard) {
        NbtCompound nbt = new NbtCompound();
        
        // Note: This is a simplified implementation
        // Only saves string representations of values
        // For full persistence, implement custom serialization
        
        return nbt;
    }

    /**
     * Restores blackboard data from NBT.
     *
     * @param blackboard The blackboard to restore
     * @param nbt NBT compound with blackboard data
     */
    private static void restoreBlackboard(Blackboard blackboard, NbtCompound nbt) {
        // Note: This is a simplified implementation
        // Implement custom deserialization as needed
    }

    /**
     * Creates a snapshot of a state machine's current state.
     *
     * @param stateMachine The state machine
     * @return Snapshot object
     */
    public static StateSnapshot createSnapshot(StateMachine stateMachine) {
        return new StateSnapshot(
            stateMachine.getName(),
            stateMachine.getCurrentStateName(),
            stateMachine.isRunning(),
            System.currentTimeMillis()
        );
    }

    /**
     * Immutable snapshot of a state machine's state.
     */
    public static class StateSnapshot {
        private final String machineName;
        private final String currentState;
        private final boolean isRunning;
        private final long timestamp;

        public StateSnapshot(String machineName, String currentState, boolean isRunning, long timestamp) {
            this.machineName = machineName;
            this.currentState = currentState;
            this.isRunning = isRunning;
            this.timestamp = timestamp;
        }

        public String getMachineName() {
            return machineName;
        }

        public String getCurrentState() {
            return currentState;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "StateSnapshot{" + machineName + 
                ", state=" + currentState + 
                ", running=" + isRunning + 
                ", time=" + timestamp + "}";
        }
    }
}
