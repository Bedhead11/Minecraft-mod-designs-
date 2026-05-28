package com.prisonplanet.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.prisonplanet.dimension.CyclePhase;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Hostile open sky for The Condemned: a swollen solar disk and a moonless night.
 */
public final class CondemnedDimensionEffects extends DimensionSpecialEffects {
    private static final int STAR_ATTEMPTS = 4800;
    private static final ResourceLocation SKY_BACKDROP =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "textures/environment/condemned_sky.png");
    private static final ResourceLocation SUN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "textures/environment/condemned_sun.png");
    private VertexBuffer starBuffer;

    public CondemnedDimensionEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(
                brightness * 0.74F + 0.10F,
                brightness * 0.47F + 0.06F,
                brightness * 0.33F + 0.05F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null;
    }

    @Override
    public boolean renderClouds(
            ClientLevel level,
            int ticks,
            float partialTick,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            Matrix4f modelViewMatrix,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog) {
        setupFog.run();
        FogType fogType = camera.getFluidInCamera();
        if (isFoggy || fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW || skyBlockedByEffect(camera)) {
            return true;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);
        float cycleProgress = cycleProgress(level, partialTick);
        float daylight = daylight(cycleProgress);
        float weatherVisibility = 1.0F - level.getRainLevel(partialTick);

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        drawAtmosphere(poseStack.last().pose(), daylight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawBackdrop(poseStack.last().pose(), 0.26F + (1.0F - daylight) * 0.66F);

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        float stars = Mth.clamp((1.0F - daylight) * 1.35F * weatherVisibility, 0.0F, 1.0F);
        if (stars > 0.01F) {
            drawStars(poseStack.last().pose(), projectionMatrix, stars);
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(cycleProgress * 360.0F - 45.0F));
        drawSun(poseStack.last().pose(), daylight * weatherVisibility);
        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        setupFog.run();
        return true;
    }

    private static float cycleProgress(ClientLevel level, float partialTick) {
        double cycleTick = Math.floorMod(level.getDayTime(), CyclePhase.CYCLE_LENGTH) + partialTick;
        return (float) (cycleTick / CyclePhase.CYCLE_LENGTH);
    }

    private static float daylight(float cycleProgress) {
        if (cycleProgress < 0.25F) {
            return 1.0F;
        }
        if (cycleProgress < 0.50F) {
            return 1.0F - (cycleProgress - 0.25F) * 4.0F;
        }
        if (cycleProgress < 0.75F) {
            return 0.0F;
        }
        return (cycleProgress - 0.75F) * 4.0F;
    }

    private static boolean skyBlockedByEffect(Camera camera) {
        return camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS));
    }

    private static void drawAtmosphere(Matrix4f matrix, float daylight) {
        float red = 0.015F + daylight * 0.22F;
        float green = 0.008F + daylight * 0.072F;
        float blue = 0.025F + daylight * 0.035F;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, 0.0F, 16.0F, 0.0F).setColor(red, green, blue, 1.0F);
        for (int degrees = -180; degrees <= 180; degrees += 30) {
            float angle = degrees * Mth.DEG_TO_RAD;
            buffer.addVertex(matrix, 512.0F * Mth.cos(angle), 16.0F, 512.0F * Mth.sin(angle))
                    .setColor(red * 0.50F, green * 0.42F, blue * 0.58F, 1.0F);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawBackdrop(Matrix4f matrix, float alpha) {
        int opacity = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SKY_BACKDROP);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addBackdropFace(buffer, matrix,
                -120.0F, 100.0F, -120.0F, -120.0F, 100.0F, 120.0F,
                120.0F, 100.0F, 120.0F, 120.0F, 100.0F, -120.0F, opacity);
        addBackdropFace(buffer, matrix,
                -120.0F, -25.0F, -120.0F, 120.0F, -25.0F, -120.0F,
                120.0F, 100.0F, -120.0F, -120.0F, 100.0F, -120.0F, opacity);
        addBackdropFace(buffer, matrix,
                120.0F, -25.0F, -120.0F, 120.0F, -25.0F, 120.0F,
                120.0F, 100.0F, 120.0F, 120.0F, 100.0F, -120.0F, opacity);
        addBackdropFace(buffer, matrix,
                120.0F, -25.0F, 120.0F, -120.0F, -25.0F, 120.0F,
                -120.0F, 100.0F, 120.0F, 120.0F, 100.0F, 120.0F, opacity);
        addBackdropFace(buffer, matrix,
                -120.0F, -25.0F, 120.0F, -120.0F, -25.0F, -120.0F,
                -120.0F, 100.0F, -120.0F, -120.0F, 100.0F, 120.0F, opacity);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addBackdropFace(
            BufferBuilder buffer,
            Matrix4f matrix,
            float minX,
            float minY,
            float minZ,
            float secondX,
            float secondY,
            float secondZ,
            float thirdX,
            float thirdY,
            float thirdZ,
            float fourthX,
            float fourthY,
            float fourthZ,
            int alpha) {
        buffer.addVertex(matrix, minX, minY, minZ).setUv(0.0F, 1.0F).setWhiteAlpha(alpha);
        buffer.addVertex(matrix, secondX, secondY, secondZ).setUv(1.0F, 1.0F).setWhiteAlpha(alpha);
        buffer.addVertex(matrix, thirdX, thirdY, thirdZ).setUv(1.0F, 0.0F).setWhiteAlpha(alpha);
        buffer.addVertex(matrix, fourthX, fourthY, fourthZ).setUv(0.0F, 0.0F).setWhiteAlpha(alpha);
    }

    private static void drawSun(Matrix4f matrix, float visibility) {
        if (visibility <= 0.01F) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawSunLayer(matrix, 83.0F, 0.98F, 0.12F, 0.015F, 0.12F * visibility, true);
        drawSunLayer(matrix, 66.0F, 1.0F, 0.22F, 0.025F, 0.24F * visibility, true);
        drawSunLayer(matrix, 58.0F, 1.0F, 0.36F, 0.045F, 0.62F * visibility, false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SUN_TEXTURE);
        BufferBuilder sun = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int opacity = Mth.clamp(Math.round(visibility * 255.0F), 0, 255);
        float radius = 52.0F;
        sun.addVertex(matrix, -radius, 100.0F, -radius).setUv(0.0F, 0.0F).setWhiteAlpha(opacity);
        sun.addVertex(matrix, radius, 100.0F, -radius).setUv(1.0F, 0.0F).setWhiteAlpha(opacity);
        sun.addVertex(matrix, radius, 100.0F, radius).setUv(1.0F, 1.0F).setWhiteAlpha(opacity);
        sun.addVertex(matrix, -radius, 100.0F, radius).setUv(0.0F, 1.0F).setWhiteAlpha(opacity);
        BufferUploader.drawWithShader(sun.buildOrThrow());
    }

    private static void drawSunLayer(
            Matrix4f matrix, float radius, float red, float green, float blue, float alpha, boolean ragged) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, 0.0F, 100.0F, 0.0F).setColor(red, green, blue, alpha);
        int points = 48;
        for (int i = 0; i <= points; i++) {
            float angle = i * Mth.TWO_PI / points;
            float corona = ragged ? 1.0F + 0.10F * Mth.sin(angle * 11.0F) + 0.06F * Mth.cos(angle * 17.0F) : 1.0F;
            float edgeRadius = radius * corona;
            buffer.addVertex(matrix, Mth.cos(angle) * edgeRadius, 100.0F, Mth.sin(angle) * edgeRadius)
                    .setColor(red, green, blue, ragged ? 0.0F : alpha);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void drawStars(Matrix4f matrix, Matrix4f projectionMatrix, float brightness) {
        ensureStarBuffer();
        RenderSystem.setShaderColor(brightness, brightness * 0.93F, brightness * 0.86F, brightness);
        FogRenderer.setupNoFog();
        starBuffer.bind();
        starBuffer.drawWithShader(matrix, projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
    }

    private void ensureStarBuffer() {
        if (starBuffer != null) {
            return;
        }
        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        starBuffer.bind();
        starBuffer.upload(buildStars());
        VertexBuffer.unbind();
    }

    private static MeshData buildStars() {
        RandomSource random = RandomSource.create(731991L);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < STAR_ATTEMPTS; i++) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 2.0F - 1.0F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            float lengthSquared = Mth.lengthSquared(x, y, z);
            if (lengthSquared <= 0.01F || lengthSquared >= 1.0F) {
                continue;
            }
            float size = 0.10F + random.nextFloat() * 0.24F;
            Vector3f center = new Vector3f(x, y, z).normalize(100.0F);
            Quaternionf rotation = new Quaternionf()
                    .rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), center)
                    .rotateZ((float) (random.nextDouble() * Mth.TWO_PI));
            buffer.addVertex(new Vector3f(center).add(new Vector3f(size, -size, 0.0F).rotate(rotation)));
            buffer.addVertex(new Vector3f(center).add(new Vector3f(size, size, 0.0F).rotate(rotation)));
            buffer.addVertex(new Vector3f(center).add(new Vector3f(-size, size, 0.0F).rotate(rotation)));
            buffer.addVertex(new Vector3f(center).add(new Vector3f(-size, -size, 0.0F).rotate(rotation)));
        }
        return buffer.buildOrThrow();
    }
}
