# Tree Counter

A RuneLite plugin to track the number of players **other than yourself** chopping a tree.

![](preview.png)

## Known Issues

- Whenever there are players chopping tree roots from the event, it will incorrectly increment the nearby tree
- ~~Rare instances where overlay will remain even when there are no people chopping the tree~~
    - Potentially resolved via [#6](https://github.com/Infinitay/tree-count-plugin/pull/6)
    - If you encounter this issue, please open an issue with the tree's location and a description of what happened

## Future Plans

- When part 2 of the forestry event is release, add the new axe animation ids to #isWoodcutting