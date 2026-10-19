package openraven.ravenxd.jade.compat;

public abstract class Event {
    private boolean canceled;

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public void setCancelled(boolean canceled) {
        this.canceled = canceled;
    }
}
