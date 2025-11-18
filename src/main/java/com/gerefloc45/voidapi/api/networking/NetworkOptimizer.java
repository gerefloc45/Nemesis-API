package com.gerefloc45.voidapi.api.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ottimizza l'uso della bandwidth per la sincronizzazione AI.
 * 
 * Tecniche di ottimizzazione:
 * - Delta compression
 * - Adaptive update rate
 * - Priority-based updates
 * - Bandwidth throttling
 * - Data aggregation
 * 
 * @since v0.6.0
 */
public class NetworkOptimizer {
    
    // Configurazione
    private int maxBandwidthPerPlayer = 10240; // bytes/sec (10 KB/s)
    private int minUpdateInterval = 50; // ms
    private int maxUpdateInterval = 500; // ms
    
    // Tracking bandwidth
    private final Map<UUID, BandwidthTracker> playerBandwidth = new ConcurrentHashMap<>();
    
    // Priority queue per aggiornamenti
    private final Map<UUID, PriorityQueue<PendingUpdate>> updateQueues = new ConcurrentHashMap<>();
    
    /**
     * Ottimizza un aggiornamento prima dell'invio.
     * 
     * @param player Player destinatario
     * @param entityId ID entità
     * @param state Stato da inviare
     * @return Stato ottimizzato, o null se da skippare
     */
    public NbtCompound optimizeUpdate(ServerPlayerEntity player, int entityId, NbtCompound state) {
        UUID playerId = player.getUuid();
        BandwidthTracker tracker = playerBandwidth.computeIfAbsent(playerId, k -> new BandwidthTracker());
        
        // Verifica bandwidth disponibile
        if (!tracker.canSend(NetworkPacketHandler.estimatePacketSize(state))) {
            // Bandwidth esaurita, accoda per dopo
            queueUpdate(playerId, entityId, state, UpdatePriority.NORMAL);
            return null;
        }
        
        // Comprimi i dati
        NbtCompound compressed = compressState(state);
        
        // Aggiorna tracker
        int size = NetworkPacketHandler.estimatePacketSize(compressed);
        tracker.recordSent(size);
        
        return compressed;
    }
    
    /**
     * Calcola l'intervallo di aggiornamento ottimale per un'entità.
     * Basato su distanza, importanza e bandwidth disponibile.
     * 
     * @param player Player
     * @param entityDistance Distanza dall'entità
     * @param importance Importanza (0.0-1.0)
     * @return Intervallo in millisecondi
     */
    public int calculateOptimalInterval(ServerPlayerEntity player, double entityDistance, double importance) {
        UUID playerId = player.getUuid();
        BandwidthTracker tracker = playerBandwidth.get(playerId);
        
        // Fattore distanza: più lontano = aggiornamenti meno frequenti
        double distanceFactor = Math.min(1.0, entityDistance / 64.0);
        
        // Fattore bandwidth: meno bandwidth = aggiornamenti meno frequenti
        double bandwidthFactor = tracker != null ? tracker.getUsageRatio() : 0.5;
        
        // Calcola intervallo
        int baseInterval = minUpdateInterval;
        int interval = (int) (baseInterval + (maxUpdateInterval - baseInterval) * 
                             distanceFactor * bandwidthFactor / importance);
        
        return Math.max(minUpdateInterval, Math.min(maxUpdateInterval, interval));
    }
    
    /**
     * Accoda un aggiornamento per invio successivo.
     */
    private void queueUpdate(UUID playerId, int entityId, NbtCompound state, UpdatePriority priority) {
        PriorityQueue<PendingUpdate> queue = updateQueues.computeIfAbsent(
            playerId,
            k -> new PriorityQueue<>(Comparator.comparingInt(u -> u.priority.value))
        );
        
        queue.add(new PendingUpdate(entityId, state, priority, System.currentTimeMillis()));
    }
    
    /**
     * Processa gli aggiornamenti in coda per un player.
     * Chiamato periodicamente.
     * 
     * @param player Player
     * @return Lista di aggiornamenti da inviare
     */
    public List<PendingUpdate> processQueue(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        PriorityQueue<PendingUpdate> queue = updateQueues.get(playerId);
        
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        
        BandwidthTracker tracker = playerBandwidth.get(playerId);
        if (tracker == null) {
            return Collections.emptyList();
        }
        
        List<PendingUpdate> toSend = new ArrayList<>();
        
        while (!queue.isEmpty()) {
            PendingUpdate update = queue.peek();
            int size = NetworkPacketHandler.estimatePacketSize(update.state);
            
            if (!tracker.canSend(size)) {
                break; // Bandwidth esaurita
            }
            
            queue.poll();
            tracker.recordSent(size);
            toSend.add(update);
        }
        
        return toSend;
    }
    
