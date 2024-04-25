package com.persson.gdmc.utils;

import com.google.gson.Gson;
import com.persson.gdmc.handlers.HandlerBase.HttpException;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

public class BuildArea {

	private static BuildAreaInstance buildAreaInstance;

	public static BuildAreaInstance getBuildArea() {
		if (buildAreaInstance == null) {
			throw new HttpException("No build area is specified. Use the /setbuildarea command inside Minecraft to set a build area.", 404);
		}
		return buildAreaInstance;
	}

	public static BuildAreaInstance setBuildArea(BlockPos from, BlockPos to) {
		buildAreaInstance = new BuildAreaInstance(from, to);
		return buildAreaInstance;
	}

	public static void unsetBuildArea() {
		buildAreaInstance = null;
	}

	public static boolean isOutsideBuildArea(BlockPos blockPos, boolean withinBuildArea) {
		if (withinBuildArea) {
			return getBuildArea().isOutsideBuildArea(blockPos.getX(), blockPos.getZ());
		}
		return false;
	}

	public static boolean isOutsideBuildArea(BlockBox box, boolean withinBuildArea) {
		if (withinBuildArea) {
			return !getBuildArea().isInsideBuildArea(box);
		}
		return false;
	}

	public static BlockBox clampToBuildArea(BlockBox box, boolean withinBuildArea) {
		if (withinBuildArea) {
			return getBuildArea().clampBox(box);
		}
		return box;
	}

	public static BlockBox clampChunksToBuildArea(BlockBox box, boolean withinBuildArea) {
		if (withinBuildArea) {
			return getBuildArea().clampSectionBox(box);
		}
		return box;
	}

	public static String toJSONString() {
		return new Gson().toJson(getBuildArea());
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class BuildAreaInstance {

		// These 6 properties are used for JSON serialisation.
		private final int xFrom;
		private final int yFrom;
		private final int zFrom;
		private final int xTo;
		private final int yTo;
		private final int zTo;

		public final transient BlockBox box;
		public final transient BlockPos from;
		public final transient BlockPos to;
		public final transient ChunkPos sectionFrom;
		public final transient ChunkPos sectionTo;
		private final transient BlockBox sectionBox;


		private BuildAreaInstance(BlockPos from, BlockPos to) {
			this.box = BlockBox.create(from, to);
			this.from = from;
			this.to = to;
			sectionFrom = new ChunkPos(this.from);
			sectionTo = new ChunkPos(this.to);
			this.sectionBox = BlockBox.create(
				new Vec3i(sectionFrom.x, 0, sectionFrom.z),
				new Vec3i(sectionTo.x, 0, sectionTo.z)
			);
			xFrom = this.from.getX();
			yFrom = this.from.getY();
			zFrom = this.from.getZ();
			xTo = this.to.getX();
			yTo = this.to.getY();
			zTo = this.to.getZ();
		}

		public boolean isOutsideBuildArea(int x, int z) {
			return x < from.getX() || x > to.getX() || z < from.getZ() || z > to.getZ();
		}

		private boolean isInsideBuildArea(BlockBox otherBox) {
			return box.getMaxX() >= otherBox.getMaxX() && box.getMinX() <= otherBox.getMinX() && box.getMaxZ() >= otherBox.getMaxZ() && box.getMinZ() <= otherBox.getMinZ();
		}

		private BlockBox clampBox(BlockBox otherBox) {
			if (!box.intersects(otherBox)) {
				throw new HttpException("Requested area is outside of build area", 403);
			}

			return BlockBox.create(
				new Vec3i(
					Math.min(Math.max(box.getMinX(), otherBox.getMinX()), box.getMaxX()),
					Math.min(Math.max(box.getMinY(), otherBox.getMinY()), box.getMaxY()),
					Math.min(Math.max(box.getMinZ(), otherBox.getMinZ()), box.getMaxZ())
				),
				new Vec3i(
					Math.max(Math.min(box.getMaxX(), otherBox.getMaxX()), box.getMinX()),
					Math.max(Math.min(box.getMaxY(), otherBox.getMaxY()), box.getMinY()),
					Math.max(Math.min(box.getMaxZ(), otherBox.getMaxZ()), box.getMinZ())
				)
			);
		}

		private BlockBox clampSectionBox(BlockBox otherBox) {
			if (!sectionBox.intersects(otherBox)) {
				throw new HttpException("Requested area is outside of build area", 403);
			}
			return BlockBox.create(
				new Vec3i(
					Math.min(Math.max(sectionBox.getMinX(), otherBox.getMinX()), sectionBox.getMaxX()),
					0,
					Math.min(Math.max(sectionBox.getMinZ(), otherBox.getMinZ()), sectionBox.getMaxZ())
				),
				new Vec3i(
					Math.max(Math.min(sectionBox.getMaxX(), otherBox.getMaxX()), sectionBox.getMinX()),
					0,
					Math.max(Math.min(sectionBox.getMaxZ(), otherBox.getMaxZ()), sectionBox.getMinZ())
				)
			);
		}
	}
}
