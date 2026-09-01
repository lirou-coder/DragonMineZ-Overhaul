package com.dmzrevamp.compat;

import com.dmzrevamp.revamp.ki.KiAttackOverhaul;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.techniques.Techniques;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Optional, reflection-only bridge for DMZ Skill Progression. */
public final class DmzSkillProgressionCompat {
    public static final String MODID = "dmzskillprogression";
    private static final ThreadLocal<ChargeContext> CHARGE_CONTEXT = new ThreadLocal<>();
    private static final Set<String> CUSTOM_STRIKE_IDS = ConcurrentHashMap.newKeySet();

    private DmzSkillProgressionCompat() {
    }

    public static boolean isLoaded() {
        try {
            return ModList.get().isLoaded(MODID);
        } catch (Throwable ignored) {
            return isLoadedEarly();
        }
    }

    public static boolean isLoadedEarly() {
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(MODID) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void beginTechniqueCharge(ServerPlayer player, StatsData data) {
        CHARGE_CONTEXT.set(new ChargeContext(player, data));
    }

    public static void endTechniqueCharge() {
        CHARGE_CONTEXT.remove();
    }

    public static float adjustChargePercent(Techniques techniques, float requestedPercent) {
        ChargeContext context = CHARGE_CONTEXT.get();
        if (context == null || context.data().getTechniques() != techniques) {
            return requestedPercent;
        }
        return KiAttackOverhaul.resolveChargePercentUpdate(context.player(), context.data(), requestedPercent);
    }

    public static void registerCustomStrike(String id) {
        if (id != null && !id.isBlank()) {
            CUSTOM_STRIKE_IDS.add(id.toLowerCase());
        }
    }

    public static boolean isCustomStrike(String id) {
        return id != null && CUSTOM_STRIKE_IDS.contains(id.toLowerCase());
    }

    public static boolean isOverhaulSignatureKiAttack(String id) {
        return "final_kamehameha".equalsIgnoreCase(id) || "big_bang_kamehameha".equalsIgnoreCase(id);
    }

    /**
     * Skill Progression trains Kaioken rank 0 by charging the selected x2 form while
     * deliberately keeping DMZ's native skill at level zero. The Overhaul level gate
     * must let that training charge reach the add-on's completion interceptor.
     */
    public static boolean isRankZeroKaiokenTraining(StatsData data, FormConfig.FormData form) {
        if (!isLoaded() || data == null || form == null || data.getCharacter() == null) {
            return false;
        }
        if (!"x2".equalsIgnoreCase(form.getName())
                || !"kaioken".equalsIgnoreCase(data.getCharacter().getSelectedStackFormGroup())
                || !"x2".equalsIgnoreCase(data.getCharacter().getSelectedStackForm())) {
            return false;
        }
        var kaioken = data.getSkills().getSkill("kaioken");
        return kaioken != null && kaioken.getLevel() == 0 && !data.getCharacter().hasActiveStackForm();
    }

    /** Clears the add-on's independent progression capability after a destructive skill reset. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void resetProgression(ServerPlayer player, boolean clearTechniques) {
        if (!isLoaded() || player == null) {
            return;
        }
        try {
            Class<?> capabilityClass = Class.forName("com.lcd.dmzskillprogression.capability.PlayerSkillCapability");
            Capability capability = (Capability) capabilityClass.getField("INSTANCE").get(null);
            Object optional = player.getCapability(capability);
            Object data = optional.getClass().getMethod("orElse", Object.class).invoke(optional, new Object[]{null});
            if (data == null) {
                return;
            }
            clearMap(data, "all");
            if (clearTechniques) {
                clearMap(data, "allTechniques");
            }
            syncProgression(player, data);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Compatibility must never make the base reset path fail when the add-on changes internals.
        }
    }

    private static void clearMap(Object data, String methodName) throws ReflectiveOperationException {
        Method method = data.getClass().getMethod(methodName);
        Object value = method.invoke(data);
        if (value instanceof Map<?, ?> map) {
            map.clear();
        }
    }

    private static void syncProgression(ServerPlayer player, Object data) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("com.lcd.dmzskillprogression.network.SkillDataSyncS2C");
        Constructor<?> constructor = packetClass.getConstructor(data.getClass());
        Object packet = constructor.newInstance(data);
        Field channelField = Class.forName("com.lcd.dmzskillprogression.network.DMZSkillProgressionNetwork")
                .getField("CHANNEL");
        SimpleChannel channel = (SimpleChannel) channelField.get(null);
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private record ChargeContext(ServerPlayer player, StatsData data) {
    }
}
