package treecount;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Player;
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
		playerMap.clear();
		playerOrientationMap.clear();
		previousPlane = -1;
		firstRun = true;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (isRegionInWoodcuttingGuild(client.getLocalPlayer().getWorldLocation().getRegionID()))
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
			firstRun = false;
			// Any missing players just in case, although it's not really required. Doesn't hurt since one time operation
			client.getPlayers().forEach(player -> {
				if (!player.equals(client.getLocalPlayer()))
				{
					playerMap.putIfAbsent(player, null);
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
					playerOrientationMap.put(player, currentOrientation);
					final PlayerOrientationChanged playerOrientationChanged = new PlayerOrientationChanged(player, previousOrientation, currentOrientation);
					onPlayerOrientationChanged(playerOrientationChanged);
				}
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		// Event runs first upon login
		GameObject gameObject = event.getGameObject();

		if (isRegionInWoodcuttingGuild(gameObject.getWorldLocation().getRegionID()))
		{
			return;
		}

		Tree tree = Tree.findTree(gameObject.getId());

		if (tree != null)
		{
			log.debug("Tree {} spawned at {}", tree, gameObject.getLocalLocation());
			treeMap.put(gameObject, 0);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();
		if (isRegionInWoodcuttingGuild(gameObject.getWorldLocation().getRegionID()))
		{
			return;
		}
		Tree tree = Tree.findTree(gameObject.getId());
		if (tree != null && !tree.equals(Tree.REGULAR_TREE))
		{
			if (treeMap.containsKey(gameObject))
			{
				treeMap.remove(gameObject);
			}
		}

	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			treeMap.clear();
			playerMap.clear();
			playerOrientationMap.clear();
			firstRun = true;
		}
	}

	@Subscribe
	public void onPlayerSpawned(final PlayerSpawned event)
	{
		// Event runs second upon login
		Player player = event.getPlayer();
		log.debug("Player {} spawned at {}", player.getName(), player.getWorldLocation());

		if (player.equals(client.getLocalPlayer()))
		{
			return;
		}

		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()))
		{
			return;
		}

		if (firstRun)
		{
			playerMap.put(player, null);
			return;
		}

		if (isWoodcutting(player))
		{
			addToTreeFocusedMaps(player);
		}
	}

	@Subscribe
	public void onPlayerDespawned(final PlayerDespawned event)
	{
		Player player = event.getPlayer();

		if (player.equals(client.getLocalPlayer()))
		{
			return;
		}

		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()))
		{
			return;
		}

		if (firstRun)
		{
			playerMap.remove(player);
			playerOrientationMap.remove(player);
			return;
		}

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

			if (player != null && player.equals(client.getLocalPlayer()))
			{
				return;
			}

			// Check combat level to avoid NPE. Not sure why this happens, maybe the Player isn't really a player?
			// The player isn't null, but all the fields are
			if (player.getCombatLevel() != 0 && isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()))
			{
				return;
			}

			if (isWoodcutting(player) && !treeMap.isEmpty())
			{
				addToTreeFocusedMaps(player);
			}
			else if (player.getAnimation() == -1)
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

		if (player.equals(client.getLocalPlayer()))
		{
			return;
		}

		if (isRegionInWoodcuttingGuild(player.getWorldLocation().getRegionID()))
		{
			return;
		}

		removeFromTreeMaps(player); // Remove the previous tracked case
		if (isWoodcutting(player))
		{
			addToTreeFocusedMaps(player);
		}
	}

	private boolean isWoodcutting(Actor actor)
	{
		switch (actor.getAnimation())
		{
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

	boolean addToTreeFocusedMaps(Player player)
	{
		GameObject closestTree = findClosestFacingTree(player);
		if (closestTree == null)
		{
			return false;
		}
		playerMap.put(player, closestTree);
		playerOrientationMap.put(player, player.getOrientation());
		int choppers = treeMap.getOrDefault(closestTree, 0) + 1;
		treeMap.put(closestTree, choppers);
		return true;
	}

	void removeFromTreeMaps(Player player)
	{
		GameObject tree = playerMap.get(player);
		playerMap.remove(player);
		playerOrientationMap.remove(player);
		if (treeMap.containsKey(tree))
		{
			int choppers = treeMap.getOrDefault(tree, 1) - 1;
			treeMap.put(tree, choppers);
		}
	}

	private GameObject findClosestFacingTree(Actor actor)
	{
		// First we filter out all trees whose tile is not in the direction we are facing
		// Orientation: N=1024, E=1536, S=0, W=512, where we would filter tile loc N = y+1, E= x+1, S=y-1, W=x-1
		int orientation = actor.getOrientation();
		Direction[] directions = getDirections(orientation);
		log.debug("Actor: {}, Orientation: {}, Directions: {}", actor.getName(), orientation, Arrays.toString(directions));
		WorldPoint actorLocation = actor.getWorldLocation();

		Optional<Map.Entry<GameObject, Integer>> closestTreeEntry = treeMap.entrySet().stream().filter((entry) ->
			{
				GameObject tree = entry.getKey();
				WorldPoint treeLocation = getSWWorldPoint(tree);
				log.debug("Actor Location: {} Tree Location: {}, Distance: {}", actor.getWorldLocation(), treeLocation, getManhattanDistance(actorLocation, treeLocation));
				boolean result = true;
				for (Direction direction : directions)
				{
					if (direction == null)
					{
						continue;
					}
					switch (direction)
					{
						case NORTH: // North, filter out trees that are not north of us
							result &= treeLocation.getY() >= actorLocation.getY();
							break;
						case EAST: // East, filter out trees that are not east of us
							result &= treeLocation.getX() >= actorLocation.getX();
							break;
						case SOUTH: // South, filter out trees that are not south of us
							result &= treeLocation.getY() < actorLocation.getY();
							break;
						case WEST: // West, filter out trees that are not west of us
							result &= treeLocation.getX() < actorLocation.getX();
							break;
					}
				}
				return result;
			}
		).sorted((entry1, entry2) ->
			{
				// Get the closest tree with relation to our player's location
				GameObject tree1 = entry1.getKey();
				GameObject tree2 = entry2.getKey();
				WorldPoint treeLocation1 = getSWWorldPoint(tree1);
				WorldPoint treeLocation2 = getSWWorldPoint(tree2);
				return getManhattanDistance(actorLocation, treeLocation1) - getManhattanDistance(actorLocation, treeLocation2);
			}
		).findFirst();

		if (closestTreeEntry.isPresent())
		{
			return closestTreeEntry.get().getKey();
		}
		else
		{
			log.debug("No closest tree found");
			return null;
		}
	}

	WorldPoint getSWWorldPoint(GameObject gameObject)
	{
		return WorldPoint.fromScene(client, gameObject.getSceneMinLocation().getX(), gameObject.getSceneMinLocation().getY(), gameObject.getPlane());
	}

	int getManhattanDistance(WorldPoint point1, WorldPoint point2)
	{
		return Math.abs(point1.getX() - point2.getX()) + Math.abs(point1.getY() - point2.getY());
	}

	Direction[] getDirections(int orientation)
	{
		// Get the direction the player is facing
		Angle playerAngle = new Angle(orientation);
		Direction primaryDirection = playerAngle.getNearestDirection();

		// Check to see if the player is facing a definitive direction
		if (playerAngle.getAngle() % 512 != 0)
		{
			Direction secondaryDirection;
			if (primaryDirection == Direction.NORTH || primaryDirection == Direction.SOUTH)
			{
				// Secondary has to be east (1536) or west (512)
				secondaryDirection = Math.abs(playerAngle.getAngle() - 512) < Math.abs(playerAngle.getAngle() - 1536) ? Direction.WEST : Direction.EAST;
			}
			else
			{
				// Secondary has to be north (1024) or south (0 or 2048)
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
			return new Direction[]{primaryDirection, secondaryDirection};
		}
		else
		{
			return new Direction[]{primaryDirection, null};
		}
	}

	boolean isRegionInWoodcuttingGuild(int regionID)
	{
		return regionID == 6198 || regionID == 6454;
	}
}
