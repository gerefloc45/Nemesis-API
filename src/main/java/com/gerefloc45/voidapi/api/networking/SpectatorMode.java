package com.gerefloc45.voidapi.api.networking;

import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modalità spettatore per osservare le decisioni AI in tempo reale.
 * Permette ai player di "guardare dentro" l'AI di un'entità.
 * 
 * Features:
 * - Visualizzazione decisioni AI live
 * - Stato behavior tree in tempo reale
 * - Valori blackboard
 * - Statistiche performance
 * - Timeline delle decisioni
 * 
 * @since v0.6.0
 */
public class SpectatorMode {
    
    private static final SpectatorMode INSTANCE = new SpectatorMode();
    
    // Player che stanno spettando
    private final Map<UUID, SpectatorSession> activeSessions = new ConcurrentHashMap<>();
    
    // Entità sotto osservazione
    private final Map<Integer, Set<UUID>> entitySpectators = new ConcurrentHashMap<>(); // entity ID -> spectators
    
    private SpectatorMode() {}
    
    public static SpectatorMode getInstance() {
        return INSTANCE;
    }
    
    /**
     * Inizia una sessione di spettatore.
     * 
     * @param player Player spettatore
     * @param entity Entità da osservare
     * @param tree Behavior tree dell'entità
     * @param blackboard Blackboard dell'entità
     * @return true se iniziata con successo
     */
    public boolean startSpectating(ServerPlayerEntity player, LivingEntity entity, 
                                   BehaviorTree tree, Blackboard blackboard) {
        
        UUID playerId = player.getUuid();
        int entityId = entity.getId();
        
        // Verifica se già sta spettando
        if (activeSessions.containsKey(playerId)) {
            stopSpectating(player);
        }
        
        // Crea sessione
        SpectatorSession session = new SpectatorSession(
            playerId,
            entityId,
            entity,
            tree,
            blackboard,
            System.currentTimeMillis()
        );
        
        activeSessions.put(playerId, session);
        entitySpectators.computeIfAbsent(entityId, k -> new HashSet<>()).add(playerId);
        
        // Invia stato iniziale
        sendInitialState(player, session);
        
        // Notifica player
        player.sendMessage(Text.literal("§aModalità spettatore attivata per " + entity.getName().getString()), false);
        
        return true;
    }
    
    /**
     * Ferma una sessione di spettatore.
     * 
     * @param player Player spettatore
     */
    public void stopSpectating(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        SpectatorSession session = activeSessions.remove(playerId);
        
        if (session != null) {
            Set<UUID> spectators = entitySpectators.get(session.entityId);
            if (spectators != null) {
                spectators.remove(playerId);
                if (spectators.isEmpty()) {
                    entitySpectators.remove(session.entityId);
                }
            }
            
            player.sendMessage(Text.literal("§cModalità spettatore disattivata"), false);
        }
    }
    
    /**
     * Aggiorna tutti gli spettatori attivi.
     * Chiamato ogni tick.
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        for (SpectatorSession session : activeSessions.values()) {
            // Aggiorna ogni 100ms (10 volte al secondo)
            if (currentTime - session.lastUpdateTime >= 100) {
                updateSpectator(session);
                session.lastUpdateTime = currentTime;
            }
        }
    }
    
    /**
     * Notifica un evento AI agli spettatori.
     * 
     * @param entityId ID entità
     * @param eventType Tipo evento
     * @param eventData Dati evento
     */
    public void notifyEvent(int entityId, String eventType, NbtCompound eventData) {
        Set<UUID> spectators = entitySpectators.get(entityId);
        if (spectators == null || spectators.isEmpty()) {
            return;
        }
        
        for (UUID spectatorId : spectators) {
            SpectatorSession session = activeSessions.get(spectatorId);
            if (session != null) {
                session.addEvent(eventType, eventData);
            }
        }
    }
    
    /**
     * Invia lo stato iniziale a uno spettatore.
     */
    private void sendInitialState(ServerPlayerEntity player, SpectatorSession session) {
        NbtCompound data = new NbtCompound();
        
        data.putInt("entityId", session.entityId);
        data.putString("entityName", session.entity.getName().getString());
        data.putString("entityType", session.entity.getType().toString());
        
        // Stato behavior tree
        NbtCompound treeData = new NbtCompound();
        treeData.putString("rootBehavior", session.tree.getRootBehavior().getClass().getSimpleName());
        treeData.putString("status", session.tree.getLastStatus().toString());
        treeData.putBoolean("running", session.tree.isRunning());
        data.put("tree", treeData);
        
        // Blackboard
        NbtCompound blackboardData = serializeBlackboard(session.blackboard);
        data.put("blackboard", blackboardData);
        
        NetworkPacketHandler.sendSpectatePacket(player, data);
    }
    
