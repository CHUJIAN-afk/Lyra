package first.lyra.common.attachmentEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record AttachmentEntityType<T extends AttachmentEntity>(@NotNull ResourceLocation location, @NotNull Supplier<T> factory) {

    public Component getDisplayName() {
        String key = "summon." + location.getNamespace() + "." + location.getPath();
        return Component.translatable(key).withStyle(ChatFormatting.BLUE);
    }
}
