package first.lyra.common.network;

import first.lyra.Lyra;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.damageInfo.DamageInfo;
import first.lyra.common.damageInfo.DamageInfoStyle;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.ArrayList;
import java.util.List;

public record BatchedDamageInfoPayload(List<Entry> entries) implements IPortPacket.S2C {

    public static final ResourceLocation ID = Lyra.rl("damage_info");

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, BatchedDamageInfoPayload> STREAM_CODEC = PortStreamCodec.composite(
            Entry.STREAM_CODEC.apply(PortByteBufCodecs.list()),
            BatchedDamageInfoPayload::entries,
            BatchedDamageInfoPayload::new
    );

    @Override
    public void work(Player player) {
        Level level = player.level();
        DamageInfoData damageInfoData = level.getData(LyraAttachmentRegister.DamageInfoData);
        for (Entry entry : entries) {
            DamageInfo info = null;
            DamageInfoStyle style = DamageInfoStyleManager.INSTANCE.getStyle(ResourceLocation.parse(entry.damageType));
            if (style != null) {
                info = new DamageInfo(style, entry.damageAmount, new Vec3(entry.x, entry.y, entry.z), new Vec3(entry.vx, entry.vy, entry.vz), entry.critical);
            }
            if (info != null) {
                Vec3 pos = info.getRenderPos(0);
                Vec3 eyePosition = player.getEyePosition(0);
                if (pos.distanceToSqr(eyePosition) < 64 * 64) {
                    damageInfoData.getActiveInfos()
                            .computeIfAbsent(info.getTexture(), key -> new ArrayList<>())
                            .add(info);
                }
            }
        }
    }

    @Override
    public ResourceLocation identifier() {
        return ID;
    }

    public record Entry(String damageType, float damageAmount, double x, double y, double z, double vx, double vy, double vz, boolean critical) {

        public static final PortStreamCodec<PortRegistryFriendlyByteBuf, Entry> STREAM_CODEC = PortStreamCodec.ofMember(
                (entry, buf) -> {
                    buf.writeUtf(entry.damageType);
                    buf.writeFloat(entry.damageAmount);
                    buf.writeDouble(entry.x);
                    buf.writeDouble(entry.y);
                    buf.writeDouble(entry.z);
                    buf.writeDouble(entry.vx);
                    buf.writeDouble(entry.vy);
                    buf.writeDouble(entry.vz);
                    buf.writeBoolean(entry.critical);
                },
                buf -> new Entry(
                        buf.readUtf(),
                        buf.readFloat(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readBoolean()
                )
        );
    }
}
