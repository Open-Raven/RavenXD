package xyz.daikirai.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerSP.class)
public interface IEntityPlayerSP {
    @Accessor("serverSprintState")
    boolean getServerSprintState();

    @Accessor("serverSprintState")
    void setServerSprintState(boolean b);

    @Accessor("serverSneakState")
    boolean getServerSneakState();

    @Accessor("serverSneakState")
    void setServerSneakState(boolean b);

    @Accessor("lastReportedPosX")
    double getLastReportedPosX();

    @Accessor("lastReportedPosX")
    void setLastReportedPosX(double d);

    @Accessor("lastReportedPosY")
    double getLastReportedPosY();

    @Accessor("lastReportedPosY")
    void setLastReportedPosY(double d);

    @Accessor("lastReportedPosZ")
    double getLastReportedPosZ();

    @Accessor("lastReportedPosZ")
    void setLastReportedPosZ(double d);

    @Accessor("lastReportedYaw")
    float getLastReportedYaw();

    @Accessor("lastReportedYaw")
    void setLastReportedYaw(float f);

    @Accessor("lastReportedPitch")
    float getLastReportedPitch();

    @Accessor("lastReportedPitch")
    void setLastReportedPitch(float f);

    @Accessor("positionUpdateTicks")
    int getPositionUpdateTicks();

    @Accessor("positionUpdateTicks")
    void setPositionUpdateTicks(int i);

    @Accessor("sprintToggleTimer")
    void setSprintToggleTimer(int i);
}
