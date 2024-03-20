package treecount;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Tree Count",
	description = "Show the number of players chopping a tree",
	tags = {"woodcutting", "wc", "tree", "count", "forestry", "overlay"}
)
public class TreeCountPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TreeCountConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TreeCountOverlay overlay;

	@Getter
	private final Map<GameObject, Integer> treeMap = new HashMap<>();
	private final Map<Player, GameObject> playerMap = new HashMap<>();
	@Getter
	private final Map<GameObject, List<WorldPoint>> treeTileMap = new HashMap<>();
	private final Map<WorldPoint, GameObject> tileTreeMap = new HashMap<>();
	// This map is used to track player orientation changes for only players that are chopping trees
	private final Map<Player, Integer> playerOrientationMap = new ConcurrentHashMap<>();

	private int previousPlane;

	private boolean firstRun;

	@Provides
	TreeCountConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TreeCountConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		treeMap.clear();
		treeTileMap.clear();
		tileTreeMap.clear();
		playerMap.clear();
		playerOrientationMap.clear();
		previousPlane = -1;
		firstRun = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged changedConfig)
	{
		if (changedConfig.getKey().equals("includeSelf"))
		{
			if (Boolean.valueOf(changedConfig.getNewValue()))
			{
				if (isWoodcutting(client.getLocalPlayer()))
				{
					addToTreeFocusedMaps(client.getLocalPlayer());
				}
			}
			else
			{
				removeFromTreeMaps(client.getLocalPlayer());
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (isRegionInWoodcuttingGuild(client.getLocalPlayer().getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}

		// Event runs third (or last) upon login
		int currentPlane = client.getPlane();
		if (previousPlane != currentPlane)
		{
			// Only clear values because sometimes the trees are still there when changing planes (Top of Seer's Bank)
			treeMap.replaceAll((k, v) -> 0);
			previousPlane = currentPlane;
		}

		if (firstRun)
		{
			// Any missing players just in case, although it's not really required. Doesn't hurt since one time operation
			client.getPlayers().forEach(player -> {
				if (!player.equals(client.getLocalPlayer()) || config.includeSelf())
				{
					playerMap.putIfAbsent(player, null);
					// Initialize it to -1 because it will be updated when the PlayerOrientationChanged event is fired
					playerOrientationMap.put(player, -1);
				}
			});
			for (Player player : playerMap.keySet())
			{
				if (isWoodcutting(player) && !treeMap.isEmpty())
				{
					addToTreeFocusedMaps(player);
				}
			}
		}

		// Let's create a PlayerOrientationChanged event for cases when the players shift's orientation while chopping
		if (!playerOrientationMap.isEmpty())
		{
			for (Map.Entry<Player, Integer> playerOrientationEntry : playerOrientationMap.entrySet())
			{
				Player player = playerOrientationEntry.getKey();
				int previousOrientation = playerOrientationEntry.getValue();
				int currentOrientation = player.getOrientation();

				if (currentOrientation != previousOrientation)
				{
					final PlayerOrientationChanged playerOrientationChanged = new PlayerOrientationChanged(player, previousOrientation, currentOrientation);
					onPlayerOrientationChanged(playerOrientationChanged);
				}
			}
		}

		if (firstRun)
		{
			firstRun = false;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		// Event runs first upon login
		GameObject gameObject = event.getGameObject();

		if (isRegionInWoodcuttingGuild(gameObject.getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}

		Tree tree = Tree.findTree(gameObject.getId());

		if (tree != null)
		{
			// log.debug("Tree {} spawned at {}", tree, gameObject.getLocalLocation());
			treeMap.put(gameObject, 0);
			List<WorldPoint> points = getPoints(gameObject);
			treeTileMap.put(gameObject, points);
			points.forEach(point -> tileTreeMap.put(point, gameObject));
		}
	}

	private List<WorldPoint> getPoints(GameObject gameObject)
	{
		WorldPoint minPoint = getSWWorldPoint(gameObject);
		WorldPoint maxPoint = getNEWorldPoint(gameObject);

		if (minPoint.equals(maxPoint))
		{
			return Collections.singletonList(minPoint);
		}

		final int plane = minPoint.getPlane();
		final List<WorldPoint> list = new ArrayList<>();
		for (int x = minPoint.getX(); x <= maxPoint.getX(); x++)
		{
			for (int y = minPoint.getY(); y <= maxPoint.getY(); y++)
			{
				list.add(new WorldPoint(x, y, plane));
			}
		}
		return list;
	}

	@Subscribe
	public void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();
		if (isRegionInWoodcuttingGuild(gameObject.getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}
		Tree tree = Tree.findTree(gameObject.getId());
		if (tree != null && !tree.equals(Tree.REGULAR_TREE))
		{
			treeMap.remove(gameObject);
			List<WorldPoint> points = treeTileMap.remove(gameObject);
			if (points != null)
			{
				points.forEach(tileTreeMap::remove);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			treeMap.clear();
			treeTileMap.clear();
			tileTreeMap.clear();
			playerMap.clear();
			playerOrientationMap.clear();
			firstRun = true;
		}
	}

	@Subscribe
	public void onPlayerSpawned(final PlayerSpawned event)
	{
		// Event runs second upon login

		if (firstRun)
		{
			return;
		}

		Player player = event.getPlayer();
		// log.debug("Player {} spawned at {}", player.getName(), player.getWorldLocation());

		if (player.equals(client.getLocalPlayer()) && !config.includeSelf())
		{
			return;
		}
		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}

		// Sometimes this event is fired after the onAnimationChanged event and as a result, the chopped tree count
		// is incorrectly incremented, so don't add the player to the map if they are already in it
		if (!playerMap.containsKey(player))
		{
			playerMap.put(player, null);
			playerOrientationMap.put(player, player.getOrientation());

			// Most of the time the player won't have an animation, but since we're already checking we should be safe
			if (isWoodcutting(player))
			{
				addToTreeFocusedMaps(player);
			}
		}
	}

	@Subscribe
	public void onPlayerDespawned(final PlayerDespawned event)
	{
		Player player = event.getPlayer();
		// log.debug("Player {} despawned at {}", player.getName(), player.getWorldLocation());

		if (player.equals(client.getLocalPlayer()) && !config.includeSelf())
		{
			return;
		}

		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}

		playerOrientationMap.remove(player);

		removeFromTreeMaps(player);
	}

	@Subscribe
	public void onAnimationChanged(final AnimationChanged event)
	{
		if (firstRun)
		{
			return;
		}

		if (event.getActor() instanceof Player)
		{
			Player player = (Player) event.getActor();
			if (Objects.equals(player, client.getLocalPlayer()) && !config.includeSelf())
			{
				return;
			}

			// Check combat level to avoid NPE. Not sure why this happens, maybe the Player isn't really a player?
			// The player isn't null, but all the fields are
			if (player.getCombatLevel() != 0 && (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()) && !config.enableWCGuild()))
			{
				return;
			}

			if (isWoodcutting(player) && !treeMap.isEmpty())
			{
				addToTreeFocusedMaps(player);
			}
			else if (player.getAnimation() == AnimationID.IDLE)
			{
				removeFromTreeMaps(player);
			}
		}
	}

	@Subscribe
	public void onPlayerOrientationChanged(final PlayerOrientationChanged event)
	{
		// Player orientation map should already consist of players chopping trees but check just in case
		// Also, animation changed should? fire before game tick, therefore non-chopping players should be removed
		// But again, just in case perform the necessary checks
		if (firstRun)
		{
			return;
		}

		Player player = event.getPlayer();
		Direction previousDirection = new Angle(event.getPreviousOrientation()).getNearestDirection();
		Direction currentDirection = new Angle(event.getCurrentOrientation()).getNearestDirection();

		// log.debug("Player {} orientation changed from {} ({}) to {} ({})",
		//	player.getName(),
		//	event.getPreviousOrientation(), previousDirection,
		//	event.getCurrentOrientation(), currentDirection
		// );

		if (player.equals(client.getLocalPlayer()) && !config.includeSelf())
		{
			return;
		}

		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()) && !config.enableWCGuild())
		{
			return;
		}

		playerOrientationMap.put(player, event.getCurrentOrientation());
		removeFromTreeMaps(player); // Remove the previous tracked case
		if (isWoodcutting(player))
		{
			addToTreeFocusedMaps(player);
		}
	}

	boolean isWoodcutting(Actor actor)
	{
		return isWoodcuttingWithRegularAxe(actor) || isWoodcuttingWithFellingAxe(actor);
	}

	private boolean isWoodcuttingWithRegularAxe(Actor actor)
	{
		switch (actor.getAnimation())
		{
			// Regular axe animation IDs
			case AnimationID.WOODCUTTING_BRONZE:
			case AnimationID.WOODCUTTING_IRON:
			case AnimationID.WOODCUTTING_STEEL:
			case AnimationID.WOODCUTTING_BLACK:
			case AnimationID.WOODCUTTING_MITHRIL:
			case AnimationID.WOODCUTTING_ADAMANT:
			case AnimationID.WOODCUTTING_RUNE:
			case AnimationID.WOODCUTTING_GILDED:
			case AnimationID.WOODCUTTING_DRAGON:
			case AnimationID.WOODCUTTING_DRAGON_OR:
			case AnimationID.WOODCUTTING_INFERNAL:
			case AnimationID.WOODCUTTING_3A_AXE:
			case AnimationID.WOODCUTTING_CRYSTAL:
			case AnimationID.WOODCUTTING_TRAILBLAZER:
				return true;
			default:
				return false;
		}
	}

	private boolean isWoodcuttingWithFellingAxe(Actor actor)
	{
		switch (actor.getAnimation())
		{
			// Felling axes (Forestry Part 2) animation IDs
			case AnimationID.WOODCUTTING_2H_BRONZE:
			case AnimationID.WOODCUTTING_2H_IRON:
			case AnimationID.WOODCUTTING_2H_STEEL:
			case AnimationID.WOODCUTTING_2H_BLACK:
			case AnimationID.WOODCUTTING_2H_MITHRIL:
			case AnimationID.WOODCUTTING_2H_ADAMANT:
			case AnimationID.WOODCUTTING_2H_RUNE:
			case AnimationID.WOODCUTTING_2H_DRAGON:
			case AnimationID.WOODCUTTING_2H_CRYSTAL:
			case AnimationID.WOODCUTTING_2H_3A:
				return true;
			default:
				return false;
		}
	}

	void addToTreeFocusedMaps(Player player)
	{
		GameObject closestTree = findClosestFacingTree(player);

		if (player != client.getLocalPlayer() || config.includeSelf())
		{
			Direction direction = new Angle(player.getOrientation()).getNearestDirection();
			// log.debug("Actor: {}, Direction: {}, Closest Facing Tree: {}, Count: {}", player.getName(), direction,
			//	closestTree != null ? closestTree.getWorldLocation() : "NULL",
			//	closestTree != null ? treeMap.get(closestTree) : -1);
		}

		if (closestTree == null)
		{
			// Hotfix for #24 where players chopping with a felling axe could be chopping a tree that is not facing them
			// This will treat the only adjacent tree as the tree the player is chopping
			if (isWoodcutting(player))
			{
				List<GameObject> adjacentTrees = getAdjacentTrees(player, false);
				if (adjacentTrees.size() == 1)
				{
					playerMap.put(player, adjacentTrees.get(0));
					treeMap.merge(adjacentTrees.get(0), 1, Integer::sum);
				}
			}
			return;
		}

		playerMap.put(player, closestTree);
		treeMap.merge(closestTree, 1, Integer::sum);
	}

	void removeFromTreeMaps(Player player)
	{
		GameObject tree = playerMap.get(player);
		playerMap.remove(player);
		treeMap.computeIfPresent(tree, (unused, value) -> {
			// log.debug("Removing player {} from tree {}. {} -> {}", player.getName(), tree.getWorldLocation(), value, Math.max(0, value - 1));
			return Math.max(0, value - 1);
		});
	}

	GameObject findClosestFacingTree(Actor actor)
	{
		WorldPoint actorLocation = actor.getWorldLocation();
		Direction direction = new Angle(actor.getOrientation()).getNearestDirection();
		WorldPoint facingPoint = neighborPoint(actorLocation, direction);
		return tileTreeMap.get(facingPoint);
	}

	List<GameObject> getAdjacentTrees(Actor actor, boolean ignoreNonForestryTrees)
	{
		WorldPoint actorLocation = actor.getWorldLocation();
		List<GameObject> adjacentTrees = new ArrayList<>();
		for (Direction direction : Direction.values())
		{
			WorldPoint neighborPoint = neighborPoint(actorLocation, direction);
			GameObject tree = tileTreeMap.get(neighborPoint);
			if (tree != null && (!ignoreNonForestryTrees || (ignoreNonForestryTrees && Tree.findForestryTree(tree.getId()) != null)))
			{
				adjacentTrees.add(tree);
			}
		}
		return adjacentTrees;
	}

	WorldPoint neighborPoint(WorldPoint point, Direction direction)
	{
		switch (direction)
		{
			case NORTH:
				return point.dy(1);
			case SOUTH:
				return point.dy(-1);
			case EAST:
				return point.dx(1);
			case WEST:
				return point.dx(-1);
			default:
				throw new IllegalStateException();
		}
	}

	private WorldPoint getSWWorldPoint(GameObject gameObject)
	{
		return getWorldPoint(gameObject, GameObject::getSceneMinLocation);
	}

	private WorldPoint getNEWorldPoint(GameObject gameObject)
	{
		return getWorldPoint(gameObject, GameObject::getSceneMaxLocation);
	}

	private WorldPoint getWorldPoint(GameObject gameObject, Function<GameObject, Point> pointFunction)
	{
		Point point = pointFunction.apply(gameObject);
		return WorldPoint.fromScene(client, point.getX(), point.getY(), gameObject.getPlane());
	}

	boolean isRegionInWoodcuttingGuild(int regionID)
	{
		return regionID == 6198 || regionID == 6454;
	}
}
