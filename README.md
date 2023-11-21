# Tree Counter

A RuneLite plugin to track the number of players **other than yourself** chopping a tree.

![](preview.png)

## Known Issues

- Edgecase where number of people chopping a tree is incorrect and missing people [(#24)](https://github.com/Infinitay/tree-count-plugin/pull/6)
    - This occurs when the player is chopping a tree but is not facing the tree due to an animation stall bug
    - More common with felling axes and potentially a rare occurrence with regular axes
    - I made a temporary hotfix for this issue [#27](https://github.com/Infinitay/tree-count-plugin/pull/27), but it is not a permanent solution until Jagex fixes animations when interacting
      - A player is said to be chopping a tree if it is facing a tree and has the chopping animation
      - If there is no facing tree found but the player is chopping, then the player is said to be chopping the tree they are standing right beside a tree IF AND ONLY IF they are standing by one tree.
- ~~Rare instances where overlay will remain even when there are no people chopping the tree~~
    - Potentially resolved via [#6](https://github.com/Infinitay/tree-count-plugin/pull/6)
    - If you encounter this issue, please open an issue with the tree's location and a description of what happened
