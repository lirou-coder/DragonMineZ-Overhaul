package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.quest.TournamentFighterStatsExtension;
import com.dmzrevamp.util.CompactNumberFormatter;
import com.dragonminez.client.gui.tournament.TournamentBracketScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.network.TournamentPackets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(value = TournamentBracketScreen.class, remap = false)
public abstract class TournamentBracketStatsTooltipMixin {
    private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");
    private static final Field HOVERED_SLOT_FIELD = dmzrevamp$findField("hoveredSlot");
    private static final Method SLOT_ID_METHOD = dmzrevamp$findSlotIdMethod();

    @Redirect(method = "renderTooltips",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/util/TextUtil;renderAdvancedTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;Ljava/util/List;Ljava/util/List;I)V"))
    private void dmzrevamp$addDefenseAndSpeedToFighterTooltip(GuiGraphics graphics, Font font,
                                                               int mouseX, int mouseY, int screenWidth, int screenHeight,
                                                               Component title, List<Component> description,
                                                               List<Component> extras, int color) {
        List<Component> rows = description;
        String slotId = dmzrevamp$hoveredSlotId();
        TournamentPackets.OpenBracketS2C message = ((TournamentBracketScreenAccessor) this).dmzrevamp$getTournamentData();
        TournamentPackets.OpenBracketS2C.FighterStats stats = message == null || slotId == null
                ? null : message.getStats().get(slotId);
        if (stats != null) {
            TournamentFighterStatsExtension configured = (TournamentFighterStatsExtension) (Object) stats;
            Double defense = configured.dmzrevamp$getDefense();
            Double movementSpeed = configured.dmzrevamp$getMovementSpeed();
            if ((defense != null && defense > 0D) || (movementSpeed != null && movementSpeed > 0D)) {
                rows = new ArrayList<>(description);
                if (defense != null && defense > 0D) {
                    rows.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.defense", dmzrevamp$format(defense)));
                }
                if (movementSpeed != null && movementSpeed > 0D) {
                    rows.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.movement_speed",
                            dmzrevamp$format((movementSpeed / 0.2D) * 100D) + "%"));
                }
            }
        }
        TextUtil.renderAdvancedTooltip(graphics, font, mouseX, mouseY, screenWidth, screenHeight,
                title, rows, extras, color);
    }

    private String dmzrevamp$hoveredSlotId() {
        if (HOVERED_SLOT_FIELD == null || SLOT_ID_METHOD == null) return null;
        try {
            Object slot = HOVERED_SLOT_FIELD.get(this);
            Object id = slot == null ? null : SLOT_ID_METHOD.invoke(slot);
            return id instanceof String string ? string : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Component dmzrevamp$stat(String key, String value) {
        return Component.translatable(key)
                .setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(0xFFFFFF))
                .append(Component.literal(": ").setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(0xFFFFFF)))
                .append(Component.literal(value).setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(0x55FFFF)));
    }

    private static String dmzrevamp$format(double value) {
        if (!Double.isFinite(value)) return "0";
        if (value == Math.floor(value)) {
            long whole = value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
            return CompactNumberFormatter.format(whole, 10_000D);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Field dmzrevamp$findField(String name) {
        try {
            Field field = TournamentBracketScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method dmzrevamp$findSlotIdMethod() {
        try {
            Class<?> slotType = Class.forName("com.dragonminez.client.gui.tournament.TournamentBracketScreen$SlotHit");
            Method method = slotType.getDeclaredMethod("id");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
