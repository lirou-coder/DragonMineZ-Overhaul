package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientKiExtraEffectSelection;
import com.dmzrevamp.client.ClientStrikeCreatorMode;
import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.network.CreateStrikeTechniqueC2SPacket;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.UpdateKiTechniqueExtrasC2SPacket;
import com.dmzrevamp.revamp.ki.KiAttackArchetype;
import com.dmzrevamp.revamp.ki.KiAttackCategoryRules;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.StrikeAttackCategoryRules;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.character.TechniqueCreatorScreen;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(TechniqueCreatorScreen.class)
public abstract class TechniqueCreatorScreenRevampMixin extends ScaledScreen {
    @Unique
    private static final ResourceLocation DMZREVAMP_EXTRA_TECH_MENU = ResourceLocation.fromNamespaceAndPath("dmzrevamp", "textures/gui/menu/extra_tech_menu.png");
    @Unique
    private static final ResourceLocation DMZREVAMP_EXTRA_TECH_MENU_2 = ResourceLocation.fromNamespaceAndPath("dmzrevamp", "textures/gui/menu/extra_tech_menu_2.png");
    @Unique
    private static final int DMZREVAMP_EFFECT_ROW_START = 14;
    @Unique
    private static final int DMZREVAMP_EFFECT_ROW_STEP = 18;
    @Unique
    private static final int DMZREVAMP_EXTRA_MENU_SRC_WIDTH = 141;
    @Unique
    private static final int DMZREVAMP_EXTRA_MENU_SRC_HEIGHT = 178;
    @Unique
    private static final int DMZREVAMP_EXTRA_MENU_WIDTH = 141;
    @Unique
    private static final int DMZREVAMP_EXTRA_MENU_HEIGHT = 178;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_MENU_WIDTH = 344;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_MENU_HEIGHT = 270;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_TEXTURE_SIZE = 1024;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_LEFT_ARROW_U = 184;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_RIGHT_ARROW_U = 137;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_V = 282;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_HOVER_V = 326;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_WIDTH = 24;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_HEIGHT = 40;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_HOVER_WIDTH = 26;
    @Unique
    private static final int DMZREVAMP_PAGE_TWO_ARROW_HOVER_HEIGHT = 42;
    @Unique
    private static final int DMZREVAMP_STRIKE_BASE_COOLDOWN_TICKS = 240;
    @Unique
    private static final int DMZREVAMP_EVASIVE_BASE_COOLDOWN_TICKS = 400;

    @Shadow(remap = false)
    private String creatorName;
    @Shadow(remap = false)
    private KiAttackData.KiType creatorType;
    @Shadow(remap = false)
    private KiAttackData.Utility creatorUtility;
    @Shadow(remap = false)
    private float creatorDamage;
    @Shadow(remap = false)
    private float creatorSize;
    @Shadow(remap = false)
    private float creatorSpeed;
    @Shadow(remap = false)
    private int creatorArmorPen;
    @Shadow(remap = false)
    private int creatorCooldown;
    @Shadow(remap = false)
    private float kiCost;
    @Shadow(remap = false)
    private float tpCost;
    @Shadow(remap = false)
    private KiAttackData.SecondaryEffectType creatorSecondaryType;
    @Shadow(remap = false)
    private KiAttackData.AffectedStat creatorAffectedStat;
    @Shadow(remap = false)
    private int creatorSecondaryIntensity;
    @Shadow(remap = false)
    private int creatorSecondaryDuration;
    @Shadow(remap = false)
    private CustomTextureButton sizeLeft;
    @Shadow(remap = false)
    private CustomTextureButton sizeRight;
    @Shadow(remap = false)
    private CustomTextureButton armorLeft;
    @Shadow(remap = false)
    private CustomTextureButton armorRight;
    @Shadow(remap = false)
    private int panelX;
    @Shadow(remap = false)
    private int panelY;
    @Shadow(remap = false)
    private void recomputeDerivedValues() {
    }
    @Shadow(remap = false)
    private void setCreatorType(KiAttackData.KiType newType) {
    }
    @Shadow(remap = false)
    private void updateAdjusterVisibility() {
    }

    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$thirdType = KiAttackData.SecondaryEffectType.NONE;
    @Unique
    private KiAttackData.AffectedStat dmzrevamp$thirdStat = KiAttackData.AffectedStat.STR;
    @Unique
    private int dmzrevamp$thirdIntensity = 5;
    @Unique
    private int dmzrevamp$thirdDuration = 1;
    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$fourthType = KiAttackData.SecondaryEffectType.NONE;
    @Unique
    private KiAttackData.AffectedStat dmzrevamp$fourthStat = KiAttackData.AffectedStat.STR;
    @Unique
    private int dmzrevamp$fourthIntensity = 5;
    @Unique
    private int dmzrevamp$fourthDuration = 1;
    @Unique
    private final ClientKiExtraEffectSelection dmzrevamp$extraOne = new ClientKiExtraEffectSelection();
    @Unique
    private final ClientKiExtraEffectSelection dmzrevamp$extraTwo = new ClientKiExtraEffectSelection();
    @Unique
    private KiAttackArchetype dmzrevamp$archetype = KiAttackArchetype.NORMAL;
    @Unique
    private int dmzrevamp$projectileCount = 1;
    @Unique
    private int dmzrevamp$domainDuration = 30;
    @Unique
    private KiAttackData.SecondaryEffectType dmzrevamp$domainSecondaryBeforeRecompute = null;
    @Unique
    private boolean dmzrevamp$areaBothUtility = false;
    @Unique
    private CustomTextureButton dmzrevamp$projectilesLeft;
    @Unique
    private CustomTextureButton dmzrevamp$projectilesRight;
    @Unique
    private CustomTextureButton dmzrevamp$areaSizeLeft;
    @Unique
    private CustomTextureButton dmzrevamp$areaSizeRight;
    @Unique
    private final List<CustomTextureButton> dmzrevamp$extraOneModeDependentButtons = new ArrayList<>();
    @Unique
    private final List<CustomTextureButton> dmzrevamp$extraTwoModeDependentButtons = new ArrayList<>();
    @Unique
    private final List<AbstractWidget> dmzrevamp$pageTwoButtons = new ArrayList<>();
    @Unique
    private final Map<AbstractWidget, Boolean> dmzrevamp$pageOneWidgetVisibleStates = new IdentityHashMap<>();
    @Unique
    private final Map<AbstractWidget, Boolean> dmzrevamp$pageOneWidgetActiveStates = new IdentityHashMap<>();
    @Unique
    private boolean dmzrevamp$secondPage = false;
    @Unique
    private boolean dmzrevamp$strikeCreator = false;
    @Unique
    private CustomStrikeType dmzrevamp$strikeType = CustomStrikeType.BASIC;

    protected TechniqueCreatorScreenRevampMixin(Component title) {
        super(title);
    }

    @Inject(method = "m_7856_", at = @At("RETURN"), remap = false)
    private void dmzrevamp$addExtendedTechniqueControls(CallbackInfo ci) {
        if (ClientStrikeCreatorMode.consumeNextCreatorIsStrike()) {
            dmzrevamp$strikeCreator = true;
            dmzrevamp$applyStrikeCreatorDefaults();
        }
        int x = this.panelX;
        int y = this.panelY;
        dmzrevamp$disableVanillaTypeArrows(x, y);
        dmzrevamp$addTypeSelectorOverlay(x, y);
        dmzrevamp$addAreaSizeControls(x, y);
        if (!dmzrevamp$skillProgressionCreator()) {
            dmzrevamp$addProjectileCountControls(x, y);
            dmzrevamp$addThirdEffectControls(x + 19, x + 139, y + 18);
            dmzrevamp$addFourthEffectControls(x + 195, x + 315, y + 18);
            dmzrevamp$addExtraEffectControls(x + 19, x + 139, y + 152, dmzrevamp$extraOne);
            dmzrevamp$addExtraEffectControls(x + 195, x + 315, y + 152, dmzrevamp$extraTwo);
        }
        // Vanilla initialized these arrows before strike mode was known, leaving Armor Pen hidden/disabled.
        updateAdjusterVisibility();
        dmzrevamp$updateCreatorPageWidgets();
    }

