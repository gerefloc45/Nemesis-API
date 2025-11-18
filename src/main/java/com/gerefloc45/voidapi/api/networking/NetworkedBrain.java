package com.gerefloc45.voidapi.api.networking;

import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce la sincronizzazione dell'AI tra server e client.
 * Implementa client-side prediction per ridurre la latenza percepita.
 * 
 * Features:
 * - Sincronizzazione stato AI
 * - Client-side prediction
 * - Interpolazione movimento
 * - Delta compression
 * 
 * @since v0.6.0
 */
public class NetworkedBrain {
    
    private final LivingEntity entity;
    private final BehaviorTree behaviorTree;
    private final Blackboard blackboard;
    
    // Network state
    private final Map<UUID, PlayerSyncState> playerStates = new ConcurrentHashMap<>();
    private long lastSyncTime = 0;
    private int syncInterval = 50; // ms
    
    // Prediction
    private boolean enablePrediction = true;
    private final Queue<PredictedAction> predictedActions = new LinkedList<>();
    private int maxPredictionSteps = 5;
    
    // Compression
    private NbtCompound lastSentState = new NbtCompound();
    private boolean useDeltaCompression = true;
    
    public NetworkedBrain(LivingEntity entity, BehaviorTree behaviorTree, Blackboard blackboard) {
        this.entity = entity;
        this.behaviorTree = behaviorTree;
        this.blackboard = blackboard;
    }
    
    /**
     * Aggiorna lo stato di rete dell'AI.
     * Chiamato ogni tick sul server.
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        // Sync con i client se necessario
        if (currentTime - lastSyncTime >= syncInterval) {
            syncToClients();
            lastSyncTime = currentTime;
        }
        
        // Pulisci predizioni vecchie
        cleanupPredictions();
    }
    
    /**
     * Sincronizza lo stato AI con tutti i client vicini.
     */
    private void syncToClients() {
        if (entity.getWorld().isClient) return;
        
        NbtCompound currentState = serializeState();
        
        // Delta compression: invia solo i cambiamenti
        NbtCompound deltaState = useDeltaCompression 
            ? computeDelta(lastSentState, currentState)
            : currentState;
        
        if (deltaState.isEmpty()) return; // Nessun cambiamento
        
        // Invia ai player vicini
        List<ServerPlayerEntity> nearbyPlayers = getNearbyPlayers();
        for (ServerPlayerEntity player : nearbyPlayers) {
            sendStateToPlayer(player, deltaState);
        }
        
        lastSentState = currentState;
    }
    
    /**
     * Serializza lo stato corrente dell'AI.
     */
    private NbtCompound serializeState() {
        NbtCompound nbt = new NbtCompound();
        
        // Stato behavior tree
        nbt.putString("currentNode", behaviorTree.getRootBehavior().getClass().getSimpleName());
        nbt.putString("status", behaviorTree.getLastStatus().toString());
        
        // Dati blackboard critici (solo quelli necessari per rendering)
        NbtCompound blackboardData = new NbtCompound();
        serializeCriticalBlackboardData(blackboardData);
        nbt.put("blackboard", blackboardData);
        
        // Posizione e movimento
        nbt.putDouble("x", entity.getX());
        nbt.putDouble("y", entity.getY());
        nbt.putDouble("z", entity.getZ());
        nbt.putFloat("yaw", entity.getYaw());
        nbt.putFloat("pitch", entity.getPitch());
        
        return nbt;
    }
    
    /**
     * Serializza solo i dati critici del blackboard per ridurre bandwidth.
     */
    private void serializeCriticalBlackboardData(NbtCompound nbt) {
        // Solo dati necessari per rendering/predizione client-side
        if (blackboard.has("target")) {
            blackboard.<String>get("target").ifPresent(val -> nbt.putString("target", val));
        }
        if (blackboard.has("animation")) {
            blackboard.<String>get("animation").ifPresent(val -> nbt.putString("animation", val));
        }
        if (blackboard.has("state")) {
            blackboard.<String>get("state").ifPresent(val -> nbt.putString("state", val));
        }
    }
    
    /**
     * Calcola il delta tra due stati per compressione.
     */
    private NbtCompound computeDelta(NbtCompound oldState, NbtCompound newState) {
        NbtCompound delta = new NbtCompound();
        
        for (String key : newState.getKeys()) {
            if (!oldState.contains(key) || !oldState.get(key).equals(newState.get(key))) {
                delta.put(key, newState.get(key));
            }
        }
        
        return delta;
    }
    
    /**
     * Invia lo stato a un player specifico.
     */
    private void sendStateToPlayer(ServerPlayerEntity player, NbtCompound state) {
        PlayerSyncState syncState = playerStates.computeIfAbsent(
            player.getUuid(), 
            uuid -> new PlayerSyncState()
        );
        
        syncState.lastSyncTime = System.currentTimeMillis();
        syncState.lastSentState = state.copy();
        
        // Invia packet (implementazione specifica del mod)
        NetworkPacketHandler.sendAIStatePacket(player, entity.getId(), state);
    }
    
    /**
     * Ottiene i player vicini che devono ricevere aggiornamenti.
     */
    private List<ServerPlayerEntity> getNearbyPlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>();
        double syncRange = 64.0; // Range di sincronizzazione
        
