package first.lyra.common.minion;

import first.lyra.common.entity.AttachmentEntity;

public abstract class MinionGoal<T extends AttachmentEntity> {

    protected final T minion;

    public MinionGoal(T minion) {
        this.minion = minion;
    }

    public abstract boolean canUse();

    public boolean canContinueToUse() {
        return canUse();
    }

    public boolean isInterruptable() {
        return true;
    }

    public void start() {}

    public void tick() {}

    public void stop() {}

}
