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
 * Sistema di debug remoto per l'AI.
 * Permette di debuggare l'AI di entità remote attraverso la rete.
 * 
 * Features:
 * - Breakpoint su nodi behavior tree
 * - Step-by-step execution
 * - Inspect variabili blackboard
 * - Modifica runtime valori
 * - Logging remoto
 * - Performance profiling
 * 
 * @since v0.6.0
 */
public class RemoteDebugger {
    
    private static final RemoteDebugger INSTANCE = new RemoteDebugger();
    
    // Sessioni di debug attive
    private final Map<UUID, DebugSession> debugSessions = new ConcurrentHashMap<>();
    
    // Breakpoint attivi
    private final Map<Integer, Set<String>> breakpoints = new ConcurrentHashMap<>(); // entity ID -> node names
    
    // Entità in pausa
    private final Set<Integer> pausedEntities = ConcurrentHashMap.newKeySet();
    
    private RemoteDebugger() {}
    
    public static RemoteDebugger getInstance() {
        return INSTANCE;
    }
    
    /**
     * Inizia una sessione di debug.
     * 
     * @param player Player debugger
     * @param entity Entità da debuggare
     * @param tree Behavior tree
     * @param blackboard Blackboard
     * @return true se iniziata con successo
     */
    public boolean startDebugSession(ServerPlayerEntity player, LivingEntity entity,
                                     BehaviorTree tree, Blackboard blackboard) {
        
        UUID playerId = player.getUuid();
        int entityId = entity.getId();
        
        // Verifica permessi (in produzione)
        if (!hasDebugPermission(player)) {
            player.sendMessage(Text.literal("§cNon hai i permessi per debuggare"), false);
            return false;
        }
        
        // Chiudi sessione precedente se esiste
        if (debugSessions.containsKey(playerId)) {
            stopDebugSession(player);
        }
        
        // Crea sessione
        DebugSession session = new DebugSession(
            playerId,
            entityId,
            entity,
            tree,
            blackboard,
            System.currentTimeMillis()
        );
        
        debugSessions.put(playerId, session);
        
        // Invia stato iniziale
        sendDebugInfo(player, session);
        
        player.sendMessage(Text.literal("§aDebug remoto attivato per " + entity.getName().getString()), false);
        
        return true;
    }
    
    /**
     * Ferma una sessione di debug.
     */
    public void stopDebugSession(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        DebugSession session = debugSessions.remove(playerId);
        
        if (session != null) {
            // Rimuovi breakpoint
            breakpoints.remove(session.entityId);
            
            // Riprendi esecuzione se in pausa
            if (pausedEntities.contains(session.entityId)) {
                resumeExecution(session.entityId);
            }
            
            player.sendMessage(Text.literal("§cDebug remoto disattivato"), false);
        }
    }
    
