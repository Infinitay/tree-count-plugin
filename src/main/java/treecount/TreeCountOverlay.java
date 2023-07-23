package treecount;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class TreeCountOverlay extends Overlay
{
	public static final Color BLANK_COLOR = new Color(0, true);
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
			if (choppers == null || choppers <= 0 || Tree.findForestryTree(gameObject.getId()) == null)
			{
				return;
			}

			final Color colorForChoppers = getColorForChoppers(choppers);
			if (config.renderTreeHull())
			{
				if (choppers < 10)
				{
					drawOutline(graphics, gameObject, colorForChoppers, 0x50, 1f);
				}
				else
				{
					drawOutline(graphics, gameObject, colorForChoppers, 0xB0, 2f);
				}
			}

			final String text = String.valueOf(choppers);
			Optional.ofNullable(Perspective.getCanvasTextLocation(client, graphics, gameObject.getLocalLocation(), text, 0))
				.ifPresent(point -> OverlayUtil.renderTextLocation(graphics, point, text, colorForChoppers));
		});

		return null;
	}

	private static void drawOutline(Graphics2D graphics,
									GameObject gameObject,
									Color colorForChoppers,
									int alpha,
									float strokeWidth)
	{
		Color outlineColor = ColorUtil.colorWithAlpha(colorForChoppers, alpha);
		Stroke stroke = new BasicStroke(strokeWidth);
		OverlayUtil.renderPolygon(graphics, gameObject.getConvexHull(), outlineColor, BLANK_COLOR, stroke);
	}

	private static Color getColorForChoppers(int choppers)
	{
		final float percent = Math.min(1f, choppers / 10f);
		final float hue1 = rgbToHsbArray(Color.RED)[0];
		final float hue2 = rgbToHsbArray(Color.GREEN)[0];
		final float lerpedHue = hue1 + (hue2 - hue1) * percent;
		return Color.getHSBColor(lerpedHue, 1f, 1f);
	}

	private static float[] rgbToHsbArray(Color color)
	{
		return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
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
