/*
 * Copyright (c) 2018, Mantautas Jurksa <https://github.com/Juzzed>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package treecount;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.ObjectID;
import static net.runelite.api.ObjectID.*;

@Getter
public enum Tree
{
	// Trees that provide a hidden boost to woodcutting when chopping in a group
	OAK_TREE(true, 9734, 10820, 37969, 42395, 42831),
	WILLOW_TREE(true, 10819, 10829, 10831, 10833),
	TEAK_TREE(true, 9036, 15062, 36686, 40758),
	MAPLE_TREE(true, 4674, 10832, 36681, 40754),
	ARCTIC_PINE_TREE(true, 3037),
	HOLLOW_TREE(true, HOLLOW_TREE_10821, HOLLOW_TREE_10830),
	MAHOGANY_TREE(true, 9034, 40760),
	YEW_TREE(true, 10822, 36683, 40756, 42391),
	MAGIC_TREE(true, MAGIC_TREE_10834, MAGIC_TREE_36685), // 36685 seems deprecated or placeholder for now, 0 locations as of July 2023
	REDWOOD_TREE(true, 29668, 29670, 34284, 34286, 34288, 34290),

	// Trees that do not provide a hidden boost to woodcutting when chopping in a group and other choppable trees
	REGULAR_TREE(false, ObjectID.TREE, TREE_1277, TREE_1278, TREE_1279, TREE_1280, TREE_1330, TREE_1331, TREE_1332, TREE_2409, TREE_3879,
		TREE_3881, TREE_3882, TREE_3883, TREE_9730, TREE_9731, TREE_9732, TREE_9733, TREE_14308, TREE_14309, TREE_16264, TREE_16265, TREE_36672, TREE_36674,
		TREE_36677, TREE_36679, TREE_37965, TREE_37967, TREE_37971, TREE_37973, TREE_40750, TREE_40752, TREE_42393, TREE_42832),
	DEAD_TREE(false, ObjectID.DEAD_TREE, DEAD_TREE_1283, DEAD_TREE_1284, DEAD_TREE_1285, DEAD_TREE_1286, DEAD_TREE_1289, DEAD_TREE_1290, DEAD_TREE_1291, DEAD_TREE_1365, DEAD_TREE_1383,
		DEAD_TREE_1384, DEAD_TREE_5902, DEAD_TREE_5903, DEAD_TREE_5904, DEAD_TREE_42907),
	DRAMEN_TREE(false, ObjectID.DRAMEN_TREE),
	EVERGREEN_TREE(false, 1318, 1319, 2091, 2092, 27060, 40932, 40933),
	ACHEY_TREE(false, ObjectID.ACHEY_TREE),
	JUNGLE_TREE(false, ObjectID.JUNGLE_TREE, JUNGLE_TREE_2889, JUNGLE_TREE_2890, JUNGLE_TREE_4818, JUNGLE_TREE_4820),
	DYING_TREE(false, ObjectID.DYING_TREE),
	DREAM_TREE(false, ObjectID.DREAM_TREE),
	WINDSWEPT_TREE(false, WINDSWEPT_TREE_18137),
	MATURE_JUNIPER_TREE(false, ObjectID.MATURE_JUNIPER_TREE),
	BURNT_TREE(false, ObjectID.BURNT_TREE, BURNT_TREE_30854),
	BLISTERWOOD_TREE(false, ObjectID.BLISTERWOOD_TREE),
	RISING_ROOTS(false, TREE_ROOTS, 47483);


	private final boolean providesForestryBoost;
	private final int[] treeIds;

	Tree(boolean providesForestryBoost, int... treeIds)
	{
		this.providesForestryBoost = providesForestryBoost;
		this.treeIds = treeIds;
	}

	private static final Map<Integer, Tree> ALL_TREES;
	private static final Map<Integer, Tree> FORESTRY_TREES;

	static
	{
		ImmutableMap.Builder<Integer, Tree> allTreesBuilder = new ImmutableMap.Builder<>();
		ImmutableMap.Builder<Integer, Tree> forestryTreesBuilder = new ImmutableMap.Builder<>();

		for (Tree tree : values())
		{
			for (int treeId : tree.treeIds)
			{
				allTreesBuilder.put(treeId, tree);
				if (tree.providesForestryBoost)
				{
					forestryTreesBuilder.put(treeId, tree);
				}
			}
		}

		ALL_TREES = allTreesBuilder.build();
		FORESTRY_TREES = forestryTreesBuilder.build();
	}

	/**
	 * Finds the tree that matches the given object ID
	 *
	 * @param objectId
	 * @return tree that matches the given object ID, or null if no match
	 */
	static Tree findTree(int objectId)
	{
		return ALL_TREES.get(objectId);
	}

	/**
	 * Finds the tree that is able to receive a forestry hidden boost that matches the given object ID
	 *
	 * @param objectId
	 * @return forestry-boost-capable tree that matches the given object ID, or null if no match
	 */
	static Tree findForestryTree(int objectId)
	{
		return FORESTRY_TREES.get(objectId);
	}
}
