package first.lyra.common.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DamageSourceProvider {
    DamageSource getDamageSource(@NotNull Level level, @Nullable Player player);
}
