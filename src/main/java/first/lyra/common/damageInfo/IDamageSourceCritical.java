package first.lyra.common.damageInfo;

/**
 * 可暴击伤害源标记接口。
 * <p>
 * 由 mixin 注入实现（前缀 {@code lyra$}，见主 mod 的 DamageSourceMixin），
 * 用于标记/查询伤害源是否为暴击。
 * </p>
 */
public interface IDamageSourceCritical {

    boolean lyra$isCritical();

    void lyra$setCritical(boolean isCritical);
}
