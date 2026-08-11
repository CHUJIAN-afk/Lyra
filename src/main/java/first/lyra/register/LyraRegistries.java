package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraRegistries {

    private static final ResourceKey<Registry<AttachmentEntityType<? extends AttachmentEntity>>> ATTACHMENT_ENTITY_TYPE_KEY = ResourceKey.createRegistryKey(Lyra.id("attachment_entity_types"));

    public static final Registry<AttachmentEntityType<? extends AttachmentEntity>> ATTACHMENT_ENTITY_TYPES = new RegistryBuilder<>(ATTACHMENT_ENTITY_TYPE_KEY).sync(true).create();

    private static final ResourceKey<Registry<ArmorSet>> ARMOR_SET_KEY = ResourceKey.createRegistryKey(Lyra.id("armor_set"));

    public static final Registry<ArmorSet> ARMOR_SETS = new RegistryBuilder<>(ARMOR_SET_KEY).sync(true).create();

    @SubscribeEvent
    public static void createRegistry(NewRegistryEvent event) {
        event.register(ATTACHMENT_ENTITY_TYPES);
        event.register(ARMOR_SETS);
    }
}
