package com.dmzrevamp.compat.sdu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SduClassEditorClient {
    private SduClassEditorClient() {
    }

    public static void open(String classId, String json, boolean newClass) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        minecraft.setScreen(new OverhaulSduClassEditScreen(parent, classId, json, newClass));
    }
}
