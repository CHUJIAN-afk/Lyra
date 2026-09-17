package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.ForgeRegistry;
import org.mesdag.portlib.registries.PortCustomRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;

import java.util.Collection;

public class LyraRegistries {

    private static final ResourceKey<Registry<AttachmentEntityType<? extends AttachmentEntity>>> ATTACHMENT_ENTITY_TYPE_KEY = ResourceKey.createRegistryKey(Lyra.rl("attachment_entity_types"));

    public static final PortCustomRegistration<AttachmentEntityType<? extends AttachmentEntity>> ATTACHMENT_ENTITY_TYPES =
            PortRegisterHandler.custom(Lyra.MODID, ATTACHMENT_ENTITY_TYPE_KEY, maker -> maker.sync(true));

    private static final ResourceKey<Registry<ArmorSet>> ARMOR_SET_KEY = ResourceKey.createRegistryKey(Lyra.rl("armor_set"));

    public static final PortCustomRegistration<ArmorSet> ARMOR_SETS =
            PortRegisterHandler.custom(Lyra.MODID, ARMOR_SET_KEY, maker -> maker.sync(true));

    public static Collection<ArmorSet> armorSets() {
        ForgeRegistry<ArmorSet> registry = RegistryManager.ACTIVE.getRegistry(ARMOR_SET_KEY);
        return registry == null ? java.util.List.of() : registry.getValues();
    }

    public static Collection<AttachmentEntityType<? extends AttachmentEntity>> attachmentEntityTypes() {
        ForgeRegistry<AttachmentEntityType<? extends AttachmentEntity>> registry = RegistryManager.ACTIVE.getRegistry(ATTACHMENT_ENTITY_TYPE_KEY);
        return registry == null ? java.util.List.of() : registry.getValues();
    }
}
