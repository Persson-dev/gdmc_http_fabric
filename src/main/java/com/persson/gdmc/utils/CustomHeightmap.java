package com.persson.gdmc.utils;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;

public class CustomHeightmap {

	private final Chunk chunk;
	private final Predicate<BlockState> isOpaque;
	private final PaletteStorage data;

	public CustomHeightmap(Chunk chunk, Types heightmapType) {
		this.isOpaque = heightmapType.isOpaque();
		this.chunk = chunk;
		int i = MathHelper.ceilLog2(chunk.getHeight() + 1);
		this.data = new PackedIntegerArray(i, 256);
	 }

	public static CustomHeightmap primeHeightmaps(Chunk chunk, CustomHeightmap.Types heightmapType) {
		CustomHeightmap customHeightmap = new CustomHeightmap(chunk, heightmapType);
		int j = chunk.getTopY() + 16;
		BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable();

		for (int k = 0; k < 16; ++k) {
			for (int l = 0; l < 16; ++l) {
				for (int i1 = j - 1; i1 >= chunk.getBottomY(); --i1) {
					blockpos$mutableblockpos.set(k, i1, l);
					BlockState blockstate = chunk.getBlockState(blockpos$mutableblockpos);
					if (!blockstate.isAir()) {
						if (customHeightmap.isOpaque.test(blockstate)) {
							customHeightmap.setHeight(k, l, i1 + 1);
							break;
						}
					}
				}
			}
		}
		return customHeightmap;
	}

	private void setHeight(int x, int z, int y) {
		this.data.set(getIndex(x, z), y - this.chunk.getBottomY());
	}

	public int get(int x, int z) {
		return this.getFirstAvailable(getIndex(x, z));
	}

	private int getFirstAvailable(int index) {
		return this.data.get(index) + this.chunk.getBottomY();
	}

	private static int getIndex(int x, int z) {
		return x + z * 16;
	}

	private static final Predicate<BlockState> NO_PLANTS = blockState -> !blockState.isIn(BlockTags.LEAVES)
			&& !blockState.isIn(BlockTags.LOGS) && !blockState.isOf(Blocks.BEE_NEST)
			&& !blockState.isOf(Blocks.MANGROVE_ROOTS) && !blockState.isOf(Blocks.MUDDY_MANGROVE_ROOTS) &&
			!blockState.isOf(Blocks.BROWN_MUSHROOM_BLOCK) && !blockState.isOf(Blocks.RED_MUSHROOM_BLOCK)
			&& !blockState.isOf(Blocks.MUSHROOM_STEM) &&
			!blockState.isOf(Blocks.PUMPKIN) &&
			!blockState.isOf(Blocks.MELON) &&
			!blockState.isOf(Blocks.MOSS_BLOCK) && !blockState.isOf(Blocks.NETHER_WART_BLOCK) &&
			!blockState.isOf(Blocks.CACTUS) &&
			!blockState.isOf(Blocks.FARMLAND) &&
			!blockState.isIn(BlockTags.CORAL_BLOCKS) && !blockState.isOf(Blocks.SPONGE)
			&& !blockState.isOf(Blocks.WET_SPONGE) &&
			!blockState.isOf(Blocks.BAMBOO) &&
			!blockState.isOf(Blocks.COBWEB) &&
			!blockState.isOf(Blocks.SCULK);

	public enum Types implements StringIdentifiable {
		MOTION_BLOCKING_NO_PLANTS(
				"MOTION_BLOCKING_NO_PLANTS",
				(blockState) -> (blockState.isOpaque()
						|| !blockState.getFluidState().isEmpty()) && NO_PLANTS.test(blockState)),
		OCEAN_FLOOR_NO_PLANTS(
				"OCEAN_FLOOR_NO_PLANTS",
				(blockState) -> blockState.isOpaque()
						&& NO_PLANTS.test(blockState));

		private final String serializationKey;
		private final Predicate<BlockState> isOpaque;

		Types(String serializationKey, Predicate<BlockState> predicate) {
			this.serializationKey = serializationKey;
			this.isOpaque = predicate;
		}

		public Predicate<BlockState> isOpaque() {
			return this.isOpaque;
		}

		@Override
		public String asString() {
			return this.serializationKey;
		}
	}
}
