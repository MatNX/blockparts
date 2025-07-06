package com.matnx.blockparts.statestore;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import static com.matnx.blockparts.BlockParts.MODID;

public class StateStoreBlockEntityRenderer implements BlockEntityRenderer<StateStoreBlockEntity> {

    public StateStoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull StateStoreBlockEntity blockEntity, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();

        poseStack.pushPose();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    BlockState state = blockEntity.getStoredState(x, y, z);
                    if (!(state.getBlock() == Blocks.AIR)) { // Don't render air blocks
                        BlockPos blockpos = blockEntity.getBlockPos();
                        int light = getLightLevel(blockEntity.getLevel(), blockpos);
                        poseStack.translate((float) x /4, (float) y /4, (float) z /4);
                        dispatcher.renderSingleBlock(state, poseStack, bufferSource, light, packedOverlay, ModelData.builder().build(), null);
                        poseStack.translate((float) -x /4, (float) -y /4, (float) -z /4);
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private int getLightLevel(Level level, BlockPos pos) {
        int bLight = level.getBrightness(LightLayer.BLOCK, pos);
        int sLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(bLight, sLight);
    }
}
