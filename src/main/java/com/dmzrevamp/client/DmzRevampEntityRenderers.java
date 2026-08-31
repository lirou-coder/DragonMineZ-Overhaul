package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.entity.DmzRevampEntities;
import com.dmzrevamp.entity.SagaGogetaEntity;
import com.dragonminez.client.init.entities.renderer.sagas.DBSagasRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DmzRevampEntityRenderers {
    private DmzRevampEntityRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DmzRevampEntities.SAGA_GOGETA_BASE.get(), GogetaBaseRenderer::new);
        event.registerEntityRenderer(DmzRevampEntities.SAGA_GOGETA_SSJ.get(), GogetaSsjRenderer::new);
    }

    private static final class GogetaBaseRenderer extends DBSagasRenderer<SagaGogetaEntity.Base> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
                DmzRevampMod.MODID, "textures/entity/sagas/saga_gogeta_base.png");

        private GogetaBaseRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(SagaGogetaEntity.Base entity) {
            return TEXTURE;
        }
    }

    private static final class GogetaSsjRenderer extends DBSagasRenderer<SagaGogetaEntity.Ssj> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
                DmzRevampMod.MODID, "textures/entity/sagas/saga_gogeta_ssj.png");

        private GogetaSsjRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(SagaGogetaEntity.Ssj entity) {
            return TEXTURE;
        }
    }
}
