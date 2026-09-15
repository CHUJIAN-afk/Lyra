package first.lyra.common.attachmentEntity;

import net.minecraft.world.entity.LivingEntity;

public interface IOwner {

    LivingEntity getOwner();

    void setOwner(LivingEntity owner);
}
