package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.item.HandwearHelper;
import com.dmzrevamp.item.HandwearItem;
import com.dmzrevamp.item.HandwearType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

public final class HandwearDmzArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
    private static final float ARM_SCALE = 1.005F;
    private static final ResourceLocation GLOVES_TEXTURE = ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, "textures/armor/gloves_layer1.png");
    private static final ResourceLocation WRISTBANDS_TEXTURE = ResourceLocation.fromNamespaceAndPath(DmzRevampMod.MODID, "textures/armor/wristbands_layer1.png");

    public HandwearDmzArmorLayer(GeoRenderer<T> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack,
                              T player,
                              GeoBone bone,
                              RenderType renderType,
                              MultiBufferSource bufferSource,
                              VertexConsumer buffer,
                              float partialTick,
                              int packedLight,
                              int packedOverlay) {
        if (!isArmorArmBone(bone)) {
            return;
        }

        Optional<ItemStack> equippedHandwear = HandwearHelper.getEquippedHandwear(player, HandwearType.GLOVES)
                .or(() -> HandwearHelper.getEquippedHandwear(player, HandwearType.WRISTBANDS));
        if (equippedHandwear.isEmpty() || !(equippedHandwear.get().getItem() instanceof HandwearItem handwear)) {
            return;
        }

        ResourceLocation texture = handwear.getHandwearType() == HandwearType.GLOVES ? GLOVES_TEXTURE : WRISTBANDS_TEXTURE;
        renderArmBoneWithTexture(poseStack, player, bone, bufferSource, texture, partialTick, packedLight);
    }

    private static boolean isArmorArmBone(GeoBone bone) {
        if (bone == null) {
            return false;
        }
        return switch (bone.getName()) {
            case "armorLeftArm", "armor_left_arm", "armorRightArm", "armor_right_arm" -> true;
            default -> false;
        };
    }

    private void renderArmBoneWithTexture(PoseStack poseStack,
                                          T player,
                                          GeoBone bone,
                                          MultiBufferSource bufferSource,
                                          ResourceLocation texture,
                                          float partialTick,
                                          int packedLight) {
        float originalScaleX = bone.getScaleX();
        float originalScaleY = bone.getScaleY();
        float originalScaleZ = bone.getScaleZ();
        boolean originalHidden = bone.isHidden();

        // The texture is drawn on the current DMZ armor arm bone with a small fixed scale above other armor.
        bone.setHidden(false);
        bone.setScaleX(ARM_SCALE);
        bone.setScaleY(ARM_SCALE);
        bone.setScaleZ(ARM_SCALE);

        RenderType handwearRenderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer handwearBuffer = bufferSource.getBuffer(handwearRenderType);
        getRenderer().renderRecursively(
                poseStack,
                player,
                bone,
                handwearRenderType,
                bufferSource,
                handwearBuffer,
                true,
                partialTick,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1F,
                1F,
                1F,
                1F
        );

        bone.setScaleX(originalScaleX);
        bone.setScaleY(originalScaleY);
        bone.setScaleZ(originalScaleZ);
        bone.setHidden(originalHidden);
    }
}
