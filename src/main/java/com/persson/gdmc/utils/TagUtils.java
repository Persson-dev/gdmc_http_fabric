package com.persson.gdmc.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public class TagUtils {

	public static boolean contains(NbtCompound existingCompound, NbtCompound newCompound) {
		if (existingCompound == newCompound) {
			return true;
		}

		if (existingCompound.isEmpty() != newCompound.isEmpty()) {
			return false;
		}

		for (String newCompoundKey : newCompound.getKeys()) {
			NbtElement existingTag = existingCompound.get(newCompoundKey);
			if (existingTag == null) {
				return false;
			}
			if (!existingTag.equals(newCompound.get(newCompoundKey))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Merge patch with target compound where the key and the tag type is the same, or if the key
	 * does not exist at the source.
	 *
	 * @param targetCompound existing compound tag to merge into
	 * @param patchCompound existing compoundtag to merge from
	 * @return targetComponent
	 */
	public static NbtCompound mergeTags(NbtCompound targetCompound, NbtCompound patchCompound) {
		for (String patchKey : patchCompound.getKeys()) {
			NbtElement patchTag = patchCompound.get(patchKey);
			if (patchTag == null) {
				continue;
			}

			if (!targetCompound.contains(patchKey)) {
				targetCompound.put(patchKey, patchTag.copy());
				continue;
			}

			NbtElement targetTag = targetCompound.get(patchKey);
			if (targetTag == null) {
				continue;
			}

			if (patchTag.getType() == NbtElement.COMPOUND_TYPE && targetTag.getType() == NbtElement.COMPOUND_TYPE) {
				TagUtils.mergeTags(
					targetCompound.getCompound(patchKey),
					patchCompound.getCompound(patchKey)
				);
				continue;
			}

			if (patchTag.getType() == NbtElement.LIST_TYPE && targetTag.getType() == NbtElement.LIST_TYPE) {
				NbtList patchListTag = (NbtList) patchTag;
				NbtList targetListTag = (NbtList) targetTag;
				if (patchListTag.getHeldType() == targetListTag.getHeldType()) {
					targetCompound.put(patchKey, patchTag.copy());
				}
				continue;
			}

			if (patchTag.getType() == targetTag.getType()) {
				targetCompound.put(patchKey, patchTag.copy());
			}
		}

		return targetCompound;
	}

}
