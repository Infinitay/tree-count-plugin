package treecount;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
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
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.geometry.SimplePolygon;
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

			centroidOfObjectHull(gameObject)
				.ifPresent(point -> drawTextCentered(graphics, point, String.valueOf(choppers), colorForChoppers));
		});

		return null;
	}

	private static void drawTextCentered(Graphics2D graphics, Point point, String text, Color color)
	{
		final FontMetrics metrics = graphics.getFontMetrics(graphics.getFont());
		Rectangle2D bounds = metrics.getStringBounds(text, graphics);
		int x = point.getX() - (int) Math.round(bounds.getWidth()) / 2;
		int y = point.getY() + (int) Math.round(bounds.getHeight()) / 2; // y coordinate is the baseline, not top
		OverlayUtil.renderTextLocation(graphics, new Point(x, y), text, color);
	}

	private static Optional<Point> centroidOfObjectHull(GameObject gameObject)
	{
		final Shape convexHull = gameObject.getConvexHull();
		if (!(convexHull instanceof SimplePolygon))
		{
			return Optional.empty();
		}

		return centroidOfPolygon((SimplePolygon) convexHull);
	}

	// https://en.wikipedia.org/wiki/Centroid#Of_a_polygon
	private static Optional<Point> centroidOfPolygon(SimplePolygon poly)
	{
		long xSum = 0, ySum = 0, areaSum = 0;
		for (int i = 0; i < poly.size(); i++)
		{
			final long currX = poly.getX(i);
			final long currY = poly.getY(i);

			// wrap around to the first point if we're on the last index
			final int nextI = i == poly.size() - 1 ? 0 : i + 1;
			final long nextX = poly.getX(nextI);
			final long nextY = poly.getY(nextI);

			final long areaSumComponent = (currX * nextY - nextX * currY);
			areaSum += areaSumComponent;
			xSum += (currX + nextX) * areaSumComponent;
			ySum += (currY + nextY) * areaSumComponent;
		}

		final long divisor = areaSum * 3;
		final long centroidX = xSum / divisor;
		final long centroidY = ySum / divisor;
		return Optional.of(new Point((int) centroidX, (int) centroidY));
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