        entity.getWorld().getPlayers().forEach(player -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (serverPlayer.squaredDistanceTo(entity) <= syncRange * syncRange) {
                    players.add(serverPlayer);
                }
            }
        });
        
        return players;
    }
    
    /**
     * Predice le prossime azioni dell'AI sul client.
     * Riduce la latenza percepita.
     */
    public void predictNextActions() {
        if (!enablePrediction || !entity.getWorld().isClient) return;
        
        // Predici basandoti sullo stato corrente
        for (int i = 0; i < maxPredictionSteps; i++) {
            PredictedAction action = predictNextAction();
            if (action != null) {
                predictedActions.add(action);
            } else {
                break;
            }
        }
    }
    
    /**
     * Predice la prossima azione basandosi sullo stato corrente.
     */
    private PredictedAction predictNextAction() {
        // Logica di predizione semplice
        // In un sistema reale, userebbe ML o pattern recognition
        
        String currentNode = behaviorTree.getRootBehavior().getClass().getSimpleName();
        if (currentNode.contains("Move")) {
            return new PredictedAction("move", System.currentTimeMillis() + 50);
        } else if (currentNode.contains("Attack")) {
            return new PredictedAction("attack", System.currentTimeMillis() + 100);
        }
        
        return null;
    }
    
    /**
     * Pulisce le predizioni vecchie o confermate.
     */
    private void cleanupPredictions() {
        long currentTime = System.currentTimeMillis();
        predictedActions.removeIf(action -> action.timestamp < currentTime - 1000);
    }
    
    /**
     * Applica un aggiornamento ricevuto dal server (client-side).
     */
    public void applyServerUpdate(NbtCompound state) {
        if (!entity.getWorld().isClient) return;
        
        // Verifica predizioni
        if (enablePrediction) {
            validatePredictions(state);
        }
        
        // Applica lo stato
        deserializeState(state);
    }
    
    /**
     * Valida le predizioni contro lo stato del server.
     */
    private void validatePredictions(NbtCompound serverState) {
        // Confronta predizioni con stato reale
        // Se differiscono troppo, correggi
        
        String serverNode = serverState.getString("currentNode");
        PredictedAction lastPrediction = predictedActions.peek();
        
        if (lastPrediction != null) {
            boolean predictionCorrect = serverNode.toLowerCase().contains(lastPrediction.action);
            if (predictionCorrect) {
                predictedActions.poll(); // Rimuovi predizione corretta
            } else {
                // Predizione sbagliata, pulisci tutte
                predictedActions.clear();
            }
        }
    }
    
    /**
     * Deserializza lo stato ricevuto dal server.
     */
    private void deserializeState(NbtCompound nbt) {
        // Aggiorna behavior tree state (solo per visualizzazione)
        // Il client non esegue l'AI, solo la visualizza
        
        // Aggiorna posizione con interpolazione
        if (nbt.contains("x")) {
            double targetX = nbt.getDouble("x");
            double targetY = nbt.getDouble("y");
            double targetZ = nbt.getDouble("z");
            
            // Interpolazione smooth
            interpolatePosition(targetX, targetY, targetZ);
        }
    }
    
    /**
     * Interpola la posizione per movimento smooth.
     */
    private void interpolatePosition(double targetX, double targetY, double targetZ) {
        double currentX = entity.getX();
        double currentY = entity.getY();
        double currentZ = entity.getZ();
        
        double distance = Math.sqrt(
            Math.pow(targetX - currentX, 2) +
            Math.pow(targetY - currentY, 2) +
            Math.pow(targetZ - currentZ, 2)
        );
        
        // Se troppo lontano, teleporta
        if (distance > 5.0) {
            entity.setPosition(targetX, targetY, targetZ);
        } else {
            // Interpolazione smooth
            double alpha = 0.3; // Velocità interpolazione
            double newX = currentX + (targetX - currentX) * alpha;
            double newY = currentY + (targetY - currentY) * alpha;
            double newZ = currentZ + (targetZ - currentZ) * alpha;
            entity.setPosition(newX, newY, newZ);
        }
    }
    
    // Getters e setters
    
    public void setSyncInterval(int milliseconds) {
        this.syncInterval = Math.max(20, milliseconds);
    }
    
    public void setEnablePrediction(boolean enable) {
        this.enablePrediction = enable;
    }
    
    public void setMaxPredictionSteps(int steps) {
        this.maxPredictionSteps = Math.max(1, Math.min(10, steps));
    }
    
    public void setUseDeltaCompression(boolean use) {
        this.useDeltaCompression = use;
    }
    
    public boolean isPredictionEnabled() {
        return enablePrediction;
    }
    
    public int getSyncInterval() {
        return syncInterval;
    }
    
    /**
     * Stato di sincronizzazione per ogni player.
     */
    private static class PlayerSyncState {
        @SuppressWarnings("unused")
        long lastSyncTime = 0;
        @SuppressWarnings("unused")
        NbtCompound lastSentState = new NbtCompound();
    }
    
    /**
     * Azione predetta dal client.
     */
    private static class PredictedAction {
        final String action;
        final long timestamp;
        
        PredictedAction(String action, long timestamp) {
            this.action = action;
            this.timestamp = timestamp;
        }
    }
}
