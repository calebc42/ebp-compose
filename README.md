# ebp-compose

Toolkit-neutral renderer models and the Compose Foundation reference renderer
for EBP.

- `:renderer:model` owns renderer profiles, semantic projection, action/editor
  hosts, raw EBP JSON readers, and the typed member descriptors
  (`vocabulary/NodeMember.kt`: `NodeMember`, `NodeMemberSet`, `NodeProperties`,
  `EnumDomain`) with their generated per-node member sets and enum classes
  (`vocabulary/generated/`).
- `:renderer:compose` owns the Foundation/UI implementation of the core node
  set and shared editing controllers. Node presentation is organised as
  components (`ComposeNodeComponent`, one object per node type, installed in a
  `ComposeComponentRegistry`, the Compose UI tier of `:renderer:model`'s
  toolkit-neutral `NodeComponent`/`NodeComponentRegistry`); the eight core
  nodes are `CoreComponents`.
- `:renderer:compose-testing` is a test-support library (add it to a module's
  test or androidTest configuration): `ComposeComponentTestController` mounts
  one node in a Compose test rule and records what it asked of its host
  (`RecordingRenderContext`, `RecordingEditorHost`), `EbpTestSurface` provides
  the renderer's composition locals, and `EbpGoldens` loads the conformance
  corpus from the sibling `ebp` checkout (`ebp.dir`). Its own tests replay
  every `widgets.golden` line through the core tier under Robolectric.
- `tools/gen-node-members.py` generates `CoreNodeMembers.kt` and
  `CoreNodeEnums.kt` from `../ebp/contract.json` (SPEC §17.1 resolution order,
  amendment #192); `CoreNodeMembersDriftTest` re-reads the contract and pins
  the committed output.
- `tools/gen-renderer-extension-vocabulary.py` projects an extension owner's
  manifest into Kotlin and Elisp without registering that extension in EBP;
  with `--members-output`/`--enums-output` it also projects that extension's
  typed members and enums (shared derivation in `tools/ebp_member_derivation.py`).

This repository depends on sibling [`ebp-kmp`](../ebp-kmp).  It must not depend
on Material, Jetpacs policy, or a downstream renderer extension. Renderer reads
go through `NodeProperties` and the generated members: strict (no coercion the
contract omits), total (never throwing), and with defaults applied at the call
site unless the contract projects one.

```sh
python3 test/test_renderer_extension_generator.py
python3 tools/gen-node-members.py --check
./gradlew :renderer:model:testDebugUnitTest \
  :renderer:compose:testDebugUnitTest \
  :renderer:compose-testing:testDebugUnitTest --console=plain
```

The Gradle build needs an Android SDK (`ANDROID_HOME` or `local.properties`).
