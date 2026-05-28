package com.prisonplanet.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Animated cubic rigs for the Condemned fauna. Every registered entity uses a
 * distinct layer definition while sharing lightweight locomotion animation.
 */
public class CondemnedCreatureModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;

    public CondemnedCreatureModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (root.hasChild("head")) {
            root.getChild("head").yRot = netHeadYaw * ((float) Math.PI / 180F);
            root.getChild("head").xRot = headPitch * ((float) Math.PI / 180F);
        }
        float stride = Mth.cos(limbSwing * 0.6662F) * 1.35F * limbSwingAmount;
        swing("right_leg", stride);
        swing("left_leg", -stride);
        swing("right_front_leg", stride);
        swing("left_front_leg", -stride);
        swing("right_hind_leg", -stride);
        swing("left_hind_leg", stride);
        swing("right_arm", -stride * 0.8F);
        swing("left_arm", stride * 0.8F);
        if (root.hasChild("left_wing")) {
            float flutter = 0.38F + Mth.sin(ageInTicks * 0.35F) * 0.35F;
            root.getChild("left_wing").zRot = -flutter;
            root.getChild("right_wing").zRot = flutter;
        }
        if (root.hasChild("tail")) {
            root.getChild("tail").yRot = Mth.sin(ageInTicks * 0.18F) * 0.18F;
        }
    }

    private void swing(String part, float rotation) {
        if (root.hasChild(part)) {
            root.getChild(part).xRot = rotation;
        }
    }

    public static LayerDefinition createAshStalkerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -7.0F, -4.5F, 7.0F, 7.0F, 7.0F)
                .texOffs(28, 0).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, -2.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4.0F, 0.0F, -2.5F, 8.0F, 10.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.26F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(28, 14)
                .addBox(-2.5F, 0.0F, -2.0F, 3.0F, 13.0F, 3.0F)
                .texOffs(42, 14).addBox(-3.0F, 11.0F, -3.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(-4.0F, 8.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 14)
                .mirror().addBox(-0.5F, 0.0F, -2.0F, 3.0F, 13.0F, 3.0F)
                .texOffs(42, 14).addBox(0.0F, 11.0F, -3.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(4.0F, 8.0F, 0.0F));
        bipedLegs(root, 2.0F, 16.0F, 8.0F);
        return layer(mesh);
    }

    public static LayerDefinition createChainWretchLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14)
                .addBox(-3.5F, 0.0F, -2.0F, 7.0F, 13.0F, 4.0F)
                .texOffs(26, 14).addBox(-5.0F, 4.0F, -2.6F, 10.0F, 2.0F, 1.0F)
                .texOffs(48, 0).addBox(-1.0F, 13.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        narrowArms(root, 4.0F, 6.0F, 15.0F);
        bipedLegs(root, 2.0F, 17.0F, 7.0F);
        return layer(mesh);
    }

    public static LayerDefinition createFrostWraithLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F)
                .texOffs(30, 0).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F)
                .texOffs(25, 18).addBox(-5.0F, 8.0F, -2.5F, 10.0F, 7.0F, 5.0F)
                .texOffs(0, 34).addBox(-3.0F, 15.0F, -1.5F, 6.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        narrowArms(root, 4.5F, 7.0F, 13.0F);
        return layer(mesh);
    }

    public static LayerDefinition createVentCrawlerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -3.0F, -6.0F, 8.0F, 5.0F, 6.0F)
                .texOffs(30, 0).addBox(-3.0F, -1.0F, -7.0F, 6.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 17.0F, -5.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13)
                .addBox(-5.0F, -3.0F, -5.0F, 10.0F, 6.0F, 11.0F)
                .texOffs(42, 12).addBox(-3.0F, -5.0F, -1.0F, 6.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        crawlerLegs(root, 18.0F);
        return layer(mesh);
    }

    public static LayerDefinition createCagePhantomLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14)
                .addBox(-5.0F, 0.0F, -3.0F, 1.0F, 14.0F, 1.0F)
                .addBox(4.0F, 0.0F, -3.0F, 1.0F, 14.0F, 1.0F)
                .addBox(-5.0F, 0.0F, 2.0F, 1.0F, 14.0F, 1.0F)
                .addBox(4.0F, 0.0F, 2.0F, 1.0F, 14.0F, 1.0F)
                .texOffs(8, 15).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 1.0F, 6.0F)
                .texOffs(8, 23).addBox(-5.0F, 13.0F, -3.0F, 10.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        narrowArms(root, 5.5F, 8.0F, 11.0F);
        return layer(mesh);
    }

    public static LayerDefinition createCinderHoundLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -4.0F, -7.0F, 7.0F, 6.0F, 7.0F)
                .texOffs(29, 0).addBox(-2.0F, -2.0F, -9.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 13.0F, -7.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-4.5F, -4.0F, -7.0F, 9.0F, 8.0F, 14.0F)
                .texOffs(42, 10).addBox(-1.0F, -7.0F, -4.0F, 2.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        quadrupedLegs(root, 3.0F, 17.0F, 5.0F, 6.0F);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(42, 24)
                .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, 7.0F, -0.55F, 0.0F, 0.0F));
        return layer(mesh);
    }

    public static LayerDefinition createRustSentinelLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0).addBox(-4.5F, -5.0F, -4.5F, 9.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F)
                .texOffs(34, 17).addBox(-8.0F, 1.0F, -3.0F, 3.0F, 4.0F, 6.0F)
                .addBox(5.0F, 1.0F, -3.0F, 3.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
        narrowArms(root, 6.0F, 8.0F, 12.0F);
        bipedLegs(root, 2.5F, 17.0F, 7.0F);
        return layer(mesh);
    }

    public static LayerDefinition createSlagBruteLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -7.0F, -4.0F, 10.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17)
                .addBox(-8.0F, 0.0F, -4.0F, 16.0F, 11.0F, 8.0F)
                .texOffs(0, 38).addBox(-6.0F, 11.0F, -3.0F, 12.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(38, 19)
                .addBox(-5.0F, 0.0F, -3.5F, 5.0F, 12.0F, 7.0F),
                PartPose.offset(-8.0F, 8.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(38, 19)
                .mirror().addBox(0.0F, 0.0F, -3.5F, 5.0F, 12.0F, 7.0F),
                PartPose.offset(8.0F, 8.0F, 0.0F));
        bipedLegs(root, 4.0F, 19.0F, 5.0F);
        return layer(mesh);
    }

    public static LayerDefinition createFurnaceWardenLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F)
                .texOffs(42, 0).addBox(-7.0F, -10.0F, -2.0F, 2.0F, 5.0F, 2.0F)
                .addBox(5.0F, -10.0F, -2.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-8.0F, 0.0F, -5.0F, 16.0F, 15.0F, 10.0F)
                .texOffs(0, 47).addBox(-4.0F, 4.0F, -6.0F, 8.0F, 7.0F, 1.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 23)
                .addBox(-5.0F, 0.0F, -4.0F, 5.0F, 15.0F, 8.0F),
                PartPose.offset(-8.0F, 5.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 23)
                .mirror().addBox(0.0F, 0.0F, -4.0F, 5.0F, 15.0F, 8.0F),
                PartPose.offset(8.0F, 5.0F, 0.0F));
        bipedLegs(root, 4.0F, 18.0F, 6.0F);
        return layer(mesh);
    }

    public static LayerDefinition createAshGrazerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F)
                .texOffs(29, 0).addBox(-3.0F, -1.0F, -8.0F, 6.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 11.0F, -8.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-6.0F, -5.0F, -8.0F, 12.0F, 10.0F, 16.0F)
                .texOffs(25, 0).addBox(-5.0F, -6.0F, -3.0F, 10.0F, 1.0F, 7.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        quadrupedLegs(root, 3.0F, 16.0F, 6.0F, 7.0F);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(46, 22)
                .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 11.0F, 8.0F));
        return layer(mesh);
    }

    public static LayerDefinition createGlacialDrifterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.5F, -3.0F, -4.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 14.0F, -3.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 9.0F)
                .texOffs(31, 10).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 30)
                .addBox(0.0F, -1.0F, -4.0F, 12.0F, 1.0F, 9.0F)
                .texOffs(0, 42).addBox(5.0F, -1.0F, 4.0F, 7.0F, 1.0F, 5.0F),
                PartPose.offset(3.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 30)
                .mirror().addBox(-12.0F, -1.0F, -4.0F, 12.0F, 1.0F, 9.0F)
                .texOffs(0, 42).addBox(-12.0F, -1.0F, 4.0F, 7.0F, 1.0F, 5.0F),
                PartPose.offset(-3.0F, 13.0F, 0.0F));
        return layer(mesh);
    }

    public static LayerDefinition createSalvagePorterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -3.0F, -7.0F, 8.0F, 6.0F, 7.0F)
                .texOffs(32, 0).addBox(-5.0F, -1.0F, -8.0F, 10.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -7.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-7.0F, -5.0F, -8.0F, 14.0F, 10.0F, 16.0F)
                .texOffs(0, 43).addBox(-6.0F, -8.0F, -4.0F, 12.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        quadrupedLegs(root, 4.0F, 18.0F, 4.0F, 6.0F);
        return layer(mesh);
    }

    private static void narrowArms(PartDefinition root, float x, float y, float length) {
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 20)
                .addBox(-2.0F, 0.0F, -1.5F, 2.0F, length, 3.0F), PartPose.offset(-x, y, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 20)
                .mirror().addBox(0.0F, 0.0F, -1.5F, 2.0F, length, 3.0F), PartPose.offset(x, y, 0.0F));
    }

    private static void bipedLegs(PartDefinition root, float x, float y, float length) {
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, length, 4.0F), PartPose.offset(-x, y, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48)
                .mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, length, 4.0F), PartPose.offset(x, y, 0.0F));
    }

    private static void quadrupedLegs(PartDefinition root, float x, float y, float z, float length) {
        CubeListBuilder leg = CubeListBuilder.create().texOffs(48, 44)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, length, 3.0F);
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-x, y, -z));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(x, y, -z));
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-x, y, z));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(x, y, z));
    }

    private static void crawlerLegs(PartDefinition root, float y) {
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 42).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F);
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-5.0F, y, -4.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(5.0F, y, -4.0F));
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-5.0F, y, 4.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(5.0F, y, 4.0F));
    }

    private static LayerDefinition layer(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, 64, 64);
    }
}
