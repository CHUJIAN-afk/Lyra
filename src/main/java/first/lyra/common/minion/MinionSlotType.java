package first.lyra.common.minion;

import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum MinionSlotType {
    Minion(player -> {
        AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.MinionMaxCount);
        if (attribute != null) {
            return (int) attribute.getValue();
        }
        return 0;
    }),
    Sentry(player -> {
        AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SentryMaxCount);
        if (attribute != null) {
            return (int) attribute.getValue();
        }
        return 0;
    }),
    None(player -> 1000);

    private final Function<Player, Integer> function;

    MinionSlotType(Function<Player, Integer> function) {
        this.function = function;
    }

    public int getLimit(Player player) {
        return function.apply(player);
    }

    public static Map<MinionSlotType, Integer> getLimits(Player player) {
        Map<MinionSlotType, Integer> limits = new HashMap<>();
        for (MinionSlotType minionSlotType : MinionSlotType.values()) {
            limits.put(minionSlotType, minionSlotType.getLimit(player));
        }
        return limits;
    }
}
