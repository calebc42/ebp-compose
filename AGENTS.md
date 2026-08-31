# ebp-compose implementation guide

- Keep `:renderer:model` free of Compose and design-system dependencies.
- Keep `:renderer:compose` on Compose Foundation/UI; Material and product
  styling belong to downstream extension repositories.
- Extension dispatch is positive and exact: an advertised extension ID and
  node type must agree, with one owner per node.
- Do not add Jetpacs navigation, Room state, app identifiers, or downstream
  vocabulary to this repository.
- Preserve semantic, text-input, editor, accessibility, and registry tests when
  changing renderer behavior.

Run the commands in `README.md` before handing off changes.
