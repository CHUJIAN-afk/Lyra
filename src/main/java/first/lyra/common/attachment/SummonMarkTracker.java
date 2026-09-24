package first.lyra.common.attachment;

import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.summonMark.SummonMarkInstance;
import first.lyra.common.summonMark.SummonMarkType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SummonMarkTracker {

    private final Player owner;
    private final Map<SummonMarkType, SummonMarkInstance> marks = new HashMap<>();
    private LivingEntity target = null;

    public SummonMarkTracker(IAttachmentHolder owner) {
        if (owner instanceof Player player) {
            this.owner = player;
        } else {
            throw new IllegalArgumentException(owner + " is not a valid SummonMarkTracker");
        }
    }

    public void tick() {
        if (target != null && target.isAlive()) {
            marks.values().removeIf(instance -> {
                instance.getType().tick(this, instance, target, owner);
                instance.setDuration(instance.getDuration() - 1);
                return instance.getDuration() <= 0;
            });
        } else {
            marks.clear();
        }
        if (marks.isEmpty()) {
            target = null;
        }
        owner.syncData(LyraAttachmentRegister.SummonMarkData.get());
    }

    public void tracker(LivingEntity target, SummonMarkInstance instance) {
        if (owner != target) {
            if (!isTarget(target)) {
                marks.clear();
            }
            this.target = target;
            marks.put(instance.getType(), instance);
        }
    }

    public @Nullable SummonMarkInstance getInstance(SummonMarkType type) {
        return marks.get(type);
    }

    public float getDamageModifier(AttachmentEntityDamageSource source, float damage) {
        if (!marks.isEmpty()) {
            float additionalDamage = 0;
            float additionalArmorPierce = 0;
            float criticalHitRate = 0;
            for (SummonMarkType summonMarkType : marks.keySet()) {
                if (summonMarkType.additionalDamage() > additionalDamage) {
                    additionalDamage = summonMarkType.additionalDamage();
                }
                if (summonMarkType.additionalArmorPierce() > additionalArmorPierce) {
                    additionalArmorPierce = summonMarkType.additionalArmorPierce();
                }
                if (summonMarkType.criticalHitRate() > criticalHitRate) {
                    criticalHitRate = summonMarkType.criticalHitRate();
                }
            }
            damage += additionalDamage;
            source.setArmorPierce(source.getArmorPierce() + additionalArmorPierce);
            if (criticalHitRate > owner.getRandom().nextFloat()) {
                damage *= 2;
            }
        }
        return damage;
    }

    public Player getOwner() {
        return owner;
    }

    public List<SummonMarkInstance> getSummonMarkInstances() {
        return marks.values().stream().toList();
    }

    public LivingEntity getTarget() {
        return target;
    }

    public boolean isTarget(LivingEntity living) {
        return target != null && target == living;
    }

    public static final class SyncHandler implements AttachmentSyncHandler<SummonMarkTracker> {

        @Override
        public void write(@NotNull RegistryFriendlyByteBuf buf, SummonMarkTracker data, boolean initialSync) {
            LyraStreamCodecs.optional(LyraStreamCodecs.INT).encode(buf, data.target != null ? Optional.of(data.target.getId()) : Optional.empty());
        }

        @Override
        public SummonMarkTracker read(@NotNull IAttachmentHolder holder, @NotNull RegistryFriendlyByteBuf buf, @Nullable SummonMarkTracker oldData) {
            SummonMarkTracker data = oldData == null ? new SummonMarkTracker(holder) : oldData;
            Optional<Integer> optional = LyraStreamCodecs.optional(LyraStreamCodecs.INT).decode(buf);
            if (optional.isPresent()) {
                int id = optional.get();
                if (holder instanceof Entity entity && entity.level().getEntity(id) instanceof LivingEntity living) {
                    data.target = living;
                }
            } else {
                data.target = null;
            }
            return data;
        }
    }
}
