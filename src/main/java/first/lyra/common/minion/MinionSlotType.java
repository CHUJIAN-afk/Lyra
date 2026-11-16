package first.lyra.common.minion;

import first.lyra.register.LyraAttributeRegister;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum MinionSlotType {
    Minion(LyraAttributeRegister.MinionMaxCount, attribute -> {
        if (attribute != null) {
            return (int) attribute.getValue();
        }
        return 0;
    }),
    Sentry(LyraAttributeRegister.SentryMaxCount,attribute -> {
        if (attribute != null) {
            return (int) attribute.getValue();
        }
        return 0;
    }),
    None(null, attribute -> 1000);

    private final Holder<Attribute> holder;
    private final Function<@Nullable AttributeInstance, Integer> function;

    MinionSlotType(@Nullable Holder<Attribute> supplier, Function<@Nullable AttributeInstance, Integer> function) {
        this.holder = supplier;
        this.function = function;
    }

    public int getLimit(Player player) {
        return function.apply(holder != null ? player.getAttribute(holder) : null);
    }

    public static Map<MinionSlotType, Integer> getLimits(Player player) {
        Map<MinionSlotType, Integer> limits = new HashMap<>();
        for (MinionSlotType minionSlotType : MinionSlotType.values()) {
            limits.put(minionSlotType, minionSlotType.getLimit(player));
        }
        return limits;
    }
}
