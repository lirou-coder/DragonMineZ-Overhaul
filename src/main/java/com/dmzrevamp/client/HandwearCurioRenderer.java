package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.item.HandwearItem;
import com.dmzrevamp.item.HandwearType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class HandwearCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation GLOVES_TEXTURE = ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, "textures/armor/gloves_layer1.png");
    private static final ResourceLocation WRISTBANDS_TEXTURE = ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, "textures/armor/wristbands_layer1.png");
    private final HumanoidModel<LivingEntity> armorModel;

    public HandwearCurioRenderer() {
        ModelPart armorLayer = net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR);
        this.armorModel = new HumanoidModel<>(armorLayer);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack,
                                                                         SlotContext slotContext,
                                                                         PoseStack poseStack,
                                                                         RenderLayerParent<T, M> parent,
                                                                         MultiBufferSource buffer,
                                                                         int light,
                                                                         float limbSwing,
                                                                         float limbSwingAmount,
                                                                         float partialTicks,
                                                                         float ageInTicks,
                                                                         float netHeadYaw,
                                                                         float headPitch) {
        if (!(stack.getItem() instanceof HandwearItem handwear) || !(parent.getModel() instanceof HumanoidModel<?> parentModel)) {
            return;
        }

        ((HumanoidModel) parentModel).copyPropertiesTo((HumanoidModel) armorModel);
        armorModel.setAllVisible(false);
        armorModel.leftArm.visible = true;
        armorModel.rightArm.visible = true;

        // DMZ chest armor textures render on the outer armor model; handwear uses the same arm surface.
        ResourceLocation texture = handwear.getHandwearType() == HandwearType.GLOVES ? GLOVES_TEXTURE : WRISTBANDS_TEXTURE;
        VertexConsumer vertex = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        armorModel.renderToBuffer(poseStack, vertex, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
    }
}
