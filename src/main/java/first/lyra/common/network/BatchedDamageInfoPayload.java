package first.lyra.common.network;

import first.lyra.Lyra;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.damageInfo.DamageInfo;
import first.lyra.common.damageInfo.DamageInfoStyle;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量伤害数字网络包（服务端 → 客户端）。
 * <p>
 * 服务端只发送最小数据（伤害类型、伤害值、位置、速度、暴击标记），
 * 客户端收到后根据伤害类型从 {@link DamageInfoStyleManager} 查询样式重建完整渲染参数。
 * </p>
 *
 * @param entries 伤害数字记录列表
 */
public record BatchedDamageInfoPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<BatchedDamageInfoPayload> TYPE = new Type<>(Lyra.rl("damage_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BatchedDamageInfoPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BatchedDamageInfoPayload::entries,
            BatchedDamageInfoPayload::new
    );

    /**
     * 客户端处理：逐条转为 {@link DamageInfo} 写入客户端 Level 附件。
     */
    public static void handleClient(BatchedDamageInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            DamageInfoData damageInfoData = level.getData(LyraAttachmentRegister.DamageInfoData);
            for (Entry entry : payload.entries()) {
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
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 单条伤害数字记录：伤害类型 + 伤害值 + 位置 + 速度 + 暴击标记。
     * <p>
     * 渲染参数（贴图、颜色、尺寸等）由客户端根据 damageType 从 JSON 样式表查询，
     * 不通过网络同步，大幅减少流量。
     * </p>
     */
    public record Entry(String damageType, float damageAmount, double x, double y, double z, double vx, double vy, double vz, boolean critical) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.ofMember(
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
