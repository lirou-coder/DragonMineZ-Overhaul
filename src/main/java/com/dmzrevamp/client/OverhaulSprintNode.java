package com.dmzrevamp.client;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.client.gui.radial.nodes.FlyNode;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Sprint toggle with the Overhaul speed limiter nested in Sprint's radial tab. */
public class OverhaulSprintNode extends FlyNode {
    @Override
    public Component label(StatsData stats) {
        return Component.translatable("skill.dragonminez.sprint");
    }

    @Override
    public ResourceLocation icon(StatsData stats) {
        return icon("sprint");
    }

    @Override
    public boolean visible(StatsData stats) {
        return stats.getSkills().getSkillLevel("sprint") > 0;
    }

    @Override
    public boolean active(StatsData stats) {
        return stats.getSkills().isSkillActive("sprint");
    }

    @Override
    public void onSelect(StatsData stats) {
        NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "sprint", 0));
        playToggle(!active(stats));
    }

    @Override
    protected List<RadialNode> buildChildren(StatsData stats) {
        return List.of(new OverhaulSpeedLimitNode());
    }
}
