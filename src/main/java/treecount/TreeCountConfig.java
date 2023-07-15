package treecount;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("treecount")
public interface TreeCountConfig extends Config
{
	@ConfigItem(
		keyName = "renderTreeTiles",
		name = "(Debug) Show tree tiles",
		description = "Configures whether to show debug info of tree tiles"
	)
	default boolean renderTreeTiles()
	{
		return false;
	}
	@ConfigItem(
		keyName = "renderFacingTree",
		name = "(Debug) Show facing tree",
		description = "Configures whether to show debug info about the tree the player is facing"
	)
	default boolean renderFacingTree()
	{
		return false;
	}
}
