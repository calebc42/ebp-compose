# ebp-compose

Toolkit-neutral renderer models and the Compose Foundation reference renderer
for EBP.

- `:renderer:model` owns renderer profiles, semantic projection, action/editor
  hosts, and raw EBP JSON readers.
- `:renderer:compose` owns the Foundation/UI implementation of the core node
  set and shared editing controllers.
- `tools/gen-renderer-extension-vocabulary.py` projects an extension owner's
  manifest into Kotlin and Elisp without registering that extension in EBP.

This repository depends on sibling [`ebp-kmp`](../ebp-kmp).  It must not depend
on Material, Jetpacs policy, or a downstream renderer extension.

```sh
python3 test/test_renderer_extension_generator.py
./gradlew :renderer:model:testDebugUnitTest \
  :renderer:compose:testDebugUnitTest --console=plain
```
