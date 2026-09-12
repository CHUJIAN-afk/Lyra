package first.lyra.api;

import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class LyraHelper {

    private final Level level;
    private final @Nullable Player player;
    private final AttachmentEntityData attachmentEntityData;

    private LyraHelper(Level level, @Nullable Player player) {
        this.level = level;
        this.player = player;
        this.attachmentEntityData = level.getData(LyraAttachmentRegister.EntityData);
    }

    public static LyraHelper get(Level level) {
        return new LyraHelper(level, null);
    }

    public static LyraHelper get(Player player) {
        return new LyraHelper(player.level(), player);
    }

    public TargetCache getTargetCache(){
        return level.getData(LyraAttachmentRegister.TargetCache);
    }

    public AttachmentEntityData getEntityData() {
        return attachmentEntityData;
    }

    public void add(AttachmentEntity entity) {
        entity.setLevel(level);
        if (player != null) {
            entity.setOwner(player);
        }
        attachmentEntityData.add(entity);
    }

    public void remove(AttachmentEntityType<?> entityType) {
        attachmentEntityData.getGroups()
                .getOrDefault(entityType, List.of())
                .stream()
                .filter(this::isOwnedBy)
                .forEach(AttachmentEntity::setRemove);
    }

    public boolean canSummon(MinionSlotType type, int slotCost) {
        return getMaxCount(type) - getUsedSlots(type) >= slotCost;
    }

    public int getMaxCount(MinionSlotType type) {
        int maxCount = 0;
        if (player != null) {
            maxCount = switch (type) {
                case Minion -> {
                    AttributeInstance attributeInstance = player.getAttribute(LyraAttributeRegister.MinionMaxCount);
                    yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
                }
                case Sentry -> {
                    AttributeInstance attributeInstance = player.getAttribute(LyraAttributeRegister.SentryMaxCount);
                    yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
                }
                case None -> 0;
            };
        }
        return maxCount;
    }

    public int getUsedSlots(MinionSlotType type) {
        return attachmentEntityData.getGroups()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(this::isOwnedBy)
                .filter(entity -> entity instanceof Minion)
                .map(entity -> (Minion) entity)
                .filter(minion -> minion.getSlotType() == type)
                .filter(minion -> !minion.isRemove())
                .mapToInt(Minion::getSlotCost)
                .sum();
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public Level getLevel() {
        return level;
    }

    private boolean isOwnedBy(AttachmentEntity entity) {
        boolean owned = player == null;
        if (player != null) {
            Player entityOwner = entity.getOwner();
            if (entityOwner != null) {
                owned = entityOwner.getUUID().equals(player.getUUID());
            }
        }
        return owned;
    }
}
