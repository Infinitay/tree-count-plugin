package treecount;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("treecount")
public interface TreeCountConfig extends Config
{
	boolean DEBUG = false;

	@ConfigItem(
		keyName = "renderTreeHull",
		name = "Draw Tree Hull",
		description = "Configures whether to draw the hull the hull of the tree with the same color as the text."
	)
	default boolean renderTreeHull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renderTreeTiles",
		name = "(Debug) Show tree tiles",
		description = "Configures whether to show debug info of tree tiles",
		hidden = !DEBUG
	)
	default boolean renderTreeTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renderFacingTree",
		name = "(Debug) Show facing tree",
		description = "Configures whether to show debug info about the tree the player is facing",
		hidden = !DEBUG
	)
	default boolean renderFacingTree()
	{
		return false;
	}
}
