# Renderer-extension projection

This repository owns the generic projection tool, not any downstream renderer
vocabulary. Each extension repository owns its JSON manifest, accepted
semantic witnesses, Kotlin receiver projection, and Elisp authoring
projection. EBP transports extension identifiers as opaque negotiated data.

The current workspace owners are independent:

| Extension | Owner | Targets |
|---|---|---|
| `jetpacs.material3` | Jetpacs' Material library and catalog | app, dialog |
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

## Manifest formats

Format 1 is the original required/optional member-name projection. Its inputs
and generated Kotlin and Elisp output remain unchanged.

Format 2 replaces each node's required and optional arrays with member-to-type
maps and adds a `definitions` object. It is a closed manifest: misspelled or
unknown manifest/schema members reject instead of being ignored. References
are resolved while generating; every definition is validated even when
unused, and unresolved or cyclic graphs reject. Generated Kotlin uses
`ExtensionNodeSchema`/`ExtensionValueSchema`; generated Elisp uses equivalent
plists. Both carry the SHA-256 of the same canonical, fully resolved schema.

The closed value algebra is:

```json
{"type":"boolean"}
{"type":"integer","minimum":0,"maximum":10}
{"type":"number","minimum":0.0,"maximum":1.0}
{"type":"string","min_length":0,"max_length":256}
{"type":"enum","values":["one","two"]}
{"type":"object","required":{},"optional":{}}
{"type":"array","items":{"type":"boolean"},"min_items":0,"max_items":16}
{"type":"identifier-map","values":{"type":"boolean"},"min_entries":0,"max_entries":16}
{"type":"ref","ref":"definition-name"}
{"type":"node"}
{"type":"node-array","min_items":0,"max_items":16}
{"type":"action"}
```

Every string, numeric value, array, node array, and identifier map has explicit
bounds. Collection maxima may not exceed 10,000, string maxima may not exceed
1,048,576, and a complete format-2 manifest may not exceed 256 KiB. Object
required/optional maps are disjoint and object values are closed at runtime.
The `node` and `action` cases delegate their nested EBP structure to the
ordinary Node and ActionDescriptor validators rather than defining a parallel
wire grammar.
