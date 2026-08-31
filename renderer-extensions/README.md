# Renderer-extension projection

This repository owns the generic projection tool, not any downstream renderer
vocabulary. Each extension repository owns its JSON manifest, accepted
semantic witnesses, Kotlin receiver projection, and Elisp authoring
projection. EBP transports extension identifiers as opaque negotiated data.

The current workspace owners are independent:

| Extension | Owner | Targets |
|---|---|---|
| `glasspane.material3` | Glasspane's Material library and catalog | app, dialog |
| `jetpacs.components` | Jetpacs' Material-free component library and catalog | app only |

An extension node is usable only when its node type and owning extension
are both advertised for the target, and the app declares the extension in
`:requires-extensions`. Namespaces do not imply support. EBP itself treats the
identifiers as opaque negotiated data.

Every invocation supplies its owner manifest and explicit destinations:

```sh
python3 tools/gen-renderer-extension-vocabulary.py \
  /path/to/owner/renderer-extensions/example.json \
  --kotlin-output /path/to/owner/ExampleVocabulary.kt \
  --kotlin-package com.example.renderer \
  --elisp-output /path/to/owner/example-vocabulary.el \
  --check
```

Omit `--check` only when intentionally regenerating after changing the
manifest. Never edit either generated vocabulary file by hand.
