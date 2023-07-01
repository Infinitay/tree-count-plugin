package treecount;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

	private int previousPlane;

	private boolean firstRun;

	@Provides
	TreeCountConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TreeCountConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		treeMap.clear();
		playerMap.clear();
		previousPlane = -1;
		firstRun = true;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// Event runs third (or last) upon login
		int currentPlane = client.getPlane();
		if (previousPlane != currentPlane)
		{
			// Only clear values because sometimes the trees are still there when changing planes (Top of Seer's Bank)
			treeMap.replaceAll((k, v) -> 0);
			playerMap.clear();
			previousPlane = currentPlane;
		}

		if (firstRun)
		{
			firstRun = false;
			// Any missing players just in case, although it's not really required. Doesn't hurt since one time operation
			client.getPlayers().forEach(player -> playerMap.putIfAbsent(player, null));
			for (Player player : playerMap.keySet())
			{
				if (isWoodcutting(player) && !treeMap.isEmpty())
				{
					// Now we have to find the closest tree to the player that we are facing
					// Orientation: N=1024, E=1536, S=0, W=512, where we would filter tile loc N = y+1, E= x+1, S=y-1, W=x-1
					GameObject closestTree = findClosestFacingTree(player);
					if (closestTree == null)
					{
						return;
					}
					playerMap.put(player, closestTree);
					int choppers = treeMap.getOrDefault(closestTree, 0) + 1;
					treeMap.put(closestTree, choppers);
				}
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		// Event runs first upon login
		GameObject gameObject = event.getGameObject();
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
		final GameObject object = event.getGameObject();

		Tree tree = Tree.findTree(object.getId());
		if (tree != null)
		{
			if (treeMap.containsKey(object))
			{
				treeMap.remove(object);
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
			firstRun = true;
		}
	}

	@Subscribe
	public void onPlayerSpawned(final PlayerSpawned event)
	{
		// Event runs second upon login
		Player player = event.getPlayer();
		log.debug("Player {} spawned at {}", player.getName(), player.getWorldLocation());

		if (firstRun)
		{
			playerMap.put(player, null);
			return;
		}

		if (isWoodcutting(player))
		{
			// Now we have to find the closest tree to the player that we are facing
			// Orientation: N=1024, E=1536, S=0, W=512, where we would filter tile loc N = y+1, E= x+1, S=y-1, W=x-1
			GameObject closestTree = findClosestFacingTree(player);
			if (closestTree == null)
			{
				return;
			}
			playerMap.put(player, closestTree);
			int choppers = treeMap.getOrDefault(closestTree, 0) + 1;
			treeMap.put(closestTree, choppers);
		}
	}

	@Subscribe
	public void onPlayerDespawned(final PlayerDespawned event)
	{
		Player player = event.getPlayer();

		if (firstRun)
		{
			playerMap.remove(player);
			return;
		}

		GameObject tree = playerMap.get(player);
		if (playerMap.containsKey(player))
		{
			playerMap.remove(player);
			if (treeMap.containsKey(tree))
			{
				int choppers = treeMap.getOrDefault(tree, 1) - 1;
				treeMap.put(tree, choppers);
			}
		}
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
			if (isWoodcutting(player) && !treeMap.isEmpty())
			{
				// Now we have to find the closest tree to the player that we are facing
				// Orientation: N=1024, E=1536, S=0, W=512, where we would filter tile loc N = y+1, E= x+1, S=y-1, W=x-1
				GameObject closestTree = findClosestFacingTree(player);
				if (closestTree == null)
				{
					return;
				}
				playerMap.put(player, closestTree);
				int choppers = treeMap.getOrDefault(closestTree, 0) + 1;
				treeMap.put(closestTree, choppers);
			}
			else if (player.getAnimation() == -1)
			{
				if (!playerMap.isEmpty() && playerMap.containsKey(player))
				{
					GameObject tree = playerMap.get(player);
					playerMap.remove(player);
					if (treeMap.containsKey(tree))
					{
						int choppers = treeMap.getOrDefault(tree, 1) - 1;
						treeMap.put(tree, choppers);
					}
				}
			}
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

	private GameObject findClosestFacingTree(Actor actor)
	{
		// First we filter out all trees whose tile is not in the direction we are facing
		// Orientation: N=1024, E=1536, S=0, W=512, where we would filter tile loc N = y+1, E= x+1, S=y-1, W=x-1
		int orientation = actor.getCurrentOrientation();
		WorldPoint actorLocation = actor.getWorldLocation();
		Optional<Map.Entry<GameObject, Integer>> closestTreeEntry = treeMap.entrySet().stream().filter((entry) ->
			{
				GameObject tree = entry.getKey();
				WorldPoint treeLocation = tree.getWorldLocation();
				switch (new Angle(orientation).getNearestDirection())
				{
					case NORTH: // North, filter out trees that are not north of us
						return treeLocation.getY() > actorLocation.getY();
					case EAST: // East, filter out trees that are not east of us
						return treeLocation.getX() > actorLocation.getX();
					case SOUTH: // South, filter out trees that are not south of us
						return treeLocation.getY() < actorLocation.getY();
					case WEST: // West, filter out trees that are not west of us
						return treeLocation.getX() < actorLocation.getX();
				}
				log.debug("Orientation {} not found", orientation);
				return false;
			}
		).sorted((entry1, entry2) ->
			{
				// Get closest tree with relation to our player's location
				GameObject tree1 = entry1.getKey();
				GameObject tree2 = entry2.getKey();
				WorldPoint treeLocation1 = tree1.getWorldLocation();
				WorldPoint treeLocation2 = tree2.getWorldLocation();
				return actorLocation.distanceTo(treeLocation1) - actorLocation.distanceTo(treeLocation2);
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
}
