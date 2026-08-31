package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientStrikeCreatorMode;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.racial.CustomRacialSkillRegistry;
import com.dmzrevamp.revamp.classes.skills.ClassSkillHelper;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dmzrevamp.revamp.ki.KiAttackCategoryRules;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dmzrevamp.revamp.strike.StrikeAttackCategoryRules;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.SkillsMenuScreen;
import com.dragonminez.client.gui.character.TechniqueCreatorScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = SkillsMenuScreen.class, priority = 1100)
public abstract class SkillsMenuScreenMixin {
    private static final String DMZ_REVAMP_CLASS_PASSIVE_ENTRY = "__class_passive__";
    private static final int DMZ_REVAMP_DESC_WIDTH = 120;
    private static final int DMZ_REVAMP_DESC_HEIGHT = 72;
    private static final int DMZ_REVAMP_LINE_HEIGHT = 12;
    private static final ResourceLocation DMZ_REVAMP_DMZ_BUTTONS = ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/buttons/characterbuttons.png");
    private static final ResourceLocation DMZ_REVAMP_SMOOTH_FONT = ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");
    private static final int DMZ_REVAMP_DETAIL_LEFT_OFFSET = 15;
    private static final int DMZ_REVAMP_DETAIL_CENTER_OFFSET = 70;
    private static final int DMZ_REVAMP_DETAIL_TOP_OFFSET = 40;
    private static final int DMZ_REVAMP_DETAIL_COLOR_TITLE = 0xFFFFFF;
    private static final int DMZ_REVAMP_DETAIL_COLOR_XP = -11141291;
    private static final int DMZ_REVAMP_DETAIL_COLOR_TYPE = 14540253;
    private static final int DMZ_REVAMP_DETAIL_COLOR_VALUE = 16777215;
    private static final int DMZ_REVAMP_DETAIL_COLOR_COST = 16755370;
    private static final int DMZ_REVAMP_DETAIL_COLOR_REQ_XP = -5592406;

    @Shadow
    private StatsData statsData;

    @Shadow
    private String selectedSkill;

    @Shadow
    private float targetDescScroll;

    @Shadow
    private float currentDescScroll;

    @Shadow
    private float maxDescScroll;

    @Shadow
    private CustomTextureButton btnSpeed;

    @Shadow
    private CustomTextureButton btnPen;

    @Shadow
    private boolean isBinding;

    @Shadow
    private boolean isImportingTechnique;

    private String dmzrevamp$lastDetailsSkill;

