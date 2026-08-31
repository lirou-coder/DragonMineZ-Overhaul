package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzKiOverchargeCompat;
import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.compat.DmzSparkingCompat;
import com.dmzrevamp.compat.SduCompat;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public final class DmzRevampMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> DMZ_KI_OVERCHARGE_COMPAT_DISABLED_MIXINS = Set.of(
            "com.dmzrevamp.mixin.TechniqueDispatcherOverchargeMixin",
            "com.dmzrevamp.mixin.TechniquesOverchargeCapMixin",
            "com.dmzrevamp.mixin.TickHandlerTechniqueOverchargeMixin",
            "com.dmzrevamp.mixin.client.TechniqueChargeOverlayOverchargeMixin"
    );

    private boolean dmzKiOverchargeLoaded;
    private boolean dmzSkillProgressionLoaded;
    private boolean dmzSparkingLoaded;
    private boolean sduLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        dmzKiOverchargeLoaded = DmzKiOverchargeCompat.isLoadedEarly();
        dmzSkillProgressionLoaded = DmzSkillProgressionCompat.isLoadedEarly();
        dmzSparkingLoaded = DmzSparkingCompat.isLoadedEarly();
        sduLoaded = SduCompat.isLoadedEarly();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.dmzrevamp.mixin.compat.sdu.")) {
            return sduLoaded;
        }
        if (dmzSparkingLoaded && "com.dmzrevamp.mixin.QuestDefaultsRevampDefaultsMixin".equals(mixinClassName)) {
            return false;
        }
        if ("com.dmzrevamp.mixin.ConfigManagerSparkingCompatMixin".equals(mixinClassName)) return dmzSparkingLoaded;
        if ("com.dmzrevamp.mixin.compat.DmzBetterFormsTierCompatMixin".equals(mixinClassName)) {
            return isModLoadedEarly("dmzbetterforms") && hasClass(targetClassName);
        }
        if (isModLoadedEarly("dmzrealistic") && Set.of(
                "com.dmzrevamp.mixin.WeightItemRevampLimitMixin",
                "com.dmzrevamp.mixin.NPCActionWeightLimitMixin",
                "com.dmzrevamp.mixin.client.MasterTextScreenWeightLimitMixin"
        ).contains(mixinClassName)) return false;
        if ("com.dmzrevamp.mixin.client.ClientStatsEventsWeaponUseMixin".equals(mixinClassName) && isModLoadedEarly("dmzweaponguard")) return false;
        if ("com.dmzrevamp.mixin.CombatConfigAdaptiveDefenseDefaultsMixin".equals(mixinClassName)) {
            return hasField(targetClassName, "adaptativeMitigationParityValue");
        }
        if ("com.dmzrevamp.mixin.StatsDataAdaptiveDefenseCurveMixin".equals(mixinClassName)) {
            return hasMethod(targetClassName, "computeAdaptativeDefenseMitigation", "(D)D");
        }
        if ("com.dmzrevamp.mixin.MobBattlePowerHelperSpellPowerMixin".equals(mixinClassName)) {
            return hasClass(targetClassName);
        }
        if ("com.dmzrevamp.mixin.DmzKiOverchargeCastReductionMixin".equals(mixinClassName)) {
            return dmzKiOverchargeLoaded;
        }
        if ("com.dmzrevamp.mixin.client.TechniqueChargeOverlaySecondBarCompatMixin".equals(mixinClassName)) {
            return dmzKiOverchargeLoaded;
        }
        if ("com.dmzrevamp.mixin.SkillProgressionTechniqueChargeContextMixin".equals(mixinClassName)) {
            return dmzSkillProgressionLoaded && !dmzKiOverchargeLoaded;
        }
        if ("com.dmzrevamp.mixin.SkillProgressionTechniqueRegistryMixin".equals(mixinClassName)) {
            return dmzSkillProgressionLoaded;
        }
        if ("com.dmzrevamp.mixin.AutoLevelingQuestSpawnSkipMixin".equals(mixinClassName)) {
            return hasClass(targetClassName);
        }
        if ("com.dmzrevamp.mixin.AttributeFixDefaultsMixin".equals(mixinClassName)) {
            return hasClass(targetClassName);
        }
        if ("com.dmzrevamp.mixin.SagaVegettoBaseTransformMixin".equals(mixinClassName)) {
            return hasClass(targetClassName) && hasField("com.dragonminez.common.init.MainEntities", "SAGA_VEGETTO_SSJ");
        }
        return !dmzKiOverchargeLoaded || !DMZ_KI_OVERCHARGE_COMPAT_DISABLED_MIXINS.contains(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean hasClass(String className) {
        return classBytes(className) != null;
    }

    private static boolean hasField(String className, String fieldName) {
        ClassNode node = classNode(className);
        return node != null && node.fields.stream().anyMatch(field -> fieldName.equals(field.name));
    }

    private static boolean hasMethod(String className, String methodName, String descriptor) {
        ClassNode node = classNode(className);
        return node != null && node.methods.stream().anyMatch(method -> methodName.equals(method.name) && descriptor.equals(method.desc));
    }

    private static ClassNode classNode(String className) {
        byte[] bytes = classBytes(className);
        if (bytes == null) {
            return null;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static byte[] classBytes(String className) {
        String path = className.replace('.', '/') + ".class";
        ClassLoader loader = DmzRevampMixinPlugin.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(path)) {
            return input == null ? null : input.readAllBytes();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static boolean isModLoadedEarly(String modId) {
        try {
            return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList() != null
                    && net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById(modId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
