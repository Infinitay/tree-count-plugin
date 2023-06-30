package treecount;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class TreeCountOverlay extends Overlay
{
	private final TreeCountPlugin plugin;
	private final TreeCountConfig config;
	private final Client client;

	@Inject
	private TreeCountOverlay(TreeCountPlugin plugin, TreeCountConfig config, Client client)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}


	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (Map.Entry<GameObject, Integer> treeEntry : plugin.getTreeMap().entrySet())
		{
			int choppers = treeEntry.getValue();
			if (choppers > 0)
			{
				Point point = Perspective.getCanvasTextLocation(client, graphics, treeEntry.getKey().getLocalLocation(), String.valueOf(choppers), 0);
				if (point == null)
				{
					return null;
				}
				Color color;
				if (choppers >= 10)
				{
					color = Color.GREEN;
				}
				else if (choppers >= 7)
				{
					color = Color.YELLOW;
				}
				else if (choppers >= 4)
				{
					color = Color.ORANGE;
				}
				else
				{
					color = Color.RED;
				}
				OverlayUtil.renderTextLocation(graphics, point, String.valueOf(choppers), color);
			}
		}
		return null;
	}
}
