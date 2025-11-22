package com.gerefloc45.voidapi.api.perception;

import com.gerefloc45.voidapi.api.BehaviorContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Touch sensor for detecting physical contact with entities and blocks.
 * Detects collisions and provides information about contact points, forces, and
 * directions.
 * 
 * @author VoidAPI Framework
 * @version 0.7.0
 */
public class TouchSensor implements Sensor {
    private final double detectionRadius;
    private final String blackboardKey;
    private final int updateFrequency;
    private final boolean detectEntities;
    private final boolean detectBlocks;

    // Contact tracking
    private final Map<UUID, ContactData> activeContacts;
    private Vec3d lastPosition;
    private Vec3d lastVelocity;

    /**
     * Creates a basic touch sensor.
     *
     * @param detectionRadius Detection radius in blocks
     * @param blackboardKey   Key to store contact data in blackboard
     */
    public TouchSensor(double detectionRadius, String blackboardKey) {
        this(detectionRadius, blackboardKey, true, true, 1);
    }

    /**
     * Creates a touch sensor with custom configuration.
     *
     * @param detectionRadius Detection radius in blocks
     * @param blackboardKey   Key to store contact data in blackboard
     * @param detectEntities  Detect entity contacts
     * @param detectBlocks    Detect block contacts
     * @param updateFrequency Update frequency in ticks
     */
    public TouchSensor(double detectionRadius, String blackboardKey,
            boolean detectEntities, boolean detectBlocks, int updateFrequency) {
        this.detectionRadius = detectionRadius;
        this.blackboardKey = blackboardKey;
        this.detectEntities = detectEntities;
        this.detectBlocks = detectBlocks;
        this.updateFrequency = updateFrequency;
        this.activeContacts = new HashMap<>();
        this.lastPosition = null;
        this.lastVelocity = Vec3d.ZERO;
    }

    @Override
    public void update(BehaviorContext context) {
        LivingEntity entity = context.getEntity();
        Vec3d currentPos = entity.getPos();
        Vec3d currentVelocity = entity.getVelocity();

        // Initialize on first update
        if (lastPosition == null) {
            lastPosition = currentPos;
        }

        List<ContactData> newContacts = new ArrayList<>();

        // Detect entity contacts
        if (detectEntities) {
            detectEntityContacts(entity, newContacts, currentVelocity);
        }

        // Detect block contacts
        if (detectBlocks) {
            detectBlockContacts(entity, newContacts);
        }

        // Update active contacts map
        activeContacts.clear();
        for (ContactData contact : newContacts) {
            if (contact.entityUuid != null) {
                activeContacts.put(contact.entityUuid, contact);
            }
        }

        // Store in blackboard
        context.getBlackboard().set(blackboardKey + "_contacts", newContacts);
        context.getBlackboard().set(blackboardKey + "_contact_count", newContacts.size());
        context.getBlackboard().set(blackboardKey + "_has_contact", !newContacts.isEmpty());

        // Calculate strongest contact
        if (!newContacts.isEmpty()) {
            ContactData strongest = newContacts.stream()
                    .max(Comparator.comparingDouble(c -> c.impactForce))
                    .orElse(null);
            context.getBlackboard().set(blackboardKey + "_strongest", strongest);
        }

        // Update tracking
        lastPosition = currentPos;
        lastVelocity = currentVelocity;
    }

    /**
     * Detects contacts with other entities.
     *
     * @param observer The observing entity
     * @param contacts List to add detected contacts to
     * @param velocity Current velocity for impact calculation
     */
    private void detectEntityContacts(LivingEntity observer, List<ContactData> contacts,
            Vec3d velocity) {
        List<Entity> nearbyEntities = observer.getWorld().getOtherEntities(
                observer,
                observer.getBoundingBox().expand(detectionRadius));

        for (Entity other : nearbyEntities) {
            // Check if bounding boxes intersect (collision)
            if (observer.getBoundingBox().intersects(other.getBoundingBox())) {
                Vec3d contactPoint = calculateContactPoint(observer, other);
                Vec3d contactNormal = calculateContactNormal(observer.getPos(), other.getPos());
                double impactForce = calculateImpactForce(velocity, other.getVelocity());

                UUID entityUuid = other instanceof LivingEntity ? other.getUuid() : null;

                contacts.add(new ContactData(
                        ContactType.ENTITY,
                        contactPoint,
                        contactNormal,
                        impactForce,
                        entityUuid,
                        other));
            }
        }
    }

