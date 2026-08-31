package com.dmzrevamp.mixin.client;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dragonminez.client.render.layer.DMZHairLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(DMZHairLayer.class)
public abstract class DMZHairLayerHelmetListModeMixin {
    // Changes the DMZ helmet hair list from whitelist behavior to blacklist behavior when configured.
    @Redirect(
            method = "renderHair",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private boolean dmzrevamp$applyHelmetHairListMode(List<String> helmetsThatKeepHair, Object helmetId) {
        boolean listed = helmetsThatKeepHair.contains(helmetId);
        return DmzRevampConfig.HELMET_KEEP_HAIR_LIST_MODE.get() == DmzRevampConfig.HelmetKeepHairListMode.BLACKLIST
                ? !listed
                : listed;
    }
}
