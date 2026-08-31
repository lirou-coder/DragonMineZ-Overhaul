package com.dmzrevamp.client.radial;

import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.SetSpeedLimitC2SPacket;
import com.dmzrevamp.revamp.speed.SpeedLimitData;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.nodes.ReleaseNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SpeedLimitNode extends ReleaseNode {
    @Override public Component label(StatsData data) { return Component.translatable("gui.action.dmzrevamp.speed_limit"); }
    @Override public ResourceLocation icon(StatsData data) { return null; }
    @Override public String faceText(StatsData data) {
        int limit = ((SpeedLimitData) data).dmzrevamp$getSpeedLimit();
        return (limit <= 0 ? 3000 : limit) + "%";
    }
    @Override public boolean active(StatsData data) { return ((SpeedLimitData) data).dmzrevamp$getSpeedLimit() > 0; }
    @Override public int labelColor(StatsData data) { return active(data) ? GREEN : 0xFFFFFF; }
    @Override public void onSelect(StatsData data) {
        ((SpeedLimitData) data).dmzrevamp$setSpeedLimit(0);
        DmzRevampNetwork.CHANNEL.sendToServer(new SetSpeedLimitC2SPacket(0));
        playToggle(false);
    }
    @Override public List<RadialNode> buildOptions(StatsData data) {
        List<RadialNode> options = new ArrayList<>();
        for (int value = 3000; value >= 100; value -= 100) options.add(new Option(value));
        return options;
    }

    private static final class Option extends AbstractRadialNode {
        private final int value;
        private Option(int value) { this.value = value; }
        @Override public Component label(StatsData data) { return Component.literal(value + "%"); }
        @Override public ResourceLocation icon(StatsData data) { return null; }
        @Override public boolean active(StatsData data) { return ((SpeedLimitData) data).dmzrevamp$getSpeedLimit() == value; }
        @Override public int labelColor(StatsData data) { return active(data) ? GREEN : 0xFFFFFF; }
        @Override public void onSelect(StatsData data) {
            ((SpeedLimitData) data).dmzrevamp$setSpeedLimit(value);
            DmzRevampNetwork.CHANNEL.sendToServer(new SetSpeedLimitC2SPacket(value));
            playClick();
        }
    }
}