    @Redirect(
            method = {"renderSkillsList", "renderSkillDetails"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/SkillsMenuScreen;getClassPassiveTitle()Ljava/lang/String;"),
            remap = false,
            require = 0
    )
    private String dmzrevamp$configuredClassPassiveListTitle(SkillsMenuScreen instance) {
        if (statsData == null) return "Class Passive";
        return ChatFormatting.stripFormatting(DmzClassConfigManager.getDisplayName(statsData.getCharacter().getCharacterClass())
                + " " + Component.translatable("class.dragonminez.passive").getString());
    }

    @Redirect(
            method = "renderSkillDetails",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/SkillsMenuScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"),
            remap = false,
            require = 0
    )
    private MutableComponent dmzrevamp$configuredClassPassiveDescription(SkillsMenuScreen instance, String key, Object[] args) {
        if (statsData != null && key != null && key.startsWith("class.dragonminez.")
                && key.endsWith(".passive.desc")) {
            String skillId = ClassSkillHelper.getSkillForCurrentClass(statsData);
            String description = skillId == null ? "" : ClassSkillHelper.getDescription(statsData, skillId);
            if (description != null && !description.isEmpty()) {
                return Component.literal(description);
            }
        }
        return instance.tr(key, args);
    }

    @Inject(method = "m_7856_", at = @At("HEAD"), remap = false)
    private void dmzrevamp$restoreStrikeCategoryAfterCreator(CallbackInfo ci) {
        if (!ClientStrikeCreatorMode.consumeNextSkillsMenuIsStrike()) {
            return;
        }
        dmzrevamp$setCurrentCategoryReflective("STRIKE");
    }

    @Redirect(
            method = "renderSkillsList",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/skills/Skill;getLevel()I"),
            require = 0,
            remap = false
    )
    // Shows the real class passive level instead of the placeholder skill level.
    private int dmzrevamp$renderEffectiveSkillLevel(Skill skill) {
        if (statsData == null || skill == null) {
            return skill != null ? skill.getLevel() : 0;
        }
        String skillId = skill.getName();
        if (ClassSkillHelper.isClassSkill(skillId)) {
            return ClassSkillHelper.level(statsData, skillId);
        }
        return skill.getLevel();
    }

    @Inject(method = "getVisibleSkillNames", at = @At("RETURN"), cancellable = true)
    // Keeps class passives out of the normal skill list.
    private void dmzrevamp$hideClassPassivesFromSkillList(CallbackInfoReturnable<List<String>> cir) {
        if (statsData == null || !dmzrevamp$isSkillsCategory()) {
            return;
        }
        List<String> visible = new ArrayList<>(cir.getReturnValue());
        visible.removeIf(ClassSkillHelper::isClassSkill);
        cir.setReturnValue(visible);
    }

    @Inject(method = "getVisibleSkillNames", at = @At("RETURN"), cancellable = true)
    private void dmzrevamp$addNewSkillToStrikeList(CallbackInfoReturnable<List<String>> cir) {
        if (statsData == null || !dmzrevamp$isStrikeCategory()) {
            return;
        }
        List<String> visible = new ArrayList<>(cir.getReturnValue());
        if (!visible.contains("__new_skill__")) {
            visible.add(0, "__new_skill__");
        }
        for (var entry : statsData.getTechniques().getUnlockedTechniques().entrySet()) {
            if (entry.getValue() instanceof StrikeAttackData && !visible.contains(entry.getKey())) {
                visible.add(entry.getKey());
            }
        }
        cir.setReturnValue(visible);
    }

    @Inject(method = "initCreateSkillButton", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$allowStrikeCreateButton(CallbackInfo ci) {
        if (statsData == null || selectedSkill == null || !"__new_skill__".equals(selectedSkill) || !dmzrevamp$isStrikeCategory()) {
            return;
        }
        int x = dmzrevamp$getUiWidthReflective() - 158;
        int y = dmzrevamp$getUiHeightReflective() / 2 - 105;
        TexturedTextButton button = new TexturedTextButton.Builder()
                .position(x + 35, y + 185)
                .size(74, 20)
                .texture(DMZ_REVAMP_DMZ_BUTTONS)
                .textureCoords(0, 28, 0, 48)
                .textureSize(74, 20)
                .message(dmzrevamp$smooth(Component.translatable("gui.dragonminez.skills.create_skill")))
                .onPress(btn -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft != null) {
                        ClientStrikeCreatorMode.markNextCreatorAsStrike();
                        minecraft.setScreen(new TechniqueCreatorScreen((SkillsMenuScreen) (Object) this));
                    }
                })
                .build();
        dmzrevamp$addWidgetReflective(button);
        ci.cancel();
    }

    @Inject(method = "m_6375_", at = @At("RETURN"), remap = false)
    private void dmzrevamp$openStrikeCreatorFromNewSkill(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Strike creation now follows Ki creation: selecting New Skill only reveals the Create Skill button.
    }

