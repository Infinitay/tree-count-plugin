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
import net.runelite.api.NullObjectID;
import static net.runelite.api.NullObjectID.NULL_10823;
import static net.runelite.api.NullObjectID.NULL_10835;
import net.runelite.api.ObjectID;
import static net.runelite.api.ObjectID.*;

@Getter
public enum Tree
{
	REGULAR_TREE(TREE, TREE_1277, TREE_1278, TREE_1279, TREE_1280, TREE_40750, TREE_40752),
	OAK_TREE(OAK_TREE_4540, OAK_10820),
	WILLOW_TREE(WILLOW, WILLOW_10829, WILLOW_10831, WILLOW_10833),
	TEAK_TREE(TEAK, TEAK_36686, TEAK_40758),
	MAPLE_TREE(MAPLE_TREE_10832, MAPLE_TREE_36681, MAPLE_TREE_40754),
	ARCTIC_PINE_TREE(ARCTIC_PINE),
	HOLLOW_TREE(ObjectID.HOLLOW_TREE, HOLLOW_TREE_10821, HOLLOW_TREE_10830),
	MAHOGANY_TREE(MAHOGANY, MAHOGANY_36688, MAHOGANY_40760),
	YEW_TREE(YEW, NULL_10823, YEW_36683, YEW_40756),
	MAGIC_TREE(MAGIC_TREE_10834, NULL_10835),
	REDWOOD_TREE(ObjectID.REDWOOD, REDWOOD_29670, NullObjectID.NULL_34633, NullObjectID.NULL_34635, NullObjectID.NULL_34637, NullObjectID.NULL_34639, ObjectID.REDWOOD_TREE_34284, ObjectID.REDWOOD_TREE_34286, ObjectID.REDWOOD_TREE_34288, ObjectID.REDWOOD_TREE_34290);

	private final int[] treeIds;

	Tree(int... treeIds)
	{
		this.treeIds = treeIds;
	}

	private static final Map<Integer, Tree> TREES;

	static
	{
		ImmutableMap.Builder<Integer, Tree> builder = new ImmutableMap.Builder<>();

		for (Tree tree : values())
		{
			for (int treeId : tree.treeIds)
			{
				builder.put(treeId, tree);
			}
		}

		TREES = builder.build();
	}

	static Tree findTree(int objectId)
	{
		return TREES.get(objectId);
	}
}
