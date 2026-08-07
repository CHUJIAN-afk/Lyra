package first.lyra.api;

import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.servant.Servant;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashMap;

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

    public TargetCache getTargetCache(){
        return player.getData(LyraAttachmentRegister.TargetCache);
    }

    public AttachmentEntityData getEntityData() {
        return attachmentEntityData;
    }

    public void add(AttachmentEntityData.Type type, AttachmentEntity entity) {
        entity.setOwner(player);
        attachmentEntityData.add(type, entity);
    }

    public boolean canSummon(AttachmentEntityData.Type type, int slotCost) {
        return getMaxCount(type) - getUsedSlots(type) >= slotCost;
    }

    public int getMaxCount(AttachmentEntityData.Type type) {
        return switch (type) {
            case Servant -> {
                AttributeInstance attributeInstance = player.getAttribute(LyraAttributeRegister.ServantMaxCount);
                yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
            }
            case SentryServant -> {
                AttributeInstance attributeInstance = player.getAttribute(LyraAttributeRegister.SentryServantMaxCount);
                yield attributeInstance != null ? (int) attributeInstance.getValue() : 0;
            }
            default -> 0;
        };

    }

    public int getUsedSlots(AttachmentEntityData.Type type) {
        return attachmentEntityData.getGroups()
                .getOrDefault(type, new HashMap<>())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(entity -> entity instanceof Servant)
                .map(entity -> (Servant) entity)
                .filter(servant -> !servant.isRemove())
                .mapToInt(Servant::getSlotCost)
                .sum();
    }

    public Player getPlayer() {
        return player;
    }
}
