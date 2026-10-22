package com.dmzrevamp.client;

import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.SetOverhaulSpeedLimitC2SPacket;
import com.dmzrevamp.revamp.OverhaulSpeedLimitState;
import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.FlightSpeedNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** FlightSpeedNode-compatible UI for limiting only bonuses contributed by Overhaul. */
public class OverhaulSpeedLimitNode extends FlightSpeedNode {
    @Override
    public Component label(StatsData stats) {
        return Component.translatable("gui.action.dmzrevamp.speed_limit");
    }

    @Override
    public boolean visible(StatsData stats) {
        return stats.getSkills().getSkillLevel("sprint") > 0;
    }

    @Override
    public boolean active(StatsData stats) {
        return OverhaulSpeedLimitState.get(net.minecraft.client.Minecraft.getInstance().player) < 100;
    }

    @Override
    public String faceText(StatsData stats) {
        return OverhaulSpeedLimitState.get(net.minecraft.client.Minecraft.getInstance().player) + "%";
    }

    @Override
    public int labelColor(StatsData stats) {
        return active(stats) ? GREEN : 0xFFFFFF;
    }

    @Override
    public void onSelect(StatsData stats) {
        setLimit(100);
        playToggle(false);
    }

    @Override
    public List<RadialNode> buildOptions() {
        List<RadialNode> options = new ArrayList<>();
        for (int value = 100; value >= 5; value -= 5) options.add(new Option(value));
        return options;
    }

    @Override
    public void adjustByWheel(StatsData stats, double delta) {
        if (delta == 0D) return;
        int current = OverhaulSpeedLimitState.get(net.minecraft.client.Minecraft.getInstance().player);
        int next = Math.max(5, Math.min(100, current + (delta > 0D ? 5 : -5)));
        if (next != current) {
            setLimit(next);
            playClick();
        }
    }

    private static void setLimit(int value) {
        DmzRevampNetwork.CHANNEL.sendToServer(new SetOverhaulSpeedLimitC2SPacket(value));
        OverhaulSpeedLimitState.setClient(value);
    }

    private static final class Option extends AbstractRadialNode {
        private final int value;

        private Option(int value) {
            this.value = value;
        }

        @Override
        public Component label(StatsData stats) {
            return Component.literal(value + "%");
        }

        @Override
        public ResourceLocation icon(StatsData stats) {
            return null;
        }

        @Override
        public boolean active(StatsData stats) {
            return OverhaulSpeedLimitState.get(net.minecraft.client.Minecraft.getInstance().player) == value;
        }

        @Override
        public int labelColor(StatsData stats) {
            return active(stats) ? GREEN : 0xFFFFFF;
        }

        @Override
        public void onSelect(StatsData stats) {
            setLimit(value);
            playClick();
        }
    }
}
