package first.lyra.api;

import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

public final class LyraHelper {

    private final Player player;
    private final AttachmentEntityData attachmentEntityData;

    private LyraHelper(Player player) {
        this.player = player;
        this.attachmentEntityData = player.getData(LyraAttachmentRegister.EntityData);
    }

    public static LyraHelper get(Player player) {
        return new LyraHelper(player);
    }

    public AttachmentEntityData getEntityData() {
        return attachmentEntityData;
    }

    public void add(AttachmentEntity entity) {
        attachmentEntityData.add(entity);
    }

    public void remove(AttachmentEntityType<?> entityType) {
        attachmentEntityData.getGroups()
                .getOrDefault(entityType, List.of())
                .stream()
                .filter(entity -> entity instanceof Minion minion)
                .map(entity -> (Minion) entity)
                .forEach(AttachmentEntity::setRemove);
    }

    public boolean canSummon(MinionSlotType type, int slotCost) {
        return getMaxCount(type) - getUsedSlots(type) >= slotCost;
    }

    public int getMaxCount(MinionSlotType type) {
        int maxCount;
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
        return maxCount;
    }

    public int getUsedSlots(MinionSlotType type) {
        return attachmentEntityData.getGroups()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(entity -> entity instanceof Minion minion)
                .map(entity -> (Minion) entity)
                .filter(minion -> minion.getSlotType() == type && !minion.isRemove())
                .mapToInt(Minion::getSlotCost)
                .sum();
    }
}