    /**
     * Aggiunge un breakpoint su un nodo.
     * 
     * @param player Player
     * @param nodeName Nome del nodo
     */
    public void addBreakpoint(ServerPlayerEntity player, String nodeName) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) {
            player.sendMessage(Text.literal("§cNessuna sessione di debug attiva"), false);
            return;
        }
        
        breakpoints.computeIfAbsent(session.entityId, k -> new HashSet<>()).add(nodeName);
        session.breakpoints.add(nodeName);
        
        player.sendMessage(Text.literal("§aBreakpoint aggiunto su: " + nodeName), false);
    }
    
    /**
     * Rimuove un breakpoint.
     */
    public void removeBreakpoint(ServerPlayerEntity player, String nodeName) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) return;
        
        Set<String> entityBreakpoints = breakpoints.get(session.entityId);
        if (entityBreakpoints != null) {
            entityBreakpoints.remove(nodeName);
        }
        session.breakpoints.remove(nodeName);
        
        player.sendMessage(Text.literal("§cBreakpoint rimosso da: " + nodeName), false);
    }
    
    /**
     * Mette in pausa l'esecuzione dell'AI.
     */
    public void pauseExecution(ServerPlayerEntity player) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) return;
        
        pausedEntities.add(session.entityId);
        session.isPaused = true;
        
        player.sendMessage(Text.literal("§eEsecuzione in pausa"), false);
    }
    
    /**
     * Riprende l'esecuzione dell'AI.
     */
    public void resumeExecution(int entityId) {
        pausedEntities.remove(entityId);
        
        // Notifica tutte le sessioni che debuggano questa entità
        for (DebugSession session : debugSessions.values()) {
            if (session.entityId == entityId) {
                session.isPaused = false;
            }
        }
    }
    
    /**
     * Esegue un singolo step.
     */
    public void stepExecution(ServerPlayerEntity player) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) return;
        
        session.stepMode = true;
        pausedEntities.remove(session.entityId);
        
        player.sendMessage(Text.literal("§eEsecuzione di un singolo step"), false);
    }
    
    /**
     * Verifica se un'entità deve essere messa in pausa.
     * Chiamato prima di eseguire un nodo.
     * 
     * @param entityId ID entità
     * @param nodeName Nome del nodo
     * @return true se deve essere messa in pausa
     */
    public boolean shouldPause(int entityId, String nodeName) {
        // Verifica se in pausa
        if (pausedEntities.contains(entityId)) {
            return true;
        }
        
        // Verifica breakpoint
        Set<String> entityBreakpoints = breakpoints.get(entityId);
        if (entityBreakpoints != null && entityBreakpoints.contains(nodeName)) {
            pausedEntities.add(entityId);
            notifyBreakpointHit(entityId, nodeName);
            return true;
        }
        
        // Verifica step mode
        for (DebugSession session : debugSessions.values()) {
            if (session.entityId == entityId && session.stepMode) {
                session.stepMode = false;
                pausedEntities.add(entityId);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Notifica che un breakpoint è stato raggiunto.
     */
    private void notifyBreakpointHit(int entityId, String nodeName) {
        for (DebugSession session : debugSessions.values()) {
            if (session.entityId == entityId) {
                NbtCompound data = new NbtCompound();
                data.putString("type", "breakpoint_hit");
                data.putString("node", nodeName);
                data.putLong("timestamp", System.currentTimeMillis());
                
                // Trova player e invia notifica
                ServerPlayerEntity player = findPlayer(session.playerId);
                if (player != null) {
                    NetworkPacketHandler.sendDebugPacket(player, data);
                    player.sendMessage(Text.literal("§eBreakpoint raggiunto: " + nodeName), false);
                }
            }
        }
    }
    
    /**
     * Modifica un valore nel blackboard.
     * 
     * @param player Player
     * @param key Chiave
     * @param value Valore (come stringa)
     */
    public void setBlackboardValue(ServerPlayerEntity player, String key, String value) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) return;
        
        // Converti e imposta il valore
        session.blackboard.set(key, value);
        
        player.sendMessage(Text.literal("§aValore impostato: " + key + " = " + value), false);
        
        // Invia aggiornamento
        sendDebugInfo(player, session);
    }
    
    /**
     * Ottiene un valore dal blackboard.
     */
    public String getBlackboardValue(ServerPlayerEntity player, String key) {
        DebugSession session = debugSessions.get(player.getUuid());
        if (session == null) return null;
        
        Optional<Object> value = session.blackboard.get(key);
        return value.map(Object::toString).orElse(null);
    }
    
    /**
     * Invia informazioni di debug al player.
     */
    private void sendDebugInfo(ServerPlayerEntity player, DebugSession session) {
        NbtCompound data = new NbtCompound();
        
        data.putInt("entityId", session.entityId);
        data.putString("entityName", session.entity.getName().getString());
        
        // Stato behavior tree
        NbtCompound treeData = new NbtCompound();
        treeData.putString("rootBehavior", session.tree.getRootBehavior().getClass().getSimpleName());
        treeData.putString("status", session.tree.getLastStatus().toString());
        treeData.putBoolean("running", session.tree.isRunning());
        treeData.putBoolean("paused", session.isPaused);
        data.put("tree", treeData);
        
        // Breakpoint
        NbtCompound breakpointsData = new NbtCompound();
        int i = 0;
        for (String bp : session.breakpoints) {
            breakpointsData.putString("bp_" + i++, bp);
        }
        data.put("breakpoints", breakpointsData);
        
        // Statistiche
        NbtCompound stats = new NbtCompound();
        stats.putLong("sessionDuration", System.currentTimeMillis() - session.startTime);
        stats.putInt("breakpointCount", session.breakpoints.size());
        data.put("stats", stats);
        
        NetworkPacketHandler.sendDebugPacket(player, data);
    }
    
    /**
     * Aggiorna tutte le sessioni di debug.
     * Chiamato ogni tick.
     */
    public void tick() {
        for (DebugSession session : debugSessions.values()) {
            ServerPlayerEntity player = findPlayer(session.playerId);
            if (player != null) {
                sendDebugInfo(player, session);
            }
        }
    }
    
    /**
     * Verifica se un player ha i permessi di debug.
     */
    private boolean hasDebugPermission(ServerPlayerEntity player) {
        // In produzione, verificherebbe i permessi reali
        return player.hasPermissionLevel(2); // OP level 2+
    }
    
    /**
     * Trova un player per UUID.
     */
    private ServerPlayerEntity findPlayer(UUID playerId) {
        // Implementazione semplificata
        return null;
    }
    
    /**
     * Verifica se un'entità è in debug.
     */
    public boolean isDebugging(int entityId) {
        for (DebugSession session : debugSessions.values()) {
            if (session.entityId == entityId) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica se un'entità è in pausa.
     */
    public boolean isPaused(int entityId) {
        return pausedEntities.contains(entityId);
    }
    
    /**
     * Sessione di debug attiva.
     */
    public static class DebugSession {
        public final UUID playerId;
        public final int entityId;
        public final LivingEntity entity;
        public final BehaviorTree tree;
        public final Blackboard blackboard;
        public final long startTime;
        
        public final Set<String> breakpoints = new HashSet<>();
        public boolean isPaused = false;
        public boolean stepMode = false;
        
        public DebugSession(UUID playerId, int entityId, LivingEntity entity,
                           BehaviorTree tree, Blackboard blackboard, long startTime) {
            this.playerId = playerId;
            this.entityId = entityId;
            this.entity = entity;
            this.tree = tree;
            this.blackboard = blackboard;
            this.startTime = startTime;
        }
    }
}
