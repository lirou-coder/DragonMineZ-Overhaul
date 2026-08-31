package com.dmzrevamp.client;

import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.PrestigeC2SPacket;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PrestigeConfirmationScreen extends ScaledScreen {
    private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(
            "dragonminez", "textures/gui/buttons/characterbuttons.png");
    private final Screen parent;

    public PrestigeConfirmationScreen(Screen parent) {
        super(Component.translatable("gui.dmzrevamp.prestige.confirmation.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int uiWidth = getUiWidth();
        int y = getUiHeight() / 2 + 42;
        addRenderableWidget(button(uiWidth / 2 - 79, y,
                Component.translatable("gui.dmzrevamp.prestige.confirmation.cancel"),
                ignored -> minecraft.setScreen(parent)));
        addRenderableWidget(button(uiWidth / 2 + 5, y,
                Component.translatable("gui.dmzrevamp.prestige.confirmation.confirm"), ignored -> {
            DmzRevampNetwork.CHANNEL.sendToServer(new PrestigeC2SPacket());
            minecraft.setScreen(parent);
        }));
    }

    private TexturedTextButton button(int x, int y, Component text, net.minecraft.client.gui.components.Button.OnPress action) {
        return new TexturedTextButton.Builder()
                .position(x, y).size(74, 20).texture(BUTTONS)
                .textureCoords(0, 28, 0, 48).textureSize(74, 20)
                .message(text).onPress(action).build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0000000);
        int uiMouseX = (int) Math.round(toUiX(mouseX));
        int uiMouseY = (int) Math.round(toUiY(mouseY));
        int uiWidth = getUiWidth();
        int y = getUiHeight() / 2 - 62;
        Component warning = Component.translatable("gui.dmzrevamp.prestige.confirmation.warning")
                .withStyle(style -> style.withFont(DMZ_FONT));
        beginUiScale(graphics);
        for (var line : font.split(warning, Math.min(360, uiWidth - 40))) {
            graphics.drawCenteredString(font, line, uiWidth / 2, y, 0xFFFFFF);
            y += 11;
        }
        super.render(graphics, uiMouseX, uiMouseY, partialTick);
        endUiScale(graphics);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
