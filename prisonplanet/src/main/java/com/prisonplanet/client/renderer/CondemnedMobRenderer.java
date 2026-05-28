package com.prisonplanet.client.renderer;

import com.prisonplanet.client.model.CondemnedCreatureModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class CondemnedMobRenderer<T extends Mob> extends MobRenderer<T, CondemnedCreatureModel<T>> {
    private final ResourceLocation texture;

    public CondemnedMobRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String textureId, float shadowRadius) {
        super(context, new CondemnedCreatureModel<>(context.bakeLayer(layer)), shadowRadius);
        this.texture = ResourceLocation.fromNamespaceAndPath("prisonplanet", "textures/entity/" + textureId + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
