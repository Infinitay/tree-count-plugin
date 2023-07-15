package treecount;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
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
		if (plugin.isRegionInWoodcuttingGuild(client.getLocalPlayer().getWorldLocation().getRegionID()))
		{
			return null;
		}

		renderDebugOverlay(graphics);

		plugin.getTreeMap().forEach((gameObject, choppers) ->
		{
			if (choppers <= 0 || Tree.findForestryTree(gameObject.getId()) == null)
			{
				return;
			}

			String text = String.valueOf(choppers);
			Point point = Perspective.getCanvasTextLocation(client, graphics, gameObject.getLocalLocation(), text, 0);
			if (point == null)
			{
				return;
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
			OverlayUtil.renderTextLocation(graphics, point, text, color);
		});

		return null;
	}

	private static final Random random = ThreadLocalRandom.current();

	private static final Map<GameObject, Color> colorMap = new WeakHashMap<>();

	private void renderDebugOverlay(Graphics2D graphics)
	{
		if (config.renderFacingTree())
		{
			renderFacingTree(graphics);
		}

		if (config.renderTreeTiles())
		{
			renderTreeTiles(graphics);
		}

	}

	private void renderFacingTree(Graphics2D graphics)
	{
		if (client.getLocalPlayer() != null)
		{
			GameObject tree = plugin.findClosestFacingTree(client.getLocalPlayer());
			if (tree != null)
			{
				OverlayUtil.renderTileOverlay(graphics, tree, "", Color.GREEN);
			}
		}
	}

	private void renderTreeTiles(Graphics2D graphics)
	{
		plugin.getTreeTileMap().forEach((tree, tiles) ->
			{
				final Color color = colorMap.computeIfAbsent(tree, (unused) -> Color.getHSBColor(random.nextFloat(), 1f, 1f));
				tiles.forEach(worldPoint ->
					{
						LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
						if (localPoint == null)
						{
							return;
						}
						Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
						if (poly == null)
						{
							return;
						}
						OverlayUtil.renderPolygon(graphics, poly, color);
					}
				);
			}
		);
	}
}
