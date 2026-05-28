package com.prisonplanet.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.joml.Vector3f;

/**
 * Presents Suspended Brine as a translucent ceiling liquid with a visible underside.
 */
public final class SuspendedBrineClientExtension implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "block/suspended_brine_still");
    private static final ResourceLocation FLOWING =
            ResourceLocation.fromNamespaceAndPath("prisonplanet", "block/suspended_brine_flow");
    private static final int TINT = 0xD86BC7C4;

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING;
    }

    @Override
    public int getTintColor() {
        return TINT;
    }

    @Override
    public Vector3f modifyFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector3f fluidFogColor) {
        return new Vector3f(0.06F, 0.25F, 0.28F);
    }

    @Override
    public boolean renderFluid(
            FluidState fluidState,
            BlockAndTintGetter level,
            BlockPos pos,
            VertexConsumer buffer,
            BlockState blockState) {
        TextureAtlasSprite[] sprites = FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        TextureAtlasSprite still = sprites[0];
        TextureAtlasSprite flowing = sprites[1];
        float alpha = (float) (TINT >>> 24 & 255) / 255.0F;
        float red = (float) (TINT >>> 16 & 255) / 255.0F;
        float green = (float) (TINT >>> 8 & 255) / 255.0F;
        float blue = (float) (TINT & 255) / 255.0F;
        float x = pos.getX() & 15;
        float y = pos.getY() & 15;
        float z = pos.getZ() & 15;
        float surface = y + 1.0F - fluidState.getHeight(level, pos) + 0.001F;
        float ceiling = y + 0.999F;
        int light = LevelRenderer.getLightColor(level, pos);

        if (isOpenFace(level, pos.below(), fluidState)) {
            float shade = level.getShade(Direction.DOWN, true);
            quad(buffer, x, surface, z + 1.0F, x, surface, z,
                    x + 1.0F, surface, z, x + 1.0F, surface, z + 1.0F,
                    still, red * shade, green * shade, blue * shade, alpha, light, 0.0F, -1.0F, 0.0F);
        }
        if (isOpenFace(level, pos.above(), fluidState)) {
            float shade = level.getShade(Direction.UP, true);
            quad(buffer, x, ceiling, z, x, ceiling, z + 1.0F,
                    x + 1.0F, ceiling, z + 1.0F, x + 1.0F, ceiling, z,
                    still, red * shade, green * shade, blue * shade, alpha, light, 0.0F, 1.0F, 0.0F);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!isOpenFace(level, pos.relative(direction), fluidState)) {
                continue;
            }
            float shade = level.getShade(direction, true);
            float shadedRed = red * shade;
            float shadedGreen = green * shade;
            float shadedBlue = blue * shade;
            switch (direction) {
                case NORTH -> quad(buffer, x + 1.0F, ceiling, z + 0.001F, x, ceiling, z + 0.001F,
                        x, surface, z + 0.001F, x + 1.0F, surface, z + 0.001F,
                        flowing, shadedRed, shadedGreen, shadedBlue, alpha, light, 0.0F, 0.0F, -1.0F);
                case SOUTH -> quad(buffer, x, ceiling, z + 0.999F, x + 1.0F, ceiling, z + 0.999F,
                        x + 1.0F, surface, z + 0.999F, x, surface, z + 0.999F,
                        flowing, shadedRed, shadedGreen, shadedBlue, alpha, light, 0.0F, 0.0F, 1.0F);
                case WEST -> quad(buffer, x + 0.001F, ceiling, z, x + 0.001F, ceiling, z + 1.0F,
                        x + 0.001F, surface, z + 1.0F, x + 0.001F, surface, z,
                        flowing, shadedRed, shadedGreen, shadedBlue, alpha, light, -1.0F, 0.0F, 0.0F);
                case EAST -> quad(buffer, x + 0.999F, ceiling, z + 1.0F, x + 0.999F, ceiling, z,
                        x + 0.999F, surface, z, x + 0.999F, surface, z + 1.0F,
                        flowing, shadedRed, shadedGreen, shadedBlue, alpha, light, 1.0F, 0.0F, 0.0F);
                default -> {
                }
            }
        }
        return true;
    }

    private static boolean isOpenFace(BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return !level.getFluidState(pos).getType().isSame(fluidState.getType())
                && !level.getBlockState(pos).isSolidRender(level, pos);
    }

    private static void quad(
            VertexConsumer buffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            TextureAtlasSprite sprite,
            float red, float green, float blue, float alpha, int light,
            float normalX, float normalY, float normalZ) {
        vertex(buffer, x1, y1, z1, red, green, blue, alpha, sprite.getU(0.0F), sprite.getV(0.0F), light, normalX, normalY, normalZ);
        vertex(buffer, x2, y2, z2, red, green, blue, alpha, sprite.getU(0.0F), sprite.getV(1.0F), light, normalX, normalY, normalZ);
        vertex(buffer, x3, y3, z3, red, green, blue, alpha, sprite.getU(1.0F), sprite.getV(1.0F), light, normalX, normalY, normalZ);
        vertex(buffer, x4, y4, z4, red, green, blue, alpha, sprite.getU(1.0F), sprite.getV(0.0F), light, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer buffer,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v, int light,
            float normalX, float normalY, float normalZ) {
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }
}