    /**
     * Aggiorna uno spettatore con i dati correnti.
     */
    private void updateSpectator(SpectatorSession session) {
        // Trova il player
        ServerPlayerEntity player = findPlayer(session.playerId);
        if (player == null) {
            activeSessions.remove(session.playerId);
            return;
        }
        
        // Verifica che l'entità sia ancora valida
        if (session.entity.isRemoved() || !session.entity.isAlive()) {
            stopSpectating(player);
            return;
        }
        
        // Crea update
        NbtCompound data = new NbtCompound();
        data.putLong("timestamp", System.currentTimeMillis());
        
        // Stato corrente
        NbtCompound currentState = new NbtCompound();
        currentState.putString("status", session.tree.getLastStatus().toString());
        currentState.putBoolean("running", session.tree.isRunning());
        data.put("state", currentState);
        
        // Blackboard changes
        NbtCompound blackboardData = serializeBlackboard(session.blackboard);
        data.put("blackboard", blackboardData);
        
        // Eventi recenti
        if (!session.recentEvents.isEmpty()) {
            NbtCompound eventsData = new NbtCompound();
            int i = 0;
            for (AIEvent event : session.recentEvents) {
                NbtCompound eventData = new NbtCompound();
                eventData.putString("type", event.type);
                eventData.putLong("time", event.timestamp);
                eventData.put("data", event.data);
                eventsData.put("event_" + i++, eventData);
            }
            data.put("events", eventsData);
            session.recentEvents.clear();
        }
        
        // Statistiche
        NbtCompound stats = new NbtCompound();
        stats.putLong("sessionDuration", System.currentTimeMillis() - session.startTime);
        stats.putInt("eventCount", session.totalEvents);
        data.put("stats", stats);
        
        NetworkPacketHandler.sendSpectatePacket(player, data);
    }
    
    /**
     * Serializza il blackboard per visualizzazione.
     */
    private NbtCompound serializeBlackboard(Blackboard blackboard) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("size", blackboard.size());
        
        // In un sistema reale, itererebbe sui valori del blackboard
        // Per ora placeholder
        
        return nbt;
    }
    
    /**
     * Trova un player per UUID.
     */
    private ServerPlayerEntity findPlayer(UUID playerId) {
        // Implementazione semplificata
        // In produzione, userebbe il server per trovare il player
        return null;
    }
    
    /**
     * Verifica se un'entità è sotto osservazione.
     */
    public boolean isBeingSpectated(int entityId) {
        Set<UUID> spectators = entitySpectators.get(entityId);
        return spectators != null && !spectators.isEmpty();
    }
    
    /**
     * Ottiene il numero di spettatori per un'entità.
     */
    public int getSpectatorCount(int entityId) {
        Set<UUID> spectators = entitySpectators.get(entityId);
        return spectators != null ? spectators.size() : 0;
    }
    
    /**
     * Ottiene la sessione attiva per un player.
     */
    public SpectatorSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    /**
     * Sessione di spettatore attiva.
     */
    public static class SpectatorSession {
        public final UUID playerId;
        public final int entityId;
        public final LivingEntity entity;
        public final BehaviorTree tree;
        public final Blackboard blackboard;
        public final long startTime;
        
        public long lastUpdateTime;
        public final Queue<AIEvent> recentEvents = new LinkedList<>();
        public int totalEvents = 0;
        
        private static final int MAX_RECENT_EVENTS = 20;
        
        public SpectatorSession(UUID playerId, int entityId, LivingEntity entity,
                               BehaviorTree tree, Blackboard blackboard, long startTime) {
            this.playerId = playerId;
            this.entityId = entityId;
            this.entity = entity;
            this.tree = tree;
            this.blackboard = blackboard;
            this.startTime = startTime;
            this.lastUpdateTime = startTime;
        }
        
        public void addEvent(String type, NbtCompound data) {
            AIEvent event = new AIEvent(type, data, System.currentTimeMillis());
            recentEvents.add(event);
            totalEvents++;
            
            // Mantieni solo gli eventi recenti
            while (recentEvents.size() > MAX_RECENT_EVENTS) {
                recentEvents.poll();
            }
        }
    }
    
    /**
     * Evento AI registrato.
     */
    private static class AIEvent {
        final String type;
        final NbtCompound data;
        final long timestamp;
        
        AIEvent(String type, NbtCompound data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
