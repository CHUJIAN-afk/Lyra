package first.lyra.api;

import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class LyraHelper {

    private final Level level;
    private final AttachmentEntityData attachmentEntityData;

    private LyraHelper(Level level) {
        this.level = level;
        this.attachmentEntityData = level.getData(LyraAttachmentRegister.EntityData);
    }

    public static LyraHelper get(Level level) {
        return new LyraHelper(level);
    }

    public TargetCache getTargetCache(){
        return level.getData(LyraAttachmentRegister.TargetCache);
    }

    public AttachmentEntityData getEntityData() {
        return attachmentEntityData;
    }

    public void add(AttachmentEntity entity) {
        attachmentEntityData.add(entity);
    }

    public void remove(LivingEntity living, AttachmentEntityType<?> entityType) {
        attachmentEntityData.getGroups()
                .getOrDefault(entityType, List.of())
                .stream()
                .filter(entity -> entity instanceof Minion minion)
                .map(entity -> (Minion) entity)
                .filter(minion -> minion.getOwner() == living && !minion.isRemove())
                .forEach(AttachmentEntity::setRemove);
    }

    public boolean canSummon(LivingEntity living, MinionSlotType type, int slotCost) {
        return getMaxCount(living, type) - getUsedSlots(living, type) >= slotCost;
    }

    public int getMaxCount(LivingEntity living, MinionSlotType type) {
        int maxCount = 0;
        if (living != null) {
            maxCount = switch (type) {
                case Minion -> {
                    AttributeInstance attributeInstance = living.getAttribute(LyraAttributeRegister.MinionMaxCount);
                    yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
                }
                case Sentry -> {
                    AttributeInstance attributeInstance = living.getAttribute(LyraAttributeRegister.SentryMaxCount);
                    yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
                }
                case None -> 0;
            };
        }
        return maxCount;
    }

    public int getUsedSlots(LivingEntity living, MinionSlotType type) {
        return attachmentEntityData.getGroups()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(entity -> entity instanceof Minion minion)
                .map(entity -> (Minion) entity)
                .filter(minion -> minion.getSlotType() == type && minion.getOwner() == living && !minion.isRemove())
                .mapToInt(Minion::getSlotCost)
                .sum();
    }
}
