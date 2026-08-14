package first.lyra.common;

import first.lyra.Lyra;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.builder.MinionWeaponItemBuilder;
import first.lyra.common.attachment.ParticlesData;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.attachment.InvincibleData;
import first.lyra.register.LyraAttributeRegister;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Lyra.MODID)
public class Event {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(LevelTickEvent.Post event) {
        ParticlesData.tick(event);
        DamageInfoData.tick(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LivingEquipmentChangeEvent event) {
        ArmorSet.handler(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handler(PlayerInteractEvent.RightClickItem event) {
        MinionWeaponItemBuilder.handler(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(EntityAttributeModificationEvent event) {
        LyraAttributeRegister.handler(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LivingDamageEvent.Post event) {
        InvincibleData.handler(event);
        // 伤害信息数据构造已迁移到 DamageInfo 子模组(DamageInfoData.handler 由子模组调用)
    }
}
