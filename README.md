# Tree Counter

A RuneLite plugin to track the number of players chopping a tree.

![](preview.png)

## Known Issues

- Edge case when a player quickly switches the tree they're chopping, it'll fail to register the new tree
    - This is because we utilize animations for tracking, and sometimes the chopping animation doesn't stop even if you
      switch trees.
- If you turn off the plugin and on again, it won't track trees **unless**:
    - A player starts chopping a tree
    - You log back in
    - The trees spawn back in
        - Leaving and coming back to the area
        - The tree respawns
- Rare instances where overlay will remain even when there are no people chopping the tree

## Future Plans

- Track trees and players chopping trees when the plugin is started back up
- When part 2 of the forestry event is release, add the new axe animation ids to #isWoodcutting