package openraven.ravenxd.util;

// Client thread only.
public final class ClientActionTimer {
    private Runnable pending;
    private long deadline;

    public void schedule(long now, long delayMillis, Runnable action) {
        this.deadline = now + Math.max(0L, delayMillis);
        this.pending = action;
    }

    public void cancel() {
        this.pending = null;
    }

    public void tick(long now) {
        if (this.pending == null || now < this.deadline) return;
        Runnable action = this.pending;
        this.pending = null;
        action.run();
    }
}
