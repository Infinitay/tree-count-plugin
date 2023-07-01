package treecount;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.Player;

@Value
@EqualsAndHashCode
public class PlayerOrientationChanged
{
	Player player;
	int previousOrientation;
	int currentOrientation;
}