    @Inject(method = "renderRightPanel", at = @At("TAIL"), remap = false)
    private void dmzrevamp$renderStrikeNewSkillTitle(
            GuiGraphics graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        if (!dmzrevamp$isStrikeCategory() || !"__new_skill__".equals(selectedSkill)) {
            return;
        }
        // DMZ only invokes renderNewSkillPlaceholder for the KI category. Draw
        // the exact equivalent for the custom STRIKE entry using its smooth font.
        dmzrevamp$drawCenteredSmooth(
                graphics,
                dmzrevamp$smooth(Component.translatable("gui.dragonminez.skills.new_skill").withStyle(ChatFormatting.BOLD)),
                panelX + 70,
                panelY + 48,
                0xFFFFFF
        );
    }

    // Reads the private DMZ category so this addon can add entries only to the matching tab.
    private boolean dmzrevamp$isSkillsCategory() {
        try {
            Field field = SkillsMenuScreen.class.getDeclaredField("currentCategory");
            field.setAccessible(true);
            Object category = field.get(this);
            return category != null && "SKILLS".equals(category.toString());
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private boolean dmzrevamp$isStrikeCategory() {
        try {
            Field field = SkillsMenuScreen.class.getDeclaredField("currentCategory");
            field.setAccessible(true);
            Object category = field.get(this);
            return category != null && "STRIKE".equals(category.toString());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void dmzrevamp$setCurrentCategoryReflective(String name) {
        try {
            Field field = SkillsMenuScreen.class.getDeclaredField("currentCategory");
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants()) {
                    if (constant != null && name.equals(constant.toString())) {
                        field.set(this, constant);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Inject(method = "renderTechniqueDetails", at = @At("TAIL"), remap = false)
    private void dmzrevamp$renderKiTechniqueCategory(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY, CallbackInfo ci) {
        if (statsData == null || selectedSkill == null) {
            return;
        }
        TechniqueData technique = statsData.getTechniques().getUnlockedTechniques().get(selectedSkill);
        KiAttackCategory category;
        if (technique instanceof KiAttackData kiAttackData) {
            category = KiAttackCategoryRules.classify(kiAttackData);
        } else if (technique instanceof StrikeAttackData strikeAttackData) {
            category = StrikeAttackCategoryRules.classify(strikeAttackData);
        } else {
            return;
        }
        if (dmzrevamp$isCreatorCustomStrike(technique)) {
            return;
        }
        TextUtil.drawStringWithBorder(
                graphics,
                Minecraft.getInstance().font,
                Component.literal(dmzrevamp$categoryLabel(category)),
                panelX + 15,
                panelY + 172,
                dmzrevamp$categoryColor(category)
        );
    }

    @Inject(method = "renderTechniqueDetails", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$renderCustomStrikeDetails(GuiGraphics graphics, int panelX, int panelY, int mouseX, int mouseY, CallbackInfo ci) {
        TechniqueData technique = dmzrevamp$selectedTechnique();
        if (!dmzrevamp$isCreatorCustomStrike(technique) || !(technique instanceof StrikeAttackData strike) || !(strike instanceof RevampStrikeAttackData revamp)) {
            return;
        }

        int left = panelX + DMZ_REVAMP_DETAIL_LEFT_OFFSET;
        int center = panelX + DMZ_REVAMP_DETAIL_CENTER_OFFSET;
        int y = panelY + DMZ_REVAMP_DETAIL_TOP_OFFSET;
        int upgradeXpCost = strike.getUpgradeXpCost("damage");
        int cooldownTicks = strike.getActualCooldown();
        int damage = (int) (statsData.getMeleeDamage() * strike.getActualDamageMultiplier());

        dmzrevamp$drawCenteredSmooth(graphics, dmzrevamp$smooth(Component.translatable(strike.getName()).withStyle(net.minecraft.ChatFormatting.BOLD)), center, y, DMZ_REVAMP_DETAIL_COLOR_TITLE);
        y += 12;
        dmzrevamp$drawCenteredSmooth(graphics, dmzrevamp$smooth(Component.translatable("gui.dragonminez.technique.xp", strike.getExperience())), center, y, DMZ_REVAMP_DETAIL_COLOR_XP);
        y += 16;

        dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.type", Component.translatable("technique.type.strike")), left, y, DMZ_REVAMP_DETAIL_COLOR_TYPE);
        y += 12;
        dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.damage", Component.literal(String.valueOf(damage))), left, y, DMZ_REVAMP_DETAIL_COLOR_VALUE);
        y += 12;
        if (!revamp.dmzrevamp$getStrikeType().isEvasive()) {
            dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.speed", Component.literal(String.format(java.util.Locale.US, "%.1f", revamp.dmzrevamp$getDashSpeedMultiplier()))), left, y, DMZ_REVAMP_DETAIL_COLOR_VALUE);
            y += 12;
            dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.armor_pen", Component.literal(String.valueOf(revamp.dmzrevamp$getArmorPenetration()))), left, y, DMZ_REVAMP_DETAIL_COLOR_VALUE);
            y += 12;
        }
        dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.cooldown", Component.literal(String.format(java.util.Locale.US, "%.1fs", cooldownTicks / 20.0F))), left, y, DMZ_REVAMP_DETAIL_COLOR_VALUE);
        y += 16;
        dmzrevamp$drawSmooth(graphics, dmzrevamp$detailLine("gui.dragonminez.technique.energy_cost", Component.literal(String.format(java.util.Locale.US, "%.1f", strike.getCalculatedCost(statsData)))), left, y, DMZ_REVAMP_DETAIL_COLOR_COST);
        y += 16;
        dmzrevamp$drawSmooth(graphics, dmzrevamp$smooth(Component.translatable("gui.dragonminez.technique.req_xp", upgradeXpCost)), left, y, DMZ_REVAMP_DETAIL_COLOR_REQ_XP);

        KiAttackCategory category = StrikeAttackCategoryRules.classify(strike);
        dmzrevamp$drawSmooth(graphics, dmzrevamp$smooth(Component.literal(dmzrevamp$categoryLabel(category))), left, panelY + 172, dmzrevamp$categoryColor(category));
        dmzrevamp$invokeRenderActionStatus(graphics, panelX, panelY);
        ci.cancel();
    }

    @Redirect(
            method = "renderTechniqueDetails",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/StatsData;getStrikeDamage()D"),
            require = 0,
            remap = false
    )
    private double dmzrevamp$displayCustomStrikeDamageFromMelee(StatsData data) {
        TechniqueData technique = dmzrevamp$selectedTechnique();
        if (technique instanceof StrikeAttackData strike && strike instanceof RevampStrikeAttackData revamp && revamp.dmzrevamp$isCustomStrike()) {
            return data.getMeleeDamage();
        }
        return data.getStrikeDamage();
    }

    @Redirect(
            method = "renderTechniqueDetails",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/techniques/StrikeAttackData;getDamageMultiplier()F"),
            require = 0,
            remap = false
    )
    private float dmzrevamp$displayCustomStrikeActualDamageMultiplier(StrikeAttackData strike) {
        if (strike instanceof RevampStrikeAttackData revamp && revamp.dmzrevamp$isCustomStrike()) {
            return strike.getActualDamageMultiplier();
        }
        return strike.getDamageMultiplier();
    }

    @Inject(method = "initTechniqueUpgradeButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$addCustomStrikeUpgradeButtons(CallbackInfo ci) {
        TechniqueData technique = dmzrevamp$selectedTechnique();
        if (!dmzrevamp$isCreatorCustomStrike(technique) || !(technique instanceof StrikeAttackData strike)) {
            return;
        }
        int x = dmzrevamp$getUiWidthReflective() - 158;
        int y = dmzrevamp$getUiHeightReflective() / 2 - 105;
        int buttonX = x + 115;
        int buttonY = y + 80;
        if (strike.canUpgradeStat("damage") && strike.getExperience() >= strike.getUpgradeXpCost("damage")) {
            dmzrevamp$addWidgetReflective(dmzrevamp$createUpgradeBtn(buttonX, buttonY, "damage", true));
        }
        buttonY += 12;
        if (strike.canUpgradeStat("speed") && strike.getExperience() >= strike.getUpgradeXpCost("speed")) {
            btnSpeed = dmzrevamp$createUpgradeBtn(buttonX, buttonY, "speed", true);
            dmzrevamp$addWidgetReflective(btnSpeed);
        }
        buttonY += 12;
        if (strike.canUpgradeStat("armor_pen") && strike.getExperience() >= strike.getUpgradeXpCost("armor_pen")) {
            btnPen = dmzrevamp$createUpgradeBtn(buttonX, buttonY, "armor_pen", true);
            dmzrevamp$addWidgetReflective(btnPen);
        }
        buttonY += 12;
        if (strike.canUpgradeStat("cooldown") && strike.getExperience() >= strike.getUpgradeXpCost("cooldown")) {
            dmzrevamp$addWidgetReflective(dmzrevamp$createUpgradeBtn(buttonX, buttonY, "cooldown", true));
        }
        ci.cancel();
    }

    @Inject(method = "initBindButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$hideDeleteButtonForRaceExclusiveStrikes(CallbackInfo ci) {
        TechniqueData technique = dmzrevamp$selectedTechnique();
        if (!dmzrevamp$isRaceExclusiveStrike(technique) || isBinding) {
            return;
        }

        int x = dmzrevamp$getUiWidthReflective() - 158;
        int y = dmzrevamp$getUiHeightReflective() / 2 - 105;
        int buttonY = y + 185;

        // Race-exclusive strikes are permanent rewards, so only the bind button is shown here.
        TexturedTextButton bindButton = new TexturedTextButton.Builder()
                .position(x + 35, buttonY)
                .size(74, 20)
                .texture(DMZ_REVAMP_DMZ_BUTTONS)
                .textureCoords(0, 28, 0, 48)
                .textureSize(74, 20)
                .message(dmzrevamp$smooth(Component.translatable("gui.dragonminez.skills.bind_to_slot")))
                .onPress(this::dmzrevamp$startBindingRaceExclusiveStrike)
                .build();
        dmzrevamp$addWidgetReflective(bindButton);
        isImportingTechnique = false;
        ci.cancel();
    }

    private TechniqueData dmzrevamp$selectedTechnique() {
        if (statsData == null || selectedSkill == null) {
            return null;
        }
        return statsData.getTechniques().getUnlockedTechniques().get(selectedSkill);
    }

    private int dmzrevamp$getUiWidthReflective() {
        return dmzrevamp$invokeInt("getUiWidth", Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    private int dmzrevamp$getUiHeightReflective() {
        return dmzrevamp$invokeInt("getUiHeight", Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    private int dmzrevamp$invokeInt(String method, int fallback) {
        Class<?> type = ((Object) this).getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method reflected = type.getDeclaredMethod(method);
                reflected.setAccessible(true);
                Object value = reflected.invoke(this);
                return value instanceof Number number ? number.intValue() : fallback;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return fallback;
    }

    private void dmzrevamp$addWidgetReflective(GuiEventListener widget) {
        Class<?> type = ((Object) this).getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method method = type.getDeclaredMethod("m_142416_", GuiEventListener.class);
                method.setAccessible(true);
                method.invoke(this, widget);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private void dmzrevamp$invokeRenderActionStatus(GuiGraphics graphics, int panelX, int panelY) {
        Class<?> type = ((Object) this).getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method method = type.getDeclaredMethod("renderActionStatus", GuiGraphics.class, int.class, int.class);
                method.setAccessible(true);
                method.invoke(this, graphics, panelX, panelY);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private void dmzrevamp$startBindingRaceExclusiveStrike(Button button) {
        isBinding = true;
        dmzrevamp$invokeVoid("m_169413_");
        dmzrevamp$invokeVoid("initDynamicButtons");
        dmzrevamp$invokeVoid("initNavigationButtons");
        dmzrevamp$invokeVoid("initUpgradeButton");
        dmzrevamp$invokeVoid("initBindButtons");
    }

    private void dmzrevamp$invokeVoid(String methodName) {
        Class<?> type = ((Object) this).getClass();
        while (type != null) {
            try {
                java.lang.reflect.Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                method.invoke(this);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    @Invoker(value = "createUpgradeBtn", remap = false)
    protected abstract CustomTextureButton dmzrevamp$createUpgradeBtn(int x, int y, String statType, boolean plus);

    @Inject(method = "renderSkillDetails", at = @At("HEAD"), cancellable = true)
    // Replaces DMZ's generic details when the selected entry is an addon passive or racial skill.
    private void dmzrevamp$renderCustomRacialDetails(GuiGraphics graphics, int panelX, int panelY, CallbackInfo ci) {
        if (selectedSkill == null) {
            return;
        }

        if (ClassSkillHelper.isClassSkill(selectedSkill) && !DMZ_REVAMP_CLASS_PASSIVE_ENTRY.equals(selectedSkill)
                && statsData != null) {
            String classSkill = selectedSkill;
            String className = DmzClassConfigManager.getDisplayName(statsData.getCharacter().getCharacterClass());
            String description = classSkill == null ? "" : ClassSkillHelper.getDescription(statsData, classSkill);
            int startY = panelY + 40;
            dmzrevamp$drawCenteredStringWithBorder(
                    graphics,
                    dmzrevamp$smooth(Component.literal(className + " "
                            + ChatFormatting.stripFormatting(Component.translatable("class.dragonminez.passive").getString()))),
                    panelX + 72,
                    startY,
                    0xFFFFFFFF
            );
            dmzrevamp$drawCenteredStringWithBorder(
                    graphics,
                    dmzrevamp$smooth(Component.translatable("class.dragonminez.passive")),
                    panelX + 72,
                    startY + 12,
                    0xFFFFD200
            );

            List<String> wrappedDesc = dmzrevamp$wrapText(description, DMZ_REVAMP_DESC_WIDTH);
            dmzrevamp$renderScrollableDescription(graphics, classSkill == null ? selectedSkill : classSkill, wrappedDesc, panelX + 13, startY + 70);
            ci.cancel();
            return;
        }

        if (!selectedSkill.startsWith("racial_")) {
            return;
        }

        String racialSkillId = selectedSkill.substring("racial_".length());
        CustomRacialSkill customRacialSkill = CustomRacialSkillRegistry.get(racialSkillId);
        if (customRacialSkill == null || statsData == null) {
            return;
        }

        String displayName = customRacialSkill.getSkillTitle().getString();
        String description = customRacialSkill.getSkillDescription(statsData).getString();
        int startY = panelY + 40;

        dmzrevamp$drawCenteredStringWithBorder(
                graphics,
                dmzrevamp$smooth(Component.literal(displayName)),
                panelX + 72,
                startY,
                0xFFFFFFFF
        );
        dmzrevamp$drawCenteredStringWithBorder(
                graphics,
                dmzrevamp$smooth(Component.translatable("gui.dragonminez.skills.racial")),
                panelX + 72,
                startY + 12,
                0xFF55FF55
        );

        List<String> wrappedDesc = dmzrevamp$wrapText(description, DMZ_REVAMP_DESC_WIDTH);
        int descY = startY + 70;

        dmzrevamp$renderScrollableDescription(graphics, selectedSkill, wrappedDesc, panelX + 13, descY);

        ci.cancel();
    }

    // Draws long descriptions inside the same clipped area used by the DMZ menu.
    private void dmzrevamp$renderScrollableDescription(GuiGraphics graphics, String skillId, List<String> wrappedDesc, int x, int y) {
        if (dmzrevamp$lastDetailsSkill == null || !dmzrevamp$lastDetailsSkill.equals(skillId)) {
            dmzrevamp$lastDetailsSkill = skillId;
            targetDescScroll = 0.0F;
            currentDescScroll = 0.0F;
        }

        maxDescScroll = Math.max(0, wrappedDesc.size() * DMZ_REVAMP_LINE_HEIGHT - DMZ_REVAMP_DESC_HEIGHT);
        targetDescScroll = Mth.clamp(targetDescScroll, 0.0F, maxDescScroll);
        currentDescScroll = Mth.lerp(Minecraft.getInstance().getFrameTime() * 0.4F, currentDescScroll, targetDescScroll);
        TextUtil.renderScrollableText(
                graphics,
                Minecraft.getInstance().font,
                wrappedDesc,
                x,
                y,
                DMZ_REVAMP_DESC_WIDTH,
                DMZ_REVAMP_DESC_HEIGHT,
                currentDescScroll,
                maxDescScroll,
                0xFFCCCCCC,
                Style.EMPTY
        );
    }

    // Draws centered text with a small dark outline so it stays readable on the menu art.
    private void dmzrevamp$drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
        Font font = Minecraft.getInstance().font;
        dmzrevamp$drawStringWithBorder(graphics, text, centerX - font.width(text) / 2, y, textColor);
    }

    // Draws normal text with the same outline style used across DMZ screens.
    private void dmzrevamp$drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, x + 1, y, 0xFF000000, false);
        graphics.drawString(font, text, x - 1, y, 0xFF000000, false);
        graphics.drawString(font, text, x, y + 1, 0xFF000000, false);
        graphics.drawString(font, text, x, y - 1, 0xFF000000, false);
        graphics.drawString(font, text, x, y, textColor, false);
    }

    private void dmzrevamp$drawSmooth(GuiGraphics graphics, Component text, int x, int y, int textColor) {
        TextUtil.drawStringWithBorder(graphics, Minecraft.getInstance().font, text, x, y, textColor);
    }

    private void dmzrevamp$drawCenteredSmooth(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
        TextUtil.drawCenteredStringWithBorder(graphics, Minecraft.getInstance().font, text, centerX, y, textColor);
    }

    private MutableComponent dmzrevamp$detailLine(String labelKey, Component value) {
        return dmzrevamp$smooth(Component.translatable(labelKey).append(": ").append(value));
    }

    private MutableComponent dmzrevamp$smooth(Component component) {
        return component.copy().withStyle(style -> style.withFont(DMZ_REVAMP_SMOOTH_FONT));
    }

    private String dmzrevamp$categoryLabel(KiAttackCategory category) {
        return switch (category) {
            case ADVANCED -> "Advanced";
            case ULTIMATE -> "Ultimate";
            case BASIC -> "Basic";
        };
    }

    private int dmzrevamp$categoryColor(KiAttackCategory category) {
        return switch (category) {
            case ADVANCED -> 0xFFFFD35A;
            case ULTIMATE -> 0xFFD58CFF;
            case BASIC -> 0xFF55D6FF;
        };
    }

    private boolean dmzrevamp$isCreatorCustomStrike(TechniqueData technique) {
        return technique instanceof StrikeAttackData
                && technique instanceof RevampStrikeAttackData revamp
                && revamp.dmzrevamp$isCustomStrike()
                && !dmzrevamp$isRaceExclusiveStrike(technique);
    }

    private boolean dmzrevamp$isRaceExclusiveStrike(TechniqueData technique) {
        if (!(technique instanceof StrikeAttackData)) {
            return false;
        }
        String id = technique.getId();
        return StrikeAttackTemplates.ANDROID_ABSORPTION.equals(id)
                || StrikeAttackTemplates.SLEEP_RECOVERY.equals(id)
                || StrikeAttackTemplates.NAMEKIAN_REGENERATION.equals(id);
    }

    // Splits plain text into lines that fit inside the details panel.
    private List<String> dmzrevamp$wrapText(String text, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
