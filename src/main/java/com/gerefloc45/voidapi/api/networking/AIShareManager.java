package com.gerefloc45.voidapi.api.networking;

import com.gerefloc45.voidapi.api.BehaviorTree;
import com.gerefloc45.voidapi.api.Blackboard;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce la condivisione di configurazioni AI tra player.
 * Permette ai player di condividere, importare ed esportare AI.
 * 
 * Features:
 * - Esporta AI come template
 * - Importa AI da template
 * - Condividi AI con altri player
 * - Libreria AI condivise
 * - Permessi e controllo accessi
 * 
 * @since v0.6.0
 */
public class AIShareManager {
    
    private static final AIShareManager INSTANCE = new AIShareManager();
    
    // Storage AI condivise
    private final Map<UUID, SharedAI> sharedAIs = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerShares = new ConcurrentHashMap<>(); // player -> shared AI IDs
    
    // Permessi
    private final Map<UUID, Set<UUID>> accessPermissions = new ConcurrentHashMap<>(); // AI ID -> allowed players
    
    private AIShareManager() {}
    
    public static AIShareManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Esporta un'AI come template condivisibile.
     * 
     * @param entity Entità con l'AI
     * @param tree Behavior tree
     * @param blackboard Blackboard
     * @param owner Player proprietario
     * @param name Nome del template
     * @param description Descrizione
     * @return UUID del template creato
     */
    public UUID exportAI(LivingEntity entity, BehaviorTree tree, Blackboard blackboard, 
                         ServerPlayerEntity owner, String name, String description) {
        
        UUID aiId = UUID.randomUUID();
        
        // Serializza l'AI
        NbtCompound aiData = new NbtCompound();
        aiData.putString("name", name);
        aiData.putString("description", description);
        aiData.putString("entityType", entity.getType().toString());
        aiData.putString("treeClass", tree.getRootBehavior().getClass().getName());
        
        // Serializza configurazione (semplificata)
        NbtCompound config = new NbtCompound();
        serializeBlackboardConfig(blackboard, config);
        aiData.put("config", config);
        
        // Crea shared AI
        SharedAI sharedAI = new SharedAI(
            aiId,
            owner.getUuid(),
            name,
            description,
            aiData,
            System.currentTimeMillis()
        );
        
        sharedAIs.put(aiId, sharedAI);
        playerShares.computeIfAbsent(owner.getUuid(), k -> new HashSet<>()).add(aiId);
        
        // Owner ha sempre accesso
        accessPermissions.computeIfAbsent(aiId, k -> new HashSet<>()).add(owner.getUuid());
        
        return aiId;
    }
    
    /**
     * Importa un'AI da un template.
     * 
     * @param aiId ID del template
     * @param requester Player che richiede l'import
     * @return Dati AI se autorizzato, null altrimenti
     */
    public NbtCompound importAI(UUID aiId, ServerPlayerEntity requester) {
        SharedAI sharedAI = sharedAIs.get(aiId);
        if (sharedAI == null) {
            return null;
        }
        
        // Verifica permessi
        if (!hasAccess(aiId, requester.getUuid())) {
            return null;
        }
        
        // Incrementa contatore download
        sharedAI.downloadCount++;
        
        return sharedAI.aiData.copy();
    }
    
    /**
     * Condivide un'AI con un altro player.
     * 
     * @param aiId ID dell'AI
     * @param owner Proprietario
     * @param target Player target
     * @return true se condivisa con successo
     */
    public boolean shareWithPlayer(UUID aiId, ServerPlayerEntity owner, ServerPlayerEntity target) {
        SharedAI sharedAI = sharedAIs.get(aiId);
        if (sharedAI == null || !sharedAI.ownerId.equals(owner.getUuid())) {
            return false;
        }
        
        // Aggiungi permesso
        accessPermissions.computeIfAbsent(aiId, k -> new HashSet<>()).add(target.getUuid());
        
        // Notifica il target
        NetworkPacketHandler.sendAISharePacket(target, createShareNotification(sharedAI));
        
        return true;
    }
    
    /**
     * Rende un'AI pubblica (accessibile a tutti).
     * 
     * @param aiId ID dell'AI
     * @param owner Proprietario
     * @return true se resa pubblica con successo
     */
    public boolean makePublic(UUID aiId, ServerPlayerEntity owner) {
        SharedAI sharedAI = sharedAIs.get(aiId);
        if (sharedAI == null || !sharedAI.ownerId.equals(owner.getUuid())) {
            return false;
        }
        
        sharedAI.isPublic = true;
        return true;
    }
    
