package treecount;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("treecount")
public interface TreeCountConfig extends Config
{
	boolean DEBUG = true;

	@ConfigItem(
		keyName = "renderTreeHull",
		name = "Draw Tree Hull",
		description = "Configures whether to draw the hull of the tree with the same color as the text."
	)
	default boolean renderTreeHull()
	{
		return false;
	}

    @ConfigItem(
            keyName = "dynamicColors",
            name = "Dynamic Color",
            description = "Use dynamic colors for the chopper count. If disabled, Text Color will be used"
    )
    default boolean dynamicColors()
    {
        return true;
    }

    @ConfigItem(
            keyName = "textColor",
            name = "Text Color",
            description = "The color of the tree count text."
    )
    default Color textColor()
    {
        return Color.YELLOW;
    }

	@ConfigItem(
		keyName = "renderTreeTiles",
		name = "(Debug) Show Tree Tiles",
		description = "Configures whether to show debug info of tree tiles",
		hidden = !DEBUG
	)
	default boolean renderTreeTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renderFacingTree",
		name = "(Debug) Show Facing Tree",
		description = "Configures whether to show debug info about the tree the player is facing",
		hidden = !DEBUG
	)
	default boolean renderFacingTree()
	{
		return false;
	}

	@ConfigItem(
		keyName = "includeSelf",
		name = "(Debug) Include Self",
		description = "Configures whether to help with debug info by including the player in the tree count",
		hidden = !DEBUG
	)
	default boolean includeSelf()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableWCGuild",
		name = "(Debug) Enable Woodcutting Guild",
		description = "Configures whether to help with debug info by allowing the woodcutting guild to be used",
		hidden = !DEBUG
	)
	default boolean enableWCGuild()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renderPlayerOrientation",
		name = "(Debug) Show Player Orientation",
		description = "Configures whether to show debug info about the tree hull",
		hidden = !DEBUG
	)
	default boolean renderPlayerOrientation()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renderExpectedChoppers",
		name = "(Debug) Show Expected Choppers",
		description = "Configures whether to show debug info about the expected number of people chopping a tree",
		hidden = !DEBUG
	)
	default boolean renderExpectedChoppers()
	{
		return false;
	}
}
