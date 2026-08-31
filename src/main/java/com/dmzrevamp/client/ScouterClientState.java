package com.dmzrevamp.client;

import com.dmzrevamp.mixin.client.LockOnEventAccessor;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.LocateMasterStructureC2SPacket;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.CuriosUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class ScouterClientState {
    private static final int SCOUTER_YELLOW = 0xFFFF55;
    private static final double SCOUTER_LOCK_RANGE = 50.0D;
    private static final int MASTER_SEARCH_RESYNC_TICKS = 100;

    private static Mode mode = Mode.OFF;
    private static BlockPos masterTarget;
    private static int masterSyncTicks;
    private static boolean scouterBackedLock;
    private static boolean wasLockKeyDown;
    private static Player cachedScouterPlayer;
    private static int cachedScouterTick = Integer.MIN_VALUE;
    private static boolean cachedHasScouter;

    private ScouterClientState() {
    }

    public static void cycleMode() {
        if (mode == Mode.OFF) {
            setMode(Mode.NORMAL);
        } else if (mode == Mode.NORMAL) {
            setMode(Mode.MASTER_SEARCH);
        } else {
            setMode(Mode.OFF);
        }
    }

    public static void forceOff() {
        if (mode == Mode.OFF) {
            masterTarget = null;
            return;
        }
        setMode(Mode.OFF);
    }

    public static void forceUnlockScouterBackedLock() {
        unlockIfScouterBacked();
    }

    public static boolean isMasterSearch() {
        return mode == Mode.MASTER_SEARCH;
    }

    public static boolean isScouterActive() {
        return mode != Mode.OFF;
    }

    public static void tick(LocalPlayer player) {
        if (player == null) {
            wasLockKeyDown = false;
            return;
        }
        if (!hasScouter(player)) {
            forceOff();
            unlockIfScouterBacked();
            wasLockKeyDown = false;
            return;
        }
        if (mode == Mode.MASTER_SEARCH && masterSyncTicks++ >= MASTER_SEARCH_RESYNC_TICKS) {
            masterSyncTicks = 0;
            DmzRevampNetwork.CHANNEL.sendToServer(new LocateMasterStructureC2SPacket());
        }
    }

    public static void handleLockInput(LocalPlayer player, StatsData data) {
        if (player == null || data == null || !hasScouter(player) || !isScouterActive() || data.getSkills().hasSkill("kisense")) {
            wasLockKeyDown = false;
            return;
        }

        boolean lockKeyDown = com.dragonminez.client.util.KeyBinds.LOCK_ON.isDown();
        if (lockKeyDown && !wasLockKeyDown) {
            toggleScouterLock(player, data);
        }
        wasLockKeyDown = lockKeyDown;
    }

    public static void setMasterTarget(BlockPos target) {
        masterTarget = target;
    }

    public static void drawBattlePowerText(GuiGraphics graphics, String value) {
        Font font = Minecraft.getInstance().font;
        String text = value;
        float scale = 0.45F;
        float centerX = 18.0F;
        float y = -5.0F;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int x = Math.round((centerX - font.width(text) * scale / 2.0F) / scale);
        graphics.drawString(font, text, x, Math.round(y / scale), SCOUTER_YELLOW, false);
        graphics.pose().popPose();
    }

    public static void renderMasterSearch(GuiGraphics graphics, ResourceLocation scouterTexture) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || masterTarget == null) {
            return;
        }

        int direction = directionIndex(player, Vec3.atCenterOf(masterTarget));
        drawDirection(graphics, scouterTexture, direction);
    }

    public static void toggleScouterLock(Player player, StatsData data) {
        LivingEntity currentTarget = LockOnEventAccessor.dmzrevamp$getLockedTarget();
        if (currentTarget != null) {
            LockOnEvent.unlock();
            scouterBackedLock = false;
            return;
        }

        findTarget(player, data).ifPresent(target -> {
            LockOnEventAccessor.dmzrevamp$setLockedTarget(target);
            scouterBackedLock = true;
            player.playSound(MainSounds.LOCKON.get(), 1.0F, 1.0F);
        });
    }

    public static boolean validateScouterLock(Player player, StatsData data) {
        if (!scouterBackedLock) {
            return false;
        }

        LivingEntity target = LockOnEventAccessor.dmzrevamp$getLockedTarget();
        if (target == null) {
            scouterBackedLock = false;
            return true;
        }
        if (!hasScouter(player) || !isScouterActive() || !target.isAlive() || player.distanceTo(target) > SCOUTER_LOCK_RANGE || !LockOnCycleClientEvents.canTarget(target, data)) {
            LockOnEvent.unlock();
            scouterBackedLock = false;
            return true;
        }
        if (!player.hasLineOfSight(target) && !data.getStatus().isAndroidUpgraded()) {
            LockOnEvent.unlock();
            scouterBackedLock = false;
        }
        return true;
    }

    public static boolean hasScouter(Player player) {
        if (player == null) {
            cachedScouterPlayer = null;
            cachedScouterTick = Integer.MIN_VALUE;
            cachedHasScouter = false;
            return false;
        }
        if (cachedScouterPlayer == player && cachedScouterTick == player.tickCount) {
            return cachedHasScouter;
        }
        var stack = CuriosUtil.getFirstStack(player, "head_tech");
        cachedScouterPlayer = player;
        cachedScouterTick = player.tickCount;
        cachedHasScouter = !stack.isEmpty() && stack.getItem().getDescriptionId().contains("scouter");
        return cachedHasScouter;
    }

    private static void setMode(Mode nextMode) {
        mode = nextMode;
        if (nextMode == Mode.OFF) {
            masterTarget = null;
            com.dragonminez.client.gui.hud.ScouterHUD.setRenderingInfo(false);
            return;
        }

        com.dragonminez.client.gui.hud.ScouterHUD.setRenderingInfo(true);
        if (nextMode == Mode.MASTER_SEARCH) {
            masterSyncTicks = MASTER_SEARCH_RESYNC_TICKS;
        }
    }

    private static void unlockIfScouterBacked() {
        if (scouterBackedLock) {
            LockOnEvent.unlock();
            scouterBackedLock = false;
        }
    }

    private static java.util.Optional<LivingEntity> findTarget(Player player, StatsData data) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(view.scale(SCOUTER_LOCK_RANGE));
        AABB searchBox = player.getBoundingBox().expandTowards(view.scale(SCOUTER_LOCK_RANGE)).inflate(4.0D);

        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox, target ->
                        target != player && target.isAlive() && target.isPickable() && LockOnCycleClientEvents.canTarget(target, data))
                .stream()
                .filter(target -> target.getBoundingBox().inflate(0.5D).clip(eye, end).isPresent())
                .min(Comparator.comparingDouble(target -> player.distanceToSqr(target)));
    }

    private static int directionIndex(Player player, Vec3 target) {
        double dx = target.x - player.getX();
        double dz = target.z - player.getZ();
        double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        double relative = -Mth.wrapDegrees(targetAngle - player.getYRot());
        if (relative >= -22.5D && relative < 22.5D) return 0;
        if (relative >= -67.5D && relative < -22.5D) return 1;
        if (relative >= -112.5D && relative < -67.5D) return 2;
        if (relative >= -157.5D && relative < -112.5D) return 3;
        if (relative >= 157.5D || relative < -157.5D) return 4;
        if (relative >= 112.5D && relative < 157.5D) return 5;
        if (relative >= 67.5D && relative < 112.5D) return 6;
        return 7;
    }

    private static void drawDirection(GuiGraphics graphics, ResourceLocation texture, int direction) {
        if (direction == 0 || direction == 1 || direction == 7) {
            graphics.blit(texture, 40, -16, 26.0F, 75.0F, 5, 5, 128, 128);
        }
        if (direction == 1 || direction == 2 || direction == 3) {
            graphics.blit(texture, 50, -8, 14.0F, 75.0F, 5, 5, 128, 128);
        }
        if (direction == 3 || direction == 4 || direction == 5) {
            graphics.blit(texture, 40, 0, 34.0F, 75.0F, 5, 5, 128, 128);
        }
        if (direction == 5 || direction == 6 || direction == 7) {
            graphics.blit(texture, 30, -8, 19.0F, 75.0F, 5, 5, 128, 128);
        }
    }

    private enum Mode {
        OFF,
        NORMAL,
        MASTER_SEARCH
    }
}
