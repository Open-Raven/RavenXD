package openraven.ravenxd.jade.compat;

public interface ProgressBarSource {
    boolean isProgressActive();

    float getProgressFraction();

    String getProgressLabel();
}
