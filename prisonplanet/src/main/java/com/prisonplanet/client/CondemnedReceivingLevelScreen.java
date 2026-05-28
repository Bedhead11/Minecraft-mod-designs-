package com.prisonplanet.client;

import com.prisonplanet.core.ModBlocks;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class CondemnedReceivingLevelScreen extends ReceivingLevelScreen {
    @Nullable
    private TextureAtlasSprite portalSprite;

    public CondemnedReceivingLevelScreen(BooleanSupplier levelReceived, Reason reason) {
        super(levelReceived, reason);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(0, 0, -90, graphics.guiWidth(), graphics.guiHeight(), getPortalSprite());
    }

    private TextureAtlasSprite getPortalSprite() {
        if (this.portalSprite == null) {
            this.portalSprite = this.minecraft.getBlockRenderer()
                    .getBlockModelShaper()
                    .getParticleIcon(ModBlocks.CONDEMNED_PORTAL.get().defaultBlockState());
        }
        return this.portalSprite;
    }
}