    /**
     * Ottiene tutte le AI condivise da un player.
     * 
     * @param playerId UUID del player
     * @return Lista di AI condivise
     */
    public List<SharedAI> getPlayerSharedAIs(UUID playerId) {
        Set<UUID> aiIds = playerShares.get(playerId);
        if (aiIds == null) {
            return Collections.emptyList();
        }
        
        List<SharedAI> result = new ArrayList<>();
        for (UUID aiId : aiIds) {
            SharedAI ai = sharedAIs.get(aiId);
            if (ai != null) {
                result.add(ai);
            }
        }
        
        return result;
    }
    
    /**
     * Ottiene tutte le AI pubbliche.
     * 
     * @return Lista di AI pubbliche
     */
    public List<SharedAI> getPublicAIs() {
        List<SharedAI> result = new ArrayList<>();
        for (SharedAI ai : sharedAIs.values()) {
            if (ai.isPublic) {
                result.add(ai);
            }
        }
        return result;
    }
    
    /**
     * Ottiene le AI accessibili da un player.
     * 
     * @param playerId UUID del player
     * @return Lista di AI accessibili
     */
    public List<SharedAI> getAccessibleAIs(UUID playerId) {
        List<SharedAI> result = new ArrayList<>();
        
        for (SharedAI ai : sharedAIs.values()) {
            if (ai.isPublic || hasAccess(ai.id, playerId)) {
                result.add(ai);
            }
        }
        
        return result;
    }
    
    /**
     * Cerca AI per nome o descrizione.
     * 
     * @param query Query di ricerca
     * @param playerId Player che effettua la ricerca
     * @return Lista di AI trovate
     */
    public List<SharedAI> searchAIs(String query, UUID playerId) {
        String lowerQuery = query.toLowerCase();
        List<SharedAI> result = new ArrayList<>();
        
        for (SharedAI ai : sharedAIs.values()) {
            if (!ai.isPublic && !hasAccess(ai.id, playerId)) {
                continue;
            }
            
            if (ai.name.toLowerCase().contains(lowerQuery) || 
                ai.description.toLowerCase().contains(lowerQuery)) {
                result.add(ai);
            }
        }
        
        return result;
    }
    
    /**
     * Elimina un'AI condivisa.
     * 
     * @param aiId ID dell'AI
     * @param owner Proprietario
     * @return true se eliminata con successo
     */
    public boolean deleteSharedAI(UUID aiId, ServerPlayerEntity owner) {
        SharedAI sharedAI = sharedAIs.get(aiId);
        if (sharedAI == null || !sharedAI.ownerId.equals(owner.getUuid())) {
            return false;
        }
        
        sharedAIs.remove(aiId);
        playerShares.get(owner.getUuid()).remove(aiId);
        accessPermissions.remove(aiId);
        
        return true;
    }
    
    /**
     * Verifica se un player ha accesso a un'AI.
     */
    private boolean hasAccess(UUID aiId, UUID playerId) {
        SharedAI ai = sharedAIs.get(aiId);
        if (ai == null) {
            return false;
        }
        
        if (ai.isPublic || ai.ownerId.equals(playerId)) {
            return true;
        }
        
        Set<UUID> allowed = accessPermissions.get(aiId);
        return allowed != null && allowed.contains(playerId);
    }
    
    /**
     * Serializza la configurazione del blackboard.
     */
    private void serializeBlackboardConfig(Blackboard blackboard, NbtCompound nbt) {
        // Serializza solo configurazioni, non dati runtime
        // In un sistema reale, serializzerebbe parametri configurabili
        nbt.putInt("size", blackboard.size());
    }
    
    /**
     * Crea una notifica di condivisione.
     */
    private NbtCompound createShareNotification(SharedAI ai) {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("aiId", ai.id);
        nbt.putString("name", ai.name);
        nbt.putString("description", ai.description);
        nbt.putString("owner", ai.ownerId.toString());
        return nbt;
    }
    
    /**
     * Rappresenta un'AI condivisa.
     */
    public static class SharedAI {
        public final UUID id;
        public final UUID ownerId;
        public final String name;
        public final String description;
        public final NbtCompound aiData;
        public final long createdTime;
        
        public boolean isPublic = false;
        public int downloadCount = 0;
        public int rating = 0;
        public int ratingCount = 0;
        
        public SharedAI(UUID id, UUID ownerId, String name, String description, 
                       NbtCompound aiData, long createdTime) {
            this.id = id;
            this.ownerId = ownerId;
            this.name = name;
            this.description = description;
            this.aiData = aiData;
            this.createdTime = createdTime;
        }
        
        /**
         * Aggiunge una valutazione.
         */
        public void addRating(int stars) {
            if (stars < 1 || stars > 5) return;
            
            rating = (rating * ratingCount + stars) / (ratingCount + 1);
            ratingCount++;
        }
        
        /**
         * Ottiene il rating medio.
         */
        public double getAverageRating() {
            return ratingCount > 0 ? (double) rating / ratingCount : 0.0;
        }
    }
}