    /**
     * Detects contacts with blocks.
     *
     * @param entity   The entity
     * @param contacts List to add detected contacts to
     */
    private void detectBlockContacts(LivingEntity entity, List<ContactData> contacts) {
        // Check collisions with blocks using entity's collision state
        boolean onGround = entity.isOnGround();
        boolean horizontalCollision = entity.horizontalCollision;
        boolean verticalCollision = entity.verticalCollision;

        Vec3d pos = entity.getPos();

        if (onGround) {
            // Ground contact
            Vec3d contactPoint = new Vec3d(pos.x, entity.getBoundingBox().minY, pos.z);
            contacts.add(new ContactData(
                    ContactType.GROUND,
                    contactPoint,
                    new Vec3d(0, 1, 0), // Normal pointing up
                    Math.abs(lastVelocity.y),
                    null,
                    null));
        }

        if (horizontalCollision) {
            // Wall contact - use velocity direction to estimate normal
            Vec3d horizontalVel = new Vec3d(lastVelocity.x, 0, lastVelocity.z);
            if (horizontalVel.length() > 0.01) {
                Vec3d normal = horizontalVel.normalize().negate();
                contacts.add(new ContactData(
                        ContactType.WALL,
                        pos,
                        normal,
                        horizontalVel.length(),
                        null,
                        null));
            }
        }

        if (verticalCollision && !onGround) {
            // Ceiling contact
            Vec3d contactPoint = new Vec3d(pos.x, entity.getBoundingBox().maxY, pos.z);
            contacts.add(new ContactData(
                    ContactType.CEILING,
                    contactPoint,
                    new Vec3d(0, -1, 0), // Normal pointing down
                    Math.abs(lastVelocity.y),
                    null,
                    null));
        }
    }

    /**
     * Calculates the contact point between two entities.
     *
     * @param entity1 First entity
     * @param entity2 Second entity
     * @return Contact point (average of closest points)
     */
    private Vec3d calculateContactPoint(Entity entity1, Entity entity2) {
        // Simplified: use center point between entities
        return entity1.getPos().add(entity2.getPos()).multiply(0.5);
    }

    /**
     * Calculates the contact normal (direction from entity1 to entity2).
     *
     * @param pos1 First position
     * @param pos2 Second position
     * @return Normalized contact normal
     */
    private Vec3d calculateContactNormal(Vec3d pos1, Vec3d pos2) {
        Vec3d diff = pos2.subtract(pos1);
        double length = diff.length();
        return length > 0.001 ? diff.multiply(1.0 / length) : new Vec3d(0, 1, 0);
    }

    /**
     * Calculates impact force based on relative velocity.
     *
     * @param vel1 First entity velocity
     * @param vel2 Second entity velocity
     * @return Impact force magnitude
     */
    private double calculateImpactForce(Vec3d vel1, Vec3d vel2) {
        Vec3d relativeVel = vel1.subtract(vel2);
        return relativeVel.length();
    }

    @Override
    public double getRange() {
        return detectionRadius;
    }

    @Override
    public int getUpdateFrequency() {
        return updateFrequency;
    }

    @Override
    public void reset(BehaviorContext context) {
        context.getBlackboard().remove(blackboardKey + "_contacts");
        context.getBlackboard().remove(blackboardKey + "_contact_count");
        context.getBlackboard().remove(blackboardKey + "_has_contact");
        context.getBlackboard().remove(blackboardKey + "_strongest");
        activeContacts.clear();
        lastPosition = null;
        lastVelocity = Vec3d.ZERO;
    }

    /**
     * Gets all current contacts.
     *
     * @param context Behavior context
     * @return List of contacts
     */
    public List<ContactData> getContacts(BehaviorContext context) {
        return context.getBlackboard()
                .<List<ContactData>>get(blackboardKey + "_contacts")
                .orElse(new ArrayList<>());
    }

    /**
     * Gets the strongest current contact.
     *
     * @param context Behavior context
     * @return Strongest contact, or null if no contacts
     */
    public ContactData getStrongestContact(BehaviorContext context) {
        return context.getBlackboard()
                .<ContactData>get(blackboardKey + "_strongest")
                .orElse(null);
    }

    /**
     * Checks if there's any contact.
     *
     * @param context Behavior context
     * @return True if contact detected
     */
    public boolean hasContact(BehaviorContext context) {
        return context.getBlackboard()
                .<Boolean>get(blackboardKey + "_has_contact")
                .orElse(false);
    }

    /**
     * Contact type enumeration.
     */
    public enum ContactType {
        ENTITY, // Contact with another entity
        GROUND, // Contact with ground
        WALL, // Contact with wall
        CEILING // Contact with ceiling
    }

    /**
     * Contact data.
     */
    public static class ContactData {
        public final ContactType type;
        public final Vec3d contactPoint;
        public final Vec3d contactNormal;
        public final double impactForce;
        public final UUID entityUuid; // Null if block contact
        public final Entity contactedEntity; // Null if block contact

        public ContactData(ContactType type, Vec3d contactPoint, Vec3d contactNormal,
                double impactForce, UUID entityUuid, Entity contactedEntity) {
            this.type = type;
            this.contactPoint = contactPoint;
            this.contactNormal = contactNormal;
            this.impactForce = impactForce;
            this.entityUuid = entityUuid;
            this.contactedEntity = contactedEntity;
        }

        public boolean isEntityContact() {
            return type == ContactType.ENTITY;
        }

        public boolean isBlockContact() {
            return type != ContactType.ENTITY;
        }
    }
}