    /**
     * Comprimi lo stato rimuovendo dati non essenziali.
     */
    private NbtCompound compressState(NbtCompound state) {
        NbtCompound compressed = new NbtCompound();
        
        // Copia solo campi essenziali
        for (String key : state.getKeys()) {
            if (isEssentialField(key)) {
                compressed.put(key, state.get(key));
            }
        }
        
        // Arrotonda valori numerici per ridurre precisione
        if (compressed.contains("x")) {
            compressed.putDouble("x", roundToDecimal(compressed.getDouble("x"), 2));
        }
        if (compressed.contains("y")) {
            compressed.putDouble("y", roundToDecimal(compressed.getDouble("y"), 2));
        }
        if (compressed.contains("z")) {
            compressed.putDouble("z", roundToDecimal(compressed.getDouble("z"), 2));
        }
        
        return compressed;
    }
    
    /**
     * Verifica se un campo è essenziale per la sincronizzazione.
     */
    private boolean isEssentialField(String key) {
        return switch (key) {
            case "x", "y", "z", "yaw", "pitch", "currentNode", "status" -> true;
            default -> false;
        };
    }
    
    /**
     * Arrotonda un numero a N decimali.
     */
    private double roundToDecimal(double value, int decimals) {
        double multiplier = Math.pow(10, decimals);
        return Math.round(value * multiplier) / multiplier;
    }
    
    /**
     * Aggiorna i tracker bandwidth (chiamato ogni tick).
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        for (BandwidthTracker tracker : playerBandwidth.values()) {
            tracker.tick(currentTime);
        }
    }
    
    /**
     * Resetta le statistiche per un player.
     */
    public void resetPlayer(UUID playerId) {
        playerBandwidth.remove(playerId);
        updateQueues.remove(playerId);
    }
    
    /**
     * Ottiene le statistiche bandwidth per un player.
     */
    public BandwidthStats getStats(UUID playerId) {
        BandwidthTracker tracker = playerBandwidth.get(playerId);
        if (tracker == null) {
            return new BandwidthStats(0, 0, 0.0);
        }
        
        return new BandwidthStats(
            tracker.bytesSentThisSecond,
            maxBandwidthPerPlayer,
            tracker.getUsageRatio()
        );
    }
    
    // Getters e setters
    
    public void setMaxBandwidthPerPlayer(int bytesPerSecond) {
        this.maxBandwidthPerPlayer = Math.max(1024, bytesPerSecond);
    }
    
    public void setUpdateIntervalRange(int min, int max) {
        this.minUpdateInterval = Math.max(20, min);
        this.maxUpdateInterval = Math.max(min, max);
    }
    
    /**
     * Traccia l'uso della bandwidth per un player.
     */
    private class BandwidthTracker {
        private int bytesSentThisSecond = 0;
        private long lastResetTime = System.currentTimeMillis();
        private final Queue<Integer> recentSizes = new LinkedList<>();
        private static final int HISTORY_SIZE = 10;
        
        boolean canSend(int bytes) {
            return bytesSentThisSecond + bytes <= maxBandwidthPerPlayer;
        }
        
        void recordSent(int bytes) {
            bytesSentThisSecond += bytes;
            recentSizes.add(bytes);
            
            if (recentSizes.size() > HISTORY_SIZE) {
                recentSizes.poll();
            }
        }
        
        void tick(long currentTime) {
            // Reset ogni secondo
            if (currentTime - lastResetTime >= 1000) {
                bytesSentThisSecond = 0;
                lastResetTime = currentTime;
            }
        }
        
        double getUsageRatio() {
            return (double) bytesSentThisSecond / maxBandwidthPerPlayer;
        }
    }
    
    /**
     * Priorità di aggiornamento.
     */
    public enum UpdatePriority {
        CRITICAL(0),  // Deve essere inviato immediatamente
        HIGH(1),      // Importante
        NORMAL(2),    // Standard
        LOW(3);       // Può aspettare
        
        final int value;
        
        UpdatePriority(int value) {
            this.value = value;
        }
    }
    
    /**
     * Aggiornamento in coda.
     */
    public static class PendingUpdate {
        public final int entityId;
        public final NbtCompound state;
        public final UpdatePriority priority;
        public final long queuedTime;
        
        PendingUpdate(int entityId, NbtCompound state, UpdatePriority priority, long queuedTime) {
            this.entityId = entityId;
            this.state = state;
            this.priority = priority;
            this.queuedTime = queuedTime;
        }
    }
    
    /**
     * Statistiche bandwidth.
     */
    public record BandwidthStats(int bytesUsed, int bytesMax, double usageRatio) {
        public String toReadableString() {
            return String.format("%.2f KB / %.2f KB (%.1f%%)", 
                bytesUsed / 1024.0, 
                bytesMax / 1024.0, 
                usageRatio * 100);
        }
    }
}