    @Inject(method = "createSkill", at = @At("HEAD"), remap = false)
    private void dmzrevamp$normalizeCustomArchetypeBeforeCreate(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            return;
        }
        if (dmzrevamp$areaBothEnabled()) {
            creatorUtility = KiAttackData.Utility.HEAL;
        }
    }

    @Inject(method = "createSkill", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$createCustomStrike(CallbackInfo ci) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        String name = creatorName == null || creatorName.trim().isEmpty()
                ? "New Strike"
                : creatorName.trim();
        DmzRevampNetwork.CHANNEL.sendToServer(new CreateStrikeTechniqueC2SPacket(
                name,
                dmzrevamp$strikeType.name(),
                creatorDamage,
                creatorSpeed,
                creatorArmorPen,
                creatorSecondaryType.name(),
                creatorSecondaryType == KiAttackData.SecondaryEffectType.NONE ? "" : creatorAffectedStat.name(),
                creatorSecondaryIntensity,
                creatorSecondaryDuration,
                dmzrevamp$thirdType.name(),
                dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE ? "" : dmzrevamp$thirdStat.name(),
                dmzrevamp$thirdIntensity,
                dmzrevamp$thirdDuration,
                dmzrevamp$fourthType.name(),
                dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE ? "" : dmzrevamp$fourthStat.name(),
                dmzrevamp$fourthIntensity,
                dmzrevamp$fourthDuration,
                dmzrevamp$extraOne.mode.name(),
                dmzrevamp$extraOne.effectId,
                dmzrevamp$extraOne.level,
                dmzrevamp$extraOne.duration,
                dmzrevamp$extraTwo.mode.name(),
                dmzrevamp$extraTwo.effectId,
                dmzrevamp$extraTwo.level,
                dmzrevamp$extraTwo.duration
        ));
        ClientStrikeCreatorMode.markNextSkillsMenuAsStrike();
        this.onClose();
        ci.cancel();
    }

    @Inject(method = "createSkill", at = @At("RETURN"), remap = false)
    private void dmzrevamp$sendExtendedTechniqueData(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            return;
        }
        String name = creatorName == null || creatorName.trim().isEmpty()
                ? Component.translatable("gui.dragonminez.skills.new_skill").getString()
                : creatorName.trim();

        DmzRevampNetwork.CHANNEL.sendToServer(new UpdateKiTechniqueExtrasC2SPacket(
                name,
                dmzrevamp$thirdType.name(),
                dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE ? "" : dmzrevamp$thirdStat.name(),
                dmzrevamp$thirdIntensity,
                dmzrevamp$thirdDuration,
                dmzrevamp$fourthType.name(),
                dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE ? "" : dmzrevamp$fourthStat.name(),
                dmzrevamp$fourthIntensity,
                dmzrevamp$fourthDuration,
                dmzrevamp$extraOne.mode.name(),
                dmzrevamp$extraOne.effectId,
                dmzrevamp$extraOne.level,
                dmzrevamp$extraOne.duration,
                dmzrevamp$extraTwo.mode.name(),
                dmzrevamp$extraTwo.effectId,
                dmzrevamp$extraTwo.level,
                dmzrevamp$extraTwo.duration,
                dmzrevamp$archetype.name(),
                dmzrevamp$projectileCount(),
                dmzrevamp$domainDuration,
                dmzrevamp$areaBothEnabled(),
                false,
                creatorSize
        ));
    }

    @Inject(
            method = "m_88315_",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dmzrevamp$renderOnlySecondTechniqueCreatorPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!dmzrevamp$secondPage || dmzrevamp$skillProgressionCreator()) {
            return;
        }

        this.renderBackground(graphics);
        int uiMouseX = Math.round((float) this.toUiX(mouseX));
        int uiMouseY = Math.round((float) this.toUiY(mouseY));
        dmzrevamp$updateCreatorPageWidgets();
        this.beginUiScale(graphics);
        dmzrevamp$renderSecondPage(graphics, this.panelX, this.panelY, uiMouseX, uiMouseY);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0D, 0.0D, 400.0D);
        super.render(graphics, uiMouseX, uiMouseY, partialTick);
        pose.popPose();
        this.endUiScale(graphics);
        ci.cancel();
    }

    @Inject(
            method = "m_88315_",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/util/ScaledScreen;m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.BEFORE),
            remap = false
    )
    private void dmzrevamp$renderExtendedTechniqueLabels(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int x = this.panelX;
        int y = this.panelY;
        int uiMouseX = Math.round((float) this.toUiX(mouseX));
        int uiMouseY = Math.round((float) this.toUiY(mouseY));
        dmzrevamp$updateCreatorPageWidgets();
        if (dmzrevamp$secondPage) {
            dmzrevamp$renderSecondPage(graphics, x, y, uiMouseX, uiMouseY);
        } else {
            dmzrevamp$renderSkillCategory(graphics, x + 67, y + 60);
            if (!dmzrevamp$skillProgressionCreator()) {
                dmzrevamp$renderProjectiles(graphics, x + 84, y + 228);
                dmzrevamp$renderPageArrow(graphics, x, y, false, uiMouseX, uiMouseY);
            }
        }
    }

    @Inject(method = "recomputeDerivedValues", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$rememberDomainSecondary(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            dmzrevamp$applyStrikeCreatorDerivedValues();
            ci.cancel();
            return;
        }
        dmzrevamp$domainSecondaryBeforeRecompute = dmzrevamp$areaBothEnabled() ? creatorSecondaryType : null;
    }

    @Inject(method = "recomputeDerivedValues", at = @At("RETURN"), remap = false)
    private void dmzrevamp$includeExtendedCreatorCosts(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            dmzrevamp$applyStrikeCreatorDerivedValues();
            return;
        }
        if (dmzrevamp$areaBothEnabled() && dmzrevamp$domainSecondaryBeforeRecompute != null) {
            creatorSecondaryType = dmzrevamp$domainSecondaryBeforeRecompute;
        }
        if (creatorType == KiAttackData.KiType.AREA) {
            creatorSize = Mth.clamp(creatorSize, 0.1F, 15.0F);
        } else {
            dmzrevamp$areaBothUtility = false;
        }
        dmzrevamp$applyExtendedDerivedValues();
    }

    @Inject(method = "cycleSecondaryType", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$cycleDomainSecondaryFreely(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            return;
        }
        if (!dmzrevamp$areaBothEnabled()) {
            return;
        }
        creatorSecondaryType = switch (creatorSecondaryType) {
            case NONE -> KiAttackData.SecondaryEffectType.BUFF;
            case BUFF -> KiAttackData.SecondaryEffectType.DEBUFF;
            case DEBUFF -> KiAttackData.SecondaryEffectType.NONE;
        };
        recomputeDerivedValues();
        ci.cancel();
    }

    @Inject(method = "toggleUtility", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$cycleAreaBothUtility(CallbackInfo ci) {
        if (dmzrevamp$strikeCreator) {
            creatorUtility = dmzrevamp$strikeType.isEvasive() ? KiAttackData.Utility.HEAL : KiAttackData.Utility.DAMAGE;
            recomputeDerivedValues();
            ci.cancel();
            return;
        }
        if (dmzrevamp$archetype == KiAttackArchetype.NORMAL && creatorType == KiAttackData.KiType.AREA) {
            if (dmzrevamp$areaBothUtility) {
                dmzrevamp$areaBothUtility = false;
                creatorUtility = KiAttackData.Utility.DAMAGE;
            } else if (creatorUtility == KiAttackData.Utility.DAMAGE) {
                creatorUtility = KiAttackData.Utility.HEAL;
            } else {
                creatorUtility = KiAttackData.Utility.HEAL;
                dmzrevamp$areaBothUtility = true;
            }
            recomputeDerivedValues();
            ci.cancel();
        }
    }

    @Inject(method = "toggleUtility", at = @At("RETURN"), remap = false)
    private void dmzrevamp$lockDomainUtilityToHeal(CallbackInfo ci) {
        // Domain was removed; Area + Both now provides that role through normal Area rules.
    }

    @Inject(method = "updateAdjusterVisibility", at = @At("RETURN"), remap = false)
    private void dmzrevamp$updateProjectileControls(CallbackInfo ci) {
        if (dmzrevamp$projectilesLeft != null && dmzrevamp$projectilesRight != null) {
            boolean enabled = dmzrevamp$projectilesEnabled();
            dmzrevamp$projectilesLeft.active = enabled;
            dmzrevamp$projectilesRight.active = enabled;
            dmzrevamp$projectilesLeft.visible = enabled;
            dmzrevamp$projectilesRight.visible = enabled;
            if (!enabled) {
                dmzrevamp$projectileCount = 1;
            }
        }
        dmzrevamp$updateAreaSizeControls();
        dmzrevamp$updateExtraEffectControls(dmzrevamp$extraOne, dmzrevamp$extraOneModeDependentButtons);
        dmzrevamp$updateExtraEffectControls(dmzrevamp$extraTwo, dmzrevamp$extraTwoModeDependentButtons);
        if (dmzrevamp$strikeCreator) {
            if (sizeLeft != null && sizeRight != null) {
                sizeLeft.visible = false;
                sizeRight.visible = false;
                sizeLeft.active = false;
                sizeRight.active = false;
            }
            if (armorLeft != null && armorRight != null) {
                boolean armorEnabled = !dmzrevamp$strikeType.isEvasive();
                armorLeft.visible = armorEnabled;
                armorRight.visible = armorEnabled;
                armorLeft.active = armorEnabled;
                armorRight.active = armorEnabled;
            }
        }
        dmzrevamp$updateCreatorPageWidgets();
    }

    @Redirect(
            method = {"updateAdjusterVisibility", "renderBaseEffects"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;usesCustomArmorPen(Lcom/dragonminez/common/stats/techniques/KiAttackData$KiType;)Z"),
            require = 0,
            remap = false
    )
    private boolean dmzrevamp$allowArmorPenForStrikeCreator(KiAttackData.KiType type) {
        // Strike creation reuses the Ki creator screen, so this tells DMZ that non-evasive strikes can edit Armor Pen.
        return dmzrevamp$strikeCreator ? !dmzrevamp$strikeType.isEvasive() : KiAttackData.usesCustomArmorPen(type);
    }

    @Inject(method = "adjustDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$adjustStrikeDamage(boolean increase, CallbackInfo ci) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        float step = hasShiftDown() ? 0.01F : 0.05F;
        creatorDamage = Mth.clamp(creatorDamage + (increase ? step : -step), dmzrevamp$strikeType.minDamageMultiplier(), dmzrevamp$strikeType.maxDamageMultiplier());
        dmzrevamp$applyStrikeCreatorDerivedValues();
        ci.cancel();
    }

    @Inject(method = "adjustSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$adjustStrikeSpeed(boolean increase, CallbackInfo ci) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        if (dmzrevamp$strikeType.isEvasive()) {
            creatorSpeed = 0.0F;
        } else {
            float step = hasShiftDown() ? 0.01F : 0.05F;
            creatorSpeed = Mth.clamp(creatorSpeed + (increase ? step : -step), 0.1F, 1.5F);
        }
        dmzrevamp$applyStrikeCreatorDerivedValues();
        ci.cancel();
    }

    @Inject(method = "adjustSize", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$lockStrikeSize(boolean increase, CallbackInfo ci) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        creatorSize = 1.0F;
        dmzrevamp$applyStrikeCreatorDerivedValues();
        ci.cancel();
    }

    @Inject(method = "adjustArmor", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$adjustStrikeArmor(boolean increase, CallbackInfo ci) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        if (dmzrevamp$strikeType.isEvasive()) {
            creatorArmorPen = 0;
        } else {
            int step = hasShiftDown() ? 1 : 1;
            creatorArmorPen = Mth.clamp(creatorArmorPen + (increase ? step : -step), 0, 10);
        }
        dmzrevamp$applyStrikeCreatorDerivedValues();
        ci.cancel();
    }

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$handleTechniqueCreatorPageArrow(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || dmzrevamp$skillProgressionCreator()) {
            return;
        }
        double uiMouseX = this.toUiX(mouseX);
        double uiMouseY = this.toUiY(mouseY);
        if (!dmzrevamp$secondPage && dmzrevamp$isPageArrowHovered(uiMouseX, uiMouseY, false)) {
            dmzrevamp$playPageArrowClickSound();
            dmzrevamp$secondPage = true;
            dmzrevamp$updateCreatorPageWidgets();
            cir.setReturnValue(true);
            return;
        }
        if (dmzrevamp$secondPage && dmzrevamp$isPageArrowHovered(uiMouseX, uiMouseY, true)) {
            dmzrevamp$playPageArrowClickSound();
            dmzrevamp$secondPage = false;
            dmzrevamp$updateCreatorPageWidgets();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void dmzrevamp$addTypeSelectorOverlay(int x, int y) {
        CustomTextureButton left = dmzrevamp$createArrowButton(x + 16, y + 34, true, btn -> dmzrevamp$cycleDisplayType(false));
        CustomTextureButton right = dmzrevamp$createArrowButton(x + 118, y + 34, false, btn -> dmzrevamp$cycleDisplayType(true));
        // Skill Progression hides every arrow created after DMZ's compact-panel
        // controls. These two are the one intentional exception: they select
        // Ki type or the Overhaul Strike archetype and must remain interactive.
        left.visible = true;
        left.active = true;
        right.visible = true;
        right.active = true;
        this.addRenderableWidget(left);
        this.addRenderableWidget(right);
    }

    @Unique
    private CustomTextureButton dmzrevamp$addPageTwoButton(CustomTextureButton button) {
        CustomTextureButton added = this.addRenderableWidget(button);
        dmzrevamp$pageTwoButtons.add(added);
        return added;
    }

    @Unique
    private void dmzrevamp$updateCreatorPageWidgets() {
        if (dmzrevamp$secondPage) {
            dmzrevamp$hidePageOneWidgets();
        } else {
            dmzrevamp$restorePageOneWidgets();
            dmzrevamp$setPageTwoButtonsVisible(false);
        }
    }

    @Unique
    private void dmzrevamp$hidePageOneWidgets() {
        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (!(child instanceof AbstractWidget widget) || dmzrevamp$pageTwoButtons.contains(widget) || dmzrevamp$isDoneOrCancelButton(widget)) {
                continue;
            }
            dmzrevamp$pageOneWidgetVisibleStates.putIfAbsent(widget, widget.visible);
            dmzrevamp$pageOneWidgetActiveStates.putIfAbsent(widget, widget.active);
            widget.visible = false;
            widget.active = false;
        }
        dmzrevamp$setPageTwoButtonsVisible(true);
    }

    @Unique
    private void dmzrevamp$restorePageOneWidgets() {
        for (Map.Entry<AbstractWidget, Boolean> entry : dmzrevamp$pageOneWidgetVisibleStates.entrySet()) {
            AbstractWidget widget = entry.getKey();
            widget.visible = entry.getValue();
            widget.active = dmzrevamp$pageOneWidgetActiveStates.getOrDefault(widget, widget.active);
        }
        dmzrevamp$pageOneWidgetVisibleStates.clear();
        dmzrevamp$pageOneWidgetActiveStates.clear();
    }

    @Unique
    private void dmzrevamp$setPageTwoButtonsVisible(boolean visible) {
        for (AbstractWidget widget : dmzrevamp$pageTwoButtons) {
            widget.visible = visible;
            widget.active = visible;
        }
        dmzrevamp$updateExtraEffectControls(dmzrevamp$extraOne, dmzrevamp$extraOneModeDependentButtons);
        dmzrevamp$updateExtraEffectControls(dmzrevamp$extraTwo, dmzrevamp$extraTwoModeDependentButtons);
    }

    @Unique
    private boolean dmzrevamp$isDoneOrCancelButton(AbstractWidget widget) {
        int buttonY = this.getUiHeight() - 28;
        int createX = this.panelX + 172 - 78;
        int cancelX = this.panelX + 172 + 4;
        return widget.getY() == buttonY && (widget.getX() == createX || widget.getX() == cancelX);
    }

    @Unique
    private void dmzrevamp$addProjectileCountControls(int x, int y) {
        dmzrevamp$projectilesLeft = this.addRenderableWidget(dmzrevamp$createArrowButton(x + 18, y + 228, true, btn -> {
            if (dmzrevamp$projectilesEnabled()) {
                dmzrevamp$projectileCount = Mth.clamp(dmzrevamp$projectileCount - 1, 1, dmzrevamp$maxProjectileCount());
                recomputeDerivedValues();
            }
        }));
        dmzrevamp$projectilesRight = this.addRenderableWidget(dmzrevamp$createArrowButton(x + 150, y + 228, false, btn -> {
            if (dmzrevamp$projectilesEnabled()) {
                dmzrevamp$projectileCount = Mth.clamp(dmzrevamp$projectileCount + 1, 1, dmzrevamp$maxProjectileCount());
                recomputeDerivedValues();
            }
        }));
        boolean enabled = dmzrevamp$projectilesEnabled();
        dmzrevamp$projectilesLeft.active = enabled;
        dmzrevamp$projectilesRight.active = enabled;
        dmzrevamp$projectilesLeft.visible = enabled;
        dmzrevamp$projectilesRight.visible = enabled;
    }

    @Unique
    private void dmzrevamp$addAreaSizeControls(int x, int y) {
        dmzrevamp$areaSizeLeft = this.addRenderableWidget(dmzrevamp$createArrowButton(x + 18, y + 148, true, btn -> {
            if (dmzrevamp$customAreaSizeEnabled()) {
                creatorSize = dmzrevamp$clampCreatorSize(creatorSize - 0.1F);
                recomputeDerivedValues();
            }
        }));
        dmzrevamp$areaSizeRight = this.addRenderableWidget(dmzrevamp$createArrowButton(x + 150, y + 148, false, btn -> {
            if (dmzrevamp$customAreaSizeEnabled()) {
                creatorSize = dmzrevamp$clampCreatorSize(creatorSize + 0.1F);
                recomputeDerivedValues();
            }
        }));
        dmzrevamp$updateAreaSizeControls();
    }

    @Unique
    private void dmzrevamp$updateAreaSizeControls() {
        if (dmzrevamp$areaSizeLeft == null || dmzrevamp$areaSizeRight == null) {
            return;
        }
        boolean enabled = dmzrevamp$customAreaSizeEnabled();
        dmzrevamp$areaSizeLeft.active = enabled;
        dmzrevamp$areaSizeRight.active = enabled;
        dmzrevamp$areaSizeLeft.visible = enabled;
        dmzrevamp$areaSizeRight.visible = enabled;
    }

    @Unique
    private void dmzrevamp$disableVanillaTypeArrows(int x, int y) {
        int leftX = x + 16;
        int rightX = x + 118;
        int buttonY = y + 30;
        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                    && (widget.getY() == buttonY || widget.getY() == buttonY + 4)
                    && (widget.getX() == leftX || widget.getX() == rightX)) {
                widget.active = false;
                widget.visible = false;
            }
        }
    }

    @Unique
    private void dmzrevamp$addThirdEffectControls(int leftX, int rightX, int y) {
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 0), true, btn -> dmzrevamp$cycleThirdType()));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 0), false, btn -> dmzrevamp$cycleThirdType()));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 1), true, btn -> {
            dmzrevamp$thirdStat = dmzrevamp$prev(dmzrevamp$thirdStat, KiAttackData.AffectedStat.values());
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 1), false, btn -> {
            dmzrevamp$thirdStat = dmzrevamp$next(dmzrevamp$thirdStat, KiAttackData.AffectedStat.values());
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 2), true, btn -> {
            dmzrevamp$thirdIntensity = Mth.clamp(dmzrevamp$thirdIntensity - 5, 5, 50);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 2), false, btn -> {
            dmzrevamp$thirdIntensity = Mth.clamp(dmzrevamp$thirdIntensity + 5, 5, 50);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 3), true, btn -> {
            dmzrevamp$thirdDuration = Mth.clamp(dmzrevamp$thirdDuration - 1, 1, 8);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 3), false, btn -> {
            dmzrevamp$thirdDuration = Mth.clamp(dmzrevamp$thirdDuration + 1, 1, 8);
            recomputeDerivedValues();
        }));
    }

    @Unique
    private void dmzrevamp$addFourthEffectControls(int leftX, int rightX, int y) {
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 0), true, btn -> dmzrevamp$cycleFourthType()));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 0), false, btn -> dmzrevamp$cycleFourthType()));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 1), true, btn -> {
            dmzrevamp$fourthStat = dmzrevamp$prev(dmzrevamp$fourthStat, KiAttackData.AffectedStat.values());
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 1), false, btn -> {
            dmzrevamp$fourthStat = dmzrevamp$next(dmzrevamp$fourthStat, KiAttackData.AffectedStat.values());
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 2), true, btn -> {
            dmzrevamp$fourthIntensity = Mth.clamp(dmzrevamp$fourthIntensity - 5, 5, 50);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 2), false, btn -> {
            dmzrevamp$fourthIntensity = Mth.clamp(dmzrevamp$fourthIntensity + 5, 5, 50);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 3), true, btn -> {
            dmzrevamp$fourthDuration = Mth.clamp(dmzrevamp$fourthDuration - 1, 1, 8);
            recomputeDerivedValues();
        }));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 3), false, btn -> {
            dmzrevamp$fourthDuration = Mth.clamp(dmzrevamp$fourthDuration + 1, 1, 8);
            recomputeDerivedValues();
        }));
    }

    @Unique
    private void dmzrevamp$addExtraEffectControls(int leftX, int rightX, int y, ClientKiExtraEffectSelection effect) {
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 0), true, btn -> dmzrevamp$cycleExtraMode(effect)));
        dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 0), false, btn -> dmzrevamp$cycleExtraMode(effect)));
        List<CustomTextureButton> modeDependent = effect == dmzrevamp$extraOne ? dmzrevamp$extraOneModeDependentButtons : dmzrevamp$extraTwoModeDependentButtons;
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 1), true, btn -> {
            effect.cycleEffect(false);
            recomputeDerivedValues();
        })));
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 1), false, btn -> {
            effect.cycleEffect(true);
            recomputeDerivedValues();
        })));
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 2), true, btn -> {
            effect.level = Mth.clamp(effect.level - 1, 1, 5);
            recomputeDerivedValues();
        })));
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 2), false, btn -> {
            effect.level = Mth.clamp(effect.level + 1, 1, 5);
            recomputeDerivedValues();
        })));
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(leftX, dmzrevamp$effectRowY(y, 3), true, btn -> {
            effect.adjustDuration(-1);
            recomputeDerivedValues();
        })));
        modeDependent.add(dmzrevamp$addPageTwoButton(dmzrevamp$createArrowButton(rightX, dmzrevamp$effectRowY(y, 3), false, btn -> {
            effect.adjustDuration(1);
            recomputeDerivedValues();
        })));
        dmzrevamp$updateExtraEffectControls(effect, modeDependent);
    }

    @Unique
    private void dmzrevamp$cycleThirdType() {
        if (dmzrevamp$areaBothEnabled()) {
            dmzrevamp$thirdType = switch (dmzrevamp$thirdType) {
                case NONE -> KiAttackData.SecondaryEffectType.BUFF;
                case BUFF -> KiAttackData.SecondaryEffectType.DEBUFF;
                case DEBUFF -> KiAttackData.SecondaryEffectType.NONE;
            };
        } else {
            dmzrevamp$thirdType = dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE
                    ? (creatorUtility == KiAttackData.Utility.HEAL ? KiAttackData.SecondaryEffectType.BUFF : KiAttackData.SecondaryEffectType.DEBUFF)
                    : KiAttackData.SecondaryEffectType.NONE;
        }
        recomputeDerivedValues();
    }

    @Unique
    private void dmzrevamp$cycleFourthType() {
        if (dmzrevamp$areaBothEnabled()) {
            dmzrevamp$fourthType = switch (dmzrevamp$fourthType) {
                case NONE -> KiAttackData.SecondaryEffectType.BUFF;
                case BUFF -> KiAttackData.SecondaryEffectType.DEBUFF;
                case DEBUFF -> KiAttackData.SecondaryEffectType.NONE;
            };
        } else {
            dmzrevamp$fourthType = dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE
                    ? (creatorUtility == KiAttackData.Utility.HEAL ? KiAttackData.SecondaryEffectType.BUFF : KiAttackData.SecondaryEffectType.DEBUFF)
                    : KiAttackData.SecondaryEffectType.NONE;
        }
        recomputeDerivedValues();
    }

    @Unique
    private void dmzrevamp$cycleExtraMode(ClientKiExtraEffectSelection effect) {
        if (dmzrevamp$areaBothEnabled()) {
            effect.mode = switch (effect.mode) {
                case NONE -> KiAttackExtraEffect.Mode.BENEFICIAL;
                case BENEFICIAL -> KiAttackExtraEffect.Mode.HARMFUL;
                case HARMFUL -> KiAttackExtraEffect.Mode.NONE;
            };
        } else {
            effect.mode = effect.mode == KiAttackExtraEffect.Mode.NONE
                    ? (creatorUtility == KiAttackData.Utility.HEAL ? KiAttackExtraEffect.Mode.BENEFICIAL : KiAttackExtraEffect.Mode.HARMFUL)
                    : KiAttackExtraEffect.Mode.NONE;
        }
        effect.onModeChanged();
        dmzrevamp$updateExtraEffectControls(effect, effect == dmzrevamp$extraOne ? dmzrevamp$extraOneModeDependentButtons : dmzrevamp$extraTwoModeDependentButtons);
        recomputeDerivedValues();
    }

    @Unique
    private void dmzrevamp$updateExtraEffectControls(ClientKiExtraEffectSelection effect, List<CustomTextureButton> buttons) {
        boolean enabled = effect.mode != KiAttackExtraEffect.Mode.NONE;
        for (CustomTextureButton button : buttons) {
            button.active = dmzrevamp$secondPage && enabled;
            button.visible = dmzrevamp$secondPage;
        }
    }

    @Unique
    private void dmzrevamp$cycleDisplayType(boolean next) {
        if (dmzrevamp$strikeCreator) {
            CustomStrikeType[] values = CustomStrikeType.values();
            int index = dmzrevamp$strikeType.ordinal();
            index = next ? (index + 1) % values.length : (index - 1 + values.length) % values.length;
            dmzrevamp$strikeType = values[index];
            dmzrevamp$applyStrikeCreatorDefaults();
            recomputeDerivedValues();
            return;
        }
        int current = dmzrevamp$currentDisplayTypeIndex();
        int size = KiAttackData.KiType.values().length;
        int index = next ? (current + 1) % size : (current - 1 + size) % size;
        dmzrevamp$applyDisplayTypeIndex(index);
    }

    @Unique
    private int dmzrevamp$currentDisplayTypeIndex() {
        return creatorType.ordinal();
    }

    @Unique
    private void dmzrevamp$applyDisplayTypeIndex(int index) {
        KiAttackData.KiType[] vanilla = KiAttackData.KiType.values();
        if (index < vanilla.length) {
            dmzrevamp$archetype = KiAttackArchetype.NORMAL;
            dmzrevamp$areaBothUtility = false;
            setCreatorType(vanilla[index]);
            return;
        }
    }

    @Unique
    private String dmzrevamp$categoryPreview() {
        if (dmzrevamp$skillProgressionCreator()) {
            return "???";
        }
        if (dmzrevamp$strikeCreator) {
            return switch (StrikeAttackCategoryRules.classifyPreview(creatorDamage, dmzrevamp$activePreviewEffectCount())) {
                case ULTIMATE -> "Ultimate";
                case ADVANCED -> "Advanced";
                case BASIC -> "Basic";
            };
        }
        return switch (KiAttackCategoryRules.classifyPreview(creatorDamage, dmzrevamp$activePreviewEffectCount(), dmzrevamp$projectileCount())) {
            case ULTIMATE -> "Ultimate";
            case ADVANCED -> "Advanced";
            case BASIC -> "Basic";
        };
    }

    @Unique
    private int dmzrevamp$activePreviewEffectCount() {
        int count = 0;
        if (creatorSecondaryType != KiAttackData.SecondaryEffectType.NONE) {
            count++;
        }
        if (dmzrevamp$thirdType != KiAttackData.SecondaryEffectType.NONE) {
            count++;
        }
        if (dmzrevamp$fourthType != KiAttackData.SecondaryEffectType.NONE) {
            count++;
        }
        if (dmzrevamp$extraOne.mode != KiAttackExtraEffect.Mode.NONE && !dmzrevamp$extraOne.effectId.isBlank()) {
            count++;
        }
        if (dmzrevamp$extraTwo.mode != KiAttackExtraEffect.Mode.NONE && !dmzrevamp$extraTwo.effectId.isBlank()) {
            count++;
        }
        return count;
    }

    @Unique
    private void dmzrevamp$renderThirdEffect(GuiGraphics graphics, int center, int y) {
        dmzrevamp$renderEffect(graphics, center, y, "Third Effect", dmzrevamp$thirdType, dmzrevamp$thirdStat, dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration);
    }

    @Unique
    private void dmzrevamp$renderSecondPage(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.blit(
                DMZREVAMP_EXTRA_TECH_MENU_2,
                x,
                y,
                DMZREVAMP_PAGE_TWO_MENU_WIDTH,
                DMZREVAMP_PAGE_TWO_MENU_HEIGHT,
                0.0F,
                0.0F,
                DMZREVAMP_PAGE_TWO_MENU_WIDTH,
                DMZREVAMP_PAGE_TWO_MENU_HEIGHT,
                DMZREVAMP_PAGE_TWO_TEXTURE_SIZE,
                DMZREVAMP_PAGE_TWO_TEXTURE_SIZE
        );
        dmzrevamp$renderPageArrow(graphics, x, y, true, mouseX, mouseY);
        dmzrevamp$renderEffect(graphics, x + 84, y + 18, "Third Effect", dmzrevamp$thirdType, dmzrevamp$thirdStat, dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration);
        dmzrevamp$renderEffect(graphics, x + 260, y + 18, "Fourth Effect", dmzrevamp$fourthType, dmzrevamp$fourthStat, dmzrevamp$fourthIntensity, dmzrevamp$fourthDuration);
        dmzrevamp$renderExtraEffect(graphics, x + 84, y + 152, "Extra Effect 1", dmzrevamp$extraOne);
        dmzrevamp$renderExtraEffect(graphics, x + 260, y + 152, "Extra Effect 2", dmzrevamp$extraTwo);
    }

    @Unique
    private void dmzrevamp$renderPageArrow(GuiGraphics graphics, int x, int y, boolean left, int mouseX, int mouseY) {
        int arrowX = dmzrevamp$pageArrowX(x, left);
        int arrowY = dmzrevamp$pageArrowY(y);
        boolean hovered = dmzrevamp$isPageArrowHovered(mouseX, mouseY, left);
        int arrowU = left ? DMZREVAMP_PAGE_TWO_LEFT_ARROW_U : DMZREVAMP_PAGE_TWO_RIGHT_ARROW_U;
        int arrowV = hovered ? DMZREVAMP_PAGE_TWO_ARROW_HOVER_V : DMZREVAMP_PAGE_TWO_ARROW_V;
        int renderX = hovered ? arrowX - 1 : arrowX;
        int renderY = hovered ? arrowY - 1 : arrowY;
        int renderWidth = hovered ? DMZREVAMP_PAGE_TWO_ARROW_HOVER_WIDTH : DMZREVAMP_PAGE_TWO_ARROW_WIDTH;
        int renderHeight = hovered ? DMZREVAMP_PAGE_TWO_ARROW_HOVER_HEIGHT : DMZREVAMP_PAGE_TWO_ARROW_HEIGHT;
        graphics.blit(
                DMZREVAMP_EXTRA_TECH_MENU_2,
                renderX,
                renderY,
                renderWidth,
                renderHeight,
                hovered ? arrowU - 1 : arrowU,
                arrowV,
                renderWidth,
                renderHeight,
                DMZREVAMP_PAGE_TWO_TEXTURE_SIZE,
                DMZREVAMP_PAGE_TWO_TEXTURE_SIZE
        );
    }

    @Unique
    private boolean dmzrevamp$isPageArrowHovered(double mouseX, double mouseY, boolean left) {
        int arrowX = dmzrevamp$pageArrowX(this.panelX, left);
        int arrowY = dmzrevamp$pageArrowY(this.panelY);
        return mouseX >= arrowX
                && mouseX < arrowX + DMZREVAMP_PAGE_TWO_ARROW_WIDTH
                && mouseY >= arrowY
                && mouseY < arrowY + DMZREVAMP_PAGE_TWO_ARROW_HEIGHT;
    }

    @Unique
    private static int dmzrevamp$pageArrowX(int panelX, boolean left) {
        return left ? panelX - 30 : panelX + DMZREVAMP_PAGE_TWO_MENU_WIDTH + 8;
    }

    @Unique
    private static int dmzrevamp$pageArrowY(int panelY) {
        return panelY + 116;
    }

    @Unique
    private static void dmzrevamp$playPageArrowClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.UI_NAVE_COOLDOWN.get(), 1.0F));
    }

    @Unique
    private void dmzrevamp$renderEffect(GuiGraphics graphics, int center, int y, String title, KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, int intensity, int duration) {
        boolean hasEffect = type != KiAttackData.SecondaryEffectType.NONE;
        int active = -1;
        int inactive = -8947849;
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.txt(title), center, y, -10496);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$label("gui.dragonminez.technique.effect_type", "gui.dragonminez.technique.effect_type." + type.name().toLowerCase(Locale.ROOT)), center, dmzrevamp$effectRowY(y, 0), active);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("gui.dragonminez.technique.affected_stat", dmzrevamp$statName(stat)), center, dmzrevamp$effectRowY(y, 1), hasEffect ? active : inactive);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("gui.dragonminez.technique.intensity", intensity + "%"), center, dmzrevamp$effectRowY(y, 2), hasEffect ? active : inactive);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("gui.dragonminez.technique.duration", duration + "s"), center, dmzrevamp$effectRowY(y, 3), hasEffect ? active : inactive);
    }

    @Unique
    private void dmzrevamp$renderExtraMenus(GuiGraphics graphics, int x, int y) {
        dmzrevamp$renderExtraMenu(graphics, x - 68, y + 184);
        dmzrevamp$renderExtraMenu(graphics, x + 413, y + 184);
    }

    @Unique
    private void dmzrevamp$renderExtraMenu(GuiGraphics graphics, int centerX, int centerY) {
        graphics.blit(
                DMZREVAMP_EXTRA_TECH_MENU,
                centerX - DMZREVAMP_EXTRA_MENU_WIDTH / 2,
                centerY - DMZREVAMP_EXTRA_MENU_HEIGHT / 2,
                DMZREVAMP_EXTRA_MENU_WIDTH,
                DMZREVAMP_EXTRA_MENU_HEIGHT,
                0.0F,
                0.0F,
                DMZREVAMP_EXTRA_MENU_SRC_WIDTH,
                DMZREVAMP_EXTRA_MENU_SRC_HEIGHT,
                DMZREVAMP_EXTRA_MENU_SRC_WIDTH,
                DMZREVAMP_EXTRA_MENU_SRC_HEIGHT
        );
    }

    @Unique
    private void dmzrevamp$renderProjectiles(GuiGraphics graphics, int center, int y) {
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("Projectiles", String.valueOf(dmzrevamp$projectileCount())), center, y, dmzrevamp$projectilesEnabled() ? -1 : -8947849);
    }

    @Unique
    private void dmzrevamp$renderExtraEffect(GuiGraphics graphics, int center, int y, String title, ClientKiExtraEffectSelection effect) {
        boolean activeEffect = effect.mode != KiAttackExtraEffect.Mode.NONE;
        int active = -1;
        int inactive = -8947849;
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.txt(title), center, y, -10496);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("gui.dragonminez.technique.effect_type", dmzrevamp$extraModeName(effect)), center, dmzrevamp$effectRowY(y, 0), active);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("Effect", dmzrevamp$extraEffectName(effect)), center, dmzrevamp$effectRowY(y, 1), activeEffect ? active : inactive);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("Level", String.valueOf(effect.level)), center, dmzrevamp$effectRowY(y, 2), activeEffect ? active : inactive);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, dmzrevamp$labelLiteral("gui.dragonminez.technique.duration", effect.duration + "s"), center, dmzrevamp$effectRowY(y, 3), activeEffect ? active : inactive);
    }

    @Unique
    private void dmzrevamp$renderSkillCategory(GuiGraphics graphics, int center, int y) {
        String label = "Skill Category: ";
        String category = dmzrevamp$categoryPreview();
        int labelWidth = this.font.width(label);
        int categoryWidth = this.font.width(category);
        int left = center - ((labelWidth + categoryWidth) / 2);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.txt(label), left + labelWidth / 2, y, -1);
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.txt(category), left + labelWidth + categoryWidth / 2, y, dmzrevamp$categoryColor(category));
    }

    @Unique
    private static boolean dmzrevamp$skillProgressionCreator() {
        return DmzSkillProgressionCompat.isLoaded();
    }

    @Unique
    private int dmzrevamp$categoryColor(String category) {
        return switch (category) {
            case "Advanced" -> 0xFFD35A;
            case "Ultimate" -> 0xD58CFF;
            default -> 0x55D6FF;
        };
    }

    @Unique
    private boolean dmzrevamp$customAreaSizeEnabled() {
        if (dmzrevamp$strikeCreator) {
            return false;
        }
        return creatorType == KiAttackData.KiType.AREA;
    }

    @Unique
    private float dmzrevamp$clampCreatorSize(float size) {
        return Mth.clamp(size, 0.1F, 15.0F);
    }

    @Unique
    private static int dmzrevamp$effectRowY(int y, int row) {
        return y + DMZREVAMP_EFFECT_ROW_START + DMZREVAMP_EFFECT_ROW_STEP * row;
    }

    @Unique
    private static <T extends Enum<T>> T dmzrevamp$next(T value, T[] values) {
        return values[(value.ordinal() + 1) % values.length];
    }

    @Unique
    private static <T extends Enum<T>> T dmzrevamp$prev(T value, T[] values) {
        return values[(value.ordinal() - 1 + values.length) % values.length];
    }

    @Unique
    private Component dmzrevamp$label(String key, String valueKey) {
        return this.tr(key).append(": ").append(this.tr(valueKey));
    }

    @Unique
    private Component dmzrevamp$labelLiteral(String key, String value) {
        return this.tr(key).append(": ").append(this.txt(value));
    }

    @Unique
    private String dmzrevamp$statName(KiAttackData.AffectedStat stat) {
        if (stat == KiAttackData.AffectedStat.SKP) {
            return "SPD";
        }
        return this.tr("gui.dragonminez.technique.affected_stat." + stat.name().toLowerCase(Locale.ROOT)).getString();
    }

    @Unique
    private String dmzrevamp$extraModeName(ClientKiExtraEffectSelection effect) {
        return switch (effect.mode) {
            case HARMFUL -> "Harmful";
            case BENEFICIAL -> "Beneficial";
            case NONE -> "None";
        };
    }

    @Unique
    private String dmzrevamp$extraEffectName(ClientKiExtraEffectSelection effect) {
        if (effect.mode == KiAttackExtraEffect.Mode.NONE || effect.effectId.isBlank()) {
            return "None";
        }
        MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(effect.effectId));
        if (mobEffect == null) {
            return effect.effectId.substring(effect.effectId.lastIndexOf(':') + 1);
        }
        return Component.translatable(mobEffect.getDescriptionId()).getString();
    }

    @Unique
    private void dmzrevamp$applyExtendedDerivedValues() {
        if (dmzrevamp$strikeCreator) {
            dmzrevamp$applyStrikeCreatorDerivedValues();
            return;
        }
        float multiplier = 1.0F
                + (dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : KiAttackCategoryRules.secondaryWeight(dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration))
                + (dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : KiAttackCategoryRules.secondaryWeight(dmzrevamp$fourthIntensity, dmzrevamp$fourthDuration))
                + dmzrevamp$extraOne.costWeight()
                + dmzrevamp$extraTwo.costWeight()
                + dmzrevamp$archetypeClientCostWeight();
        if (dmzrevamp$projectilesHaveExtraCost()) {
            multiplier += (dmzrevamp$projectileCount() - 1) * 0.15F;
        }
        kiCost *= multiplier;
        tpCost *= multiplier;
        if (dmzrevamp$areaBothEnabled()) {
            kiCost *= 2.0F;
            tpCost *= 2.0F;
        }
        int extraCooldown = Math.round((multiplier - 1.0F) * 80.0F);
        creatorCooldown += extraCooldown;
        if (dmzrevamp$areaBothEnabled()) {
            creatorCooldown *= 2;
        }
    }

    @Unique
    private float dmzrevamp$archetypeClientCostWeight() {
        return 0.0F;
    }

    @Unique
    private boolean dmzrevamp$projectilesEnabled() {
        if (dmzrevamp$strikeCreator) {
            return false;
        }
        return dmzrevamp$archetype == KiAttackArchetype.NORMAL
                && (creatorType == KiAttackData.KiType.MEDIUM_BALL || creatorType == KiAttackData.KiType.SMALL_BALL);
    }

    @Unique
    private int dmzrevamp$projectileCount() {
        return dmzrevamp$projectilesEnabled() ? Mth.clamp(dmzrevamp$projectileCount, 1, dmzrevamp$maxProjectileCount()) : 1;
    }

    @Unique
    private int dmzrevamp$maxProjectileCount() {
        if (creatorType == KiAttackData.KiType.SMALL_BALL) {
            return 10;
        }
        if (creatorType == KiAttackData.KiType.MEDIUM_BALL) {
            return 5;
        }
        return 1;
    }

    @Unique
    private boolean dmzrevamp$projectilesHaveExtraCost() {
        return dmzrevamp$projectileCount() > 1
                && (creatorSecondaryType != KiAttackData.SecondaryEffectType.NONE
                || dmzrevamp$thirdType != KiAttackData.SecondaryEffectType.NONE
                || dmzrevamp$fourthType != KiAttackData.SecondaryEffectType.NONE
                || dmzrevamp$extraOne.mode != KiAttackExtraEffect.Mode.NONE
                || dmzrevamp$extraTwo.mode != KiAttackExtraEffect.Mode.NONE);
    }

    @Unique
    private boolean dmzrevamp$areaBothEnabled() {
        return dmzrevamp$areaBothUtility && dmzrevamp$archetype == KiAttackArchetype.NORMAL && creatorType == KiAttackData.KiType.AREA;
    }

    @Redirect(
            method = "renderHeader",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData$KiType;name()Ljava/lang/String;", ordinal = 0),
            require = 0,
            remap = false
    )
    private String dmzrevamp$displayCustomKiTypeName(KiAttackData.KiType instance) {
        if (dmzrevamp$strikeCreator) {
            return dmzrevamp$strikeType.translationSuffix();
        }
        return instance.name();
    }

    @Redirect(
            method = "renderHeader",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData$Utility;name()Ljava/lang/String;", ordinal = 0),
            require = 0,
            remap = false
    )
    private String dmzrevamp$displayAreaBothUtilityName(KiAttackData.Utility instance) {
        if (dmzrevamp$strikeCreator) {
            return dmzrevamp$strikeType.isEvasive() ? "HEAL" : "DAMAGE";
        }
        return dmzrevamp$areaBothEnabled() ? "BOTH" : instance.name();
    }

    @Redirect(
            method = "renderBaseEffects",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/KiAttackData;usesCustomSize(Lcom/dragonminez/common/stats/techniques/KiAttackData$KiType;)Z"),
            require = 0,
            remap = false
    )
    private boolean dmzrevamp$displayAreaSizeAsEnabled(KiAttackData.KiType type) {
        if (dmzrevamp$strikeCreator) {
            return false;
        }
        return type == KiAttackData.KiType.AREA || KiAttackData.usesCustomSize(type);
    }

    @Redirect(
            method = "renderSecondaryEffects",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/TechniqueCreatorScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"),
            require = 0,
            remap = false
    )
    private MutableComponent dmzrevamp$displaySecondarySkpAsSpd(TechniqueCreatorScreen screen, String key, Object[] args) {
        return this.tr("gui.dragonminez.technique.affected_stat.skp".equals(key) ? "gui.dragonminez.technique.affected_stat.spd" : key, args);
    }

    @Redirect(
            method = "renderSecondaryEffects",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/util/ScaledScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"),
            require = 0,
            remap = false
    )
    private MutableComponent dmzrevamp$displaySecondarySkpAsSpdFromScaledScreen(ScaledScreen screen, String key, Object[] args) {
        return this.tr("gui.dragonminez.technique.affected_stat.skp".equals(key) ? "gui.dragonminez.technique.affected_stat.spd" : key, args);
    }

    @Redirect(
            method = "renderEffectTooltip",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/TechniqueCreatorScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"),
            require = 0,
            remap = false
    )
    private MutableComponent dmzrevamp$displayStrikeDamageTooltipText(TechniqueCreatorScreen screen, String key, Object[] args) {
        if (dmzrevamp$strikeCreator) {
            if ("gui.dragonminez.technique.effect.tooltip.damage".equals(key)) {
                return this.tr("gui.dmzrevamp.technique.effect.tooltip.damage_strike", dmzrevamp$strikeDamageTooltipValue());
            }
            if ("gui.dragonminez.technique.effect.tooltip.heal".equals(key)) {
                return this.tr("gui.dmzrevamp.technique.effect.tooltip.heal_strike", dmzrevamp$strikeDamageTooltipValue());
            }
            if ("gui.dragonminez.technique.effect.tooltip.desc".equals(key)) {
                return this.tr("gui.dmzrevamp.technique.effect.tooltip.desc_strike", args);
            }
        }
        return this.tr(key, args);
    }

    @Inject(method = "getDamageHealingExpression", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzrevamp$useMeleeDamageExpressionForStrikeTooltip(CallbackInfoReturnable<String> cir) {
        if (!dmzrevamp$strikeCreator) {
            return;
        }
        double utilityMultiplier = dmzrevamp$strikeType.isEvasive() ? 0.4D : 1.0D;
        cir.setReturnValue(String.format(Locale.US, "%.2f", dmzrevamp$clientMeleeDamage() * creatorDamage * utilityMultiplier));
    }

    @Redirect(
            method = "getDamageHealingExpression",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getKiDamage()D"),
            require = 0,
            remap = false
    )
    private double dmzrevamp$useMeleeDamageForStrikeDamageExpression(StatsData data) {
        if (dmzrevamp$strikeCreator) {
            return dmzrevamp$clientMeleeDamage();
        }
        return data.getKiDamage();
    }

    @Redirect(
            method = "getDamageHealingExpression",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/config/TechniqueConfig$TechniqueTypeConfig;getDamageMultiplier()D"),
            require = 0,
            remap = false
    )
    private double dmzrevamp$skipKiTypeDamageMultiplierForStrikeTooltip(TechniqueConfig.TechniqueTypeConfig config) {
        if (dmzrevamp$strikeCreator) {
            return 1.0D;
        }
        return config.getDamageMultiplier();
    }

    @Unique
    private String dmzrevamp$strikeDamageTooltipValue() {
        double utilityMultiplier = dmzrevamp$strikeType.isEvasive() ? 0.4D : 1.0D;
        return String.format(Locale.US, "%.2f", dmzrevamp$clientMeleeDamage() * creatorDamage * utilityMultiplier);
    }

    @Invoker(value = "createArrowButton", remap = false)
    protected abstract CustomTextureButton dmzrevamp$createArrowButton(int x, int y, boolean left, net.minecraft.client.gui.components.Button.OnPress onPress);

    @Unique
    private void dmzrevamp$applyStrikeCreatorDefaults() {
        creatorType = KiAttackData.KiType.SMALL_BALL;
        creatorUtility = dmzrevamp$strikeType.isEvasive() ? KiAttackData.Utility.HEAL : KiAttackData.Utility.DAMAGE;
        creatorDamage = dmzrevamp$strikeType.defaultDamageMultiplier();
        creatorSize = 1.0F;
        creatorSpeed = dmzrevamp$strikeType.isEvasive() ? 0.0F : dmzrevamp$strikeType.defaultSpeedMultiplier();
        creatorArmorPen = dmzrevamp$strikeType.isEvasive() ? 0 : Mth.clamp(creatorArmorPen, 0, 10);
        dmzrevamp$archetype = KiAttackArchetype.NORMAL;
        dmzrevamp$areaBothUtility = false;
        dmzrevamp$filterStrikeEffects();
    }

    @Unique
    private void dmzrevamp$applyStrikeCreatorDerivedValues() {
        dmzrevamp$filterStrikeEffects();
        creatorSize = 1.0F;
        creatorArmorPen = dmzrevamp$strikeType.isEvasive() ? 0 : Mth.clamp(creatorArmorPen, 0, 10);
        if (dmzrevamp$strikeType.isEvasive()) {
            creatorUtility = KiAttackData.Utility.HEAL;
            creatorSpeed = 0.0F;
        } else {
            creatorUtility = KiAttackData.Utility.DAMAGE;
            creatorSpeed = Mth.clamp(creatorSpeed, 0.1F, 1.5F);
        }
        creatorDamage = Mth.clamp(creatorDamage, dmzrevamp$strikeType.minDamageMultiplier(), dmzrevamp$strikeType.maxDamageMultiplier());
        float extraMultiplier = 1.0F
                + (creatorSecondaryType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : KiAttackCategoryRules.secondaryWeight(creatorSecondaryIntensity, creatorSecondaryDuration))
                + (creatorArmorPen <= 0 ? 0.0F : creatorArmorPen * 0.02F)
                + (dmzrevamp$thirdType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : KiAttackCategoryRules.secondaryWeight(dmzrevamp$thirdIntensity, dmzrevamp$thirdDuration))
                + (dmzrevamp$fourthType == KiAttackData.SecondaryEffectType.NONE ? 0.0F : KiAttackCategoryRules.secondaryWeight(dmzrevamp$fourthIntensity, dmzrevamp$fourthDuration))
                + dmzrevamp$extraOne.costWeight()
                + dmzrevamp$extraTwo.costWeight();
        float minDamage = Math.max(0.1F, dmzrevamp$strikeType.minDamageMultiplier());
        float defaultDamage = Math.max(0.1F, dmzrevamp$strikeType.defaultDamageMultiplier());
        float damageRatio = creatorDamage / defaultDamage;
        float speedRatio = dmzrevamp$strikeType.isEvasive() ? 1.0F : 1.0F + Math.max(0.0F, creatorSpeed - 1.0F) * 0.35F;
        kiCost = Math.max(5.0F, (float) (dmzrevamp$clientMeleeNoForms() * creatorDamage * 0.35D / 2.0D)) * extraMultiplier * speedRatio;
        tpCost = Math.max(100.0F, 100.0F * (creatorDamage / minDamage) * speedRatio * extraMultiplier);
        int baseCooldown = dmzrevamp$strikeType.isEvasive() ? DMZREVAMP_EVASIVE_BASE_COOLDOWN_TICKS : DMZREVAMP_STRIKE_BASE_COOLDOWN_TICKS;
        // Armor Pen and extra effects raise the derived costs through extraMultiplier.
        creatorCooldown = Math.max(1, Math.round((baseCooldown * damageRatio * speedRatio + Math.round((extraMultiplier - 1.0F) * 80.0F)) * dmzrevamp$strikeType.cooldownMultiplier()));
    }

    @Redirect(
            method = "renderHeader",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;", ordinal = 0),
            require = 0,
            remap = false
    )
    private String dmzrevamp$displayStrikeCreatorCooldownSeconds(int ticks) {
        if (!dmzrevamp$strikeCreator) {
            return String.valueOf(ticks);
        }
        return String.valueOf(Math.max(1, Math.round(ticks / 20.0F)));
    }

    @Unique
    private void dmzrevamp$filterStrikeEffects() {
        KiAttackData.SecondaryEffectType allowed = dmzrevamp$strikeType.isEvasive() ? KiAttackData.SecondaryEffectType.BUFF : KiAttackData.SecondaryEffectType.DEBUFF;
        KiAttackExtraEffect.Mode extraAllowed = dmzrevamp$strikeType.isEvasive() ? KiAttackExtraEffect.Mode.BENEFICIAL : KiAttackExtraEffect.Mode.HARMFUL;
        if (creatorSecondaryType != allowed) {
            creatorSecondaryType = KiAttackData.SecondaryEffectType.NONE;
        }
        if (dmzrevamp$strikeType.isEvasive()) {
            creatorArmorPen = 0;
        }
        if (dmzrevamp$thirdType != allowed) {
            dmzrevamp$thirdType = KiAttackData.SecondaryEffectType.NONE;
        }
        if (dmzrevamp$fourthType != allowed) {
            dmzrevamp$fourthType = KiAttackData.SecondaryEffectType.NONE;
        }
        if (dmzrevamp$extraOne.mode != extraAllowed) {
            dmzrevamp$extraOne.mode = KiAttackExtraEffect.Mode.NONE;
            dmzrevamp$extraOne.onModeChanged();
        }
        if (dmzrevamp$extraTwo.mode != extraAllowed) {
            dmzrevamp$extraTwo.mode = KiAttackExtraEffect.Mode.NONE;
            dmzrevamp$extraTwo.onModeChanged();
        }
    }

    @Unique
    private double dmzrevamp$clientMeleeNoForms() {
        double[] value = {0.0D};
        if (Minecraft.getInstance().player != null) {
            StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
                double base = data.getMeleeDamageNoMultipliers();
                value[0] = Math.max(0.0D, base);
            });
        }
        return value[0];
    }

    @Unique
    private double dmzrevamp$clientMeleeDamage() {
        double[] value = {0.0D};
        if (Minecraft.getInstance().player != null) {
            StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> value[0] = Math.max(0.0D, data.getMeleeDamage()));
        }
        return value[0];
    }
}
