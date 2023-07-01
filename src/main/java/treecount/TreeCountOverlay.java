package treecount;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
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
		renderDebugOverlay(graphics);

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

	private void renderDebugOverlay(Graphics2D graphics)
	{
		if (client.getLocalPlayer() != null)
		{
			renderOrientation(graphics);
//			renderTranslatedTile(graphics);
//			renderTreesSWTile(graphics);
		}
		return;
	}

	private void renderOrientation(Graphics2D graphics)
	{
		// Get the direction the player is facing
		Angle playerAngle = new Angle(client.getLocalPlayer().getOrientation());
		Direction direction = playerAngle.getNearestDirection();
		String text = String.valueOf(playerAngle.getAngle()) + " (" + direction.toString() + ")";
		Direction secondaryDirection = null;
		if (playerAngle.getAngle() % 512 != 0)
		{
			if (direction == Direction.NORTH || direction == Direction.SOUTH)
			{
				// Secondary has to be east (1536) or west (512)
				secondaryDirection = Math.abs(playerAngle.getAngle() - 512) < Math.abs(playerAngle.getAngle() - 1536) ? Direction.WEST : Direction.EAST;
			}
			else
			{
				int northCheck = Math.abs(playerAngle.getAngle() - 1024);
				int southCheck1 = Math.abs(playerAngle.getAngle() - 0);
				int southCheck2 = Math.abs(playerAngle.getAngle() - 2048);
				if (northCheck < southCheck1 && northCheck < southCheck2)
				{
					secondaryDirection = Direction.NORTH;
				}
				else if (southCheck1 < southCheck2)
				{
					secondaryDirection = Direction.SOUTH;
				}
				else
				{
					secondaryDirection = Direction.SOUTH;
				}
			}
			OverlayUtil.renderActorOverlay(graphics, client.getLocalPlayer(), text + " | " + secondaryDirection, Color.GREEN);
		}
		else
		{
			OverlayUtil.renderActorOverlay(graphics, client.getLocalPlayer(), text, Color.GREEN);
		}
	}

	private void renderTranslatedTile(Graphics2D graphics)
	{

		Direction[] directions = plugin.getDirections(client.getLocalPlayer().getCurrentOrientation());
		Polygon actorLocation = client.getLocalPlayer().getCanvasTilePoly();
		Polygon changedActorLocation = actorLocation;
		for (Direction _direction : directions)
		{
			if (_direction == null)
			{
				continue;
			}
			// If the direction is north or south, change the y coordinate
			switch (_direction)
			{
				case NORTH:
					changedActorLocation.translate(0 * actorLocation.getBounds().width, -1 * actorLocation.getBounds().height);
					break;
				case EAST:
					changedActorLocation.translate(1 * actorLocation.getBounds().width, 0 * actorLocation.getBounds().height);
					break;
				case SOUTH:
					changedActorLocation.translate(0 * actorLocation.getBounds().width, 1 * actorLocation.getBounds().height);
					break;
				case WEST:
					changedActorLocation.translate(-1 * actorLocation.getBounds().width, 0 * actorLocation.getBounds().height);
					break;
			}
		}
		OverlayUtil.renderPolygon(graphics, changedActorLocation, Color.RED);
	}

	private void renderTreesSWTile(Graphics2D graphics)
	{
		for (Map.Entry<GameObject, Integer> treeEntry : plugin.getTreeMap().entrySet())
		{
			LocalPoint swLocalPoint = LocalPoint.fromWorld(client, plugin.getSWWorldPoint(treeEntry.getKey()));
			if (swLocalPoint != null)
			{
				int distance = plugin.getManhattanDistance(plugin.getSWWorldPoint(treeEntry.getKey()), client.getLocalPlayer().getWorldLocation());
				Polygon swTilePolygon = Perspective.getCanvasTilePoly(client, swLocalPoint);
				if (swTilePolygon != null)
				{
					OverlayUtil.renderPolygon(graphics, swTilePolygon, Color.WHITE);
					if (treeEntry.getKey().getCanvasLocation() != null)
					{
						OverlayUtil.renderTextLocation(graphics, treeEntry.getKey().getCanvasLocation(), String.valueOf(distance), Color.WHITE);
					}
				}
			}
		}
	}
}
