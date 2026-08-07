package first.lyra.common.servant;

public abstract class ServantGoal<T extends Servant> {

    protected final T servant;

    public ServantGoal(T servant) {
        this.servant = servant;
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
