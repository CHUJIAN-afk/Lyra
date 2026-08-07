package first.lyra.common.armorSet;

import com.google.common.collect.Multimap;
import first.lyra.common.builder.ArmorSetBuilder;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraRegistries;
import first.lyra.utils.AttributeUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.*;
import java.util.function.Consumer;

public record ArmorSet(ResourceLocation id, List<ItemLike> items, Multimap<Holder<Attribute>, AttributeModifier> modifiers, Consumer<Player> onStart, Consumer<Player> onRemove) {

    public static void handler(LivingEquipmentChangeEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Player player) {
            Map<ArmorSet, Boolean> cache = player.getData(LyraAttachmentRegister.ArmorSetData).activeSets;
            Map<ArmorSet, Boolean> lookup = new HashMap<>();
            if (!cache.isEmpty()) {
                lookup.putAll(cache);
                cache.clear();
            }
            List<ArmorSet> list = LyraRegistries.ARMOR_SETS.stream().toList();
            for (ArmorSet armorSet : list) {
                boolean full = armorSet.full(player);
                if (full) {
                    if ((!lookup.containsKey(armorSet) || !lookup.get(armorSet))) {
                        //首次生效时调用
                        armorSet.onStart().accept(player);
                    }
                } else {
                    if (lookup.containsKey(armorSet) && lookup.get(armorSet)) {
                        //失效时调用
                        armorSet.onRemove().accept(player);
                    }
                }
                if (!player.level().isClientSide()) {
                    Collection<Map.Entry<Holder<Attribute>, AttributeModifier>> entries = armorSet.modifiers().entries();
                    for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : entries) {
                        Holder<Attribute> key = entry.getKey();
                        AttributeModifier value = entry.getValue();
                        AttributeUtils.condition(player, key, value.id(), value.amount(), value.operation(), full);
                    }
                }
            }
        }
    }

    public static ArmorSetBuilder builder(ResourceLocation id) {
        return new ArmorSetBuilder(id);
    }

    public boolean full(Player player) {
        return player.getData(LyraAttachmentRegister.ArmorSetData).activeSets
                .computeIfAbsent(this, set -> {
                    Iterable<ItemStack> armorSlots = player.getArmorSlots();
                    List<Item> target = new ArrayList<>();
                    for (ItemLike item : items) {
                        target.add(item.asItem());
                    }
                    for (ItemStack armor : armorSlots) {
                        target.remove(armor.getItem());
                        if (target.isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ArmorSet armorSet) {
            return armorSet.id.equals(id);
        }
        return false;
    }
}
