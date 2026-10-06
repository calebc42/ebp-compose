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
- `renderer/model/.../vocabulary/generated/` is an output of
  `tools/gen-node-members.py` over `../ebp/contract.json`; regenerate it, never
  hand-edit it. A member the contract does not type is a generator error, not
  an `Any` fallback.
- Read node members through `NodeProperties` and the generated member sets, not
  ad hoc `JsonObject` lookups; never coerce a JSON string into a number or
  boolean (SPEC §17.1).

Run the commands in `README.md` before handing off changes.
