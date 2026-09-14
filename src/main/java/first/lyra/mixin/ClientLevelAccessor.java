package first.lyra.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.multiplayer.ClientLevel.class)
public interface ClientLevelAccessor {
    @Invoker
    LevelEntityGetter<Entity> callGetEntities();
}
