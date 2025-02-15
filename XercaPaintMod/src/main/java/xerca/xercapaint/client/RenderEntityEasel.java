package xerca.xercapaint.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xerca.xercapaint.common.XercaPaint;
import xerca.xercapaint.common.entity.EntityEasel;
import xerca.xercapaint.common.item.ItemCanvas;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class RenderEntityEasel extends EntityRenderer<EntityEasel> implements RenderLayerParent<EntityEasel, EaselModel> {
    protected EaselModel model;
    protected final List<RenderLayer<EntityEasel, EaselModel>> layers = Lists.newArrayList();
    static public RenderEntityEasel theInstance;
    static private final ResourceLocation woodTexture = new ResourceLocation(XercaPaint.MODID, "textures/block/birch_long.png");

    RenderEntityEasel(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new EaselModel(ctx.bakeLayer(ClientStuff.EASEL_MAIN_LAYER));
        this.layers.add(new EaselCanvasLayer(this));
    }

    @Override
    public EaselModel getModel() {
        return this.model;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityEasel entity) {
        return woodTexture;
    }

    @Override
    public void render(EntityEasel entity, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();

        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-entityYaw));

        this.model.setupAnim(entity, 0, 0, 0, 0, 0);

        matrixStackIn.mulPose(Quaternion.fromXYZDegrees(new Vector3f(180, 0, 0)));
        matrixStackIn.translate(0, -1.5, 0);

        RenderType rendertype = this.model.renderType(this.getTextureLocation(entity));
        VertexConsumer vertexconsumer = bufferIn.getBuffer(rendertype);

        int i = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(false));
        this.model.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, i, 1.0F, 1.0F, 1.0F, 1.0F);

        this.layers.forEach(renderlayer -> renderlayer.render(matrixStackIn, bufferIn, packedLightIn, entity, 0, 0, 0, 0, 0, 0));

        matrixStackIn.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    @Override
    protected boolean shouldShowName(EntityEasel easel) {
        HitResult result = Minecraft.getInstance().hitResult;
        if(result instanceof EntityHitResult entityHitResult){
            if (Minecraft.renderNames() && entityHitResult.getEntity() == easel && !easel.getItem().isEmpty() && ItemCanvas.hasTitle(easel.getItem())) {
                double d0 = this.entityRenderDispatcher.distanceToSqr(easel);
                float f = easel.isDiscrete() ? 32.0F : 64.0F;
                return d0 < (double)(f * f);
            }
        }
        return false;
    }

    @Override
    protected void renderNameTag(EntityEasel easel, Component component, PoseStack poseStack, MultiBufferSource bufferSource, int p_115087_) {
        poseStack.pushPose();
        poseStack.translate(0, -0.5, 0);
        super.renderNameTag(easel, ItemCanvas.getFullLabel(easel.getItem()), poseStack, bufferSource, p_115087_);
        poseStack.popPose();
    }

    public static class RenderEntityEaselFactory implements EntityRendererProvider<EntityEasel> {
        @Override
        public EntityRenderer<EntityEasel> create(Context ctx) {
            theInstance = new RenderEntityEasel(ctx);
            return theInstance;
        }
    }
}