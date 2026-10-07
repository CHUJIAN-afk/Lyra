package first.lyra.common.summonMark;

import com.mojang.datafixers.util.Function5;
import first.lyra.common.attachment.SummonMarkTracker;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.consumer.Consumer4;
import first.lyra.common.consumer.Consumer5;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record SummonMarkType(
        ResourceLocation location,
        float additionalDamage,
        float additionalArmorPierce,
        float criticalHitRate,
        @Nullable Consumer4<SummonMarkTracker, SummonMarkInstance, LivingEntity, Player> tickConsumer,
        @Nullable Function5<SummonMarkTracker, SummonMarkInstance, LivingEntity, AttachmentEntityDamageSource, Float, Float> damagePre,
        @Nullable Consumer5<SummonMarkTracker, SummonMarkInstance, LivingEntity, AttachmentEntityDamageSource, Float> damagePost,
        @Nullable Consumer4<SummonMarkTracker, SummonMarkInstance, LivingEntity, AttachmentEntityDamageSource> killConsumer
) {

    public SummonMarkInstance createInstance(Player player, LivingEntity target) {
        return new SummonMarkInstance(this, 200);
    }

    public List<Component> getTooltips() {
        List<Component> tooltips = new ArrayList<>();
        if (additionalDamage > 0) {
            tooltips.add(Component.literal(String.format("%.1f ", additionalDamage))
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.translatable("mark.additional_damage").withStyle(ChatFormatting.GRAY)));
        }
        if (additionalArmorPierce > 0) {
            tooltips.add(Component.literal(String.format("%.1f ", additionalArmorPierce))
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.translatable("mark.additional_armor_pierce").withStyle(ChatFormatting.GRAY)));
        }
        if (criticalHitRate > 0) {
            tooltips.add(Component.literal(String.format("%.0f%% ", criticalHitRate * 100))
                    .withStyle(ChatFormatting.BLUE)
                    .append(Component.translatable("mark.critical_hit_rate").withStyle(ChatFormatting.GRAY)));
        }
        return tooltips;
    }

    public void tick(SummonMarkTracker tracker, SummonMarkInstance instance, LivingEntity target, Player owner) {
        if (tickConsumer != null) {
            tickConsumer.accept(tracker, instance, target, owner);
        }
    }

    public float damagePre(SummonMarkTracker tracker, SummonMarkInstance instance, LivingEntity target, AttachmentEntityDamageSource source, float damage) {
        if (damagePre != null) {
            return damagePre.apply(tracker, instance, target, source, damage);
        }
        return damage;
    }

    public void damagePost(SummonMarkTracker tracker, SummonMarkInstance instance, LivingEntity target, AttachmentEntityDamageSource source, float damage) {
        if (damagePost != null) {
            damagePost.accept(tracker, instance, target, source, damage);
        }
    }

    public void kill(SummonMarkTracker tracker, SummonMarkInstance instance, LivingEntity target, AttachmentEntityDamageSource source) {
        if (killConsumer != null) {
            killConsumer.accept(tracker, instance, target, source);
        }
    }
}
