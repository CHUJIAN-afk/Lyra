package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.mesdag.portlib.registries.PortCustomRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;

public class LyraRegistries {

    private static final ResourceKey<Registry<AttachmentEntityType<? extends AttachmentEntity>>> ATTACHMENT_ENTITY_TYPE_KEY = ResourceKey.createRegistryKey(Lyra.rl("attachment_entity_types"));

    public static final PortCustomRegistration<AttachmentEntityType<? extends AttachmentEntity>> ATTACHMENT_ENTITY_TYPES = PortRegisterHandler.custom(Lyra.MODID, ATTACHMENT_ENTITY_TYPE_KEY, maker -> maker.sync(true));

    public static void init() {
    }
}
