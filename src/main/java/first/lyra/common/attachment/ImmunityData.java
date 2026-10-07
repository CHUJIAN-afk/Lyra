package first.lyra.common.attachment;

import first.lyra.register.LyraAttachmentRegister;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.mesdag.portlib.attachment.IPortAttachmentHolder;

import java.util.UUID;

public class ImmunityData {

    private final LivingEntity owner;

    public ImmunityData(IPortAttachmentHolder holder) {
        if (holder instanceof LivingEntity living) {
            this.owner = living;
        } else {
            throw new IllegalArgumentException("ImmunityData only supports LivingEntity");
        }
    }

    public static ImmunityData get(LivingEntity target) {
        return target.getData(LyraAttachmentRegister.ImmunityData);
    }

    private final Object2IntOpenHashMap<UUID> data = new Object2IntOpenHashMap<>();

    public void tick() {
        if (!data.isEmpty()) {
            data.replaceAll((uuid, integer) -> integer - 1);
            data.values().removeIf(v -> v <= 0);
        }
    }

    public void record(UUID uuid, int time) {
        data.put(uuid, time);
    }

    public boolean attack(DamageSource source, float amount) {
        int invulnerableTime = owner.invulnerableTime;
        owner.invulnerableTime = 0;
        boolean hurt = owner.hurt(source, amount);
        owner.invulnerableTime = invulnerableTime;
        return hurt;
    }

    public boolean isActive(UUID uuid) {
        return data.containsKey(uuid);
    }
}
