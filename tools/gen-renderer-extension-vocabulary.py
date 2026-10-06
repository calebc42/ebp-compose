#!/usr/bin/env python3
"""Project any renderer-owned extension manifest into Kotlin and Elisp.

EBP intentionally does not register design systems.  This generator keeps a
renderer extension's schema, target support, stateful rules, and identity in
one manifest while producing Kotlin and Elisp endpoint projections.  The
extension owner supplies its manifest and both output paths:

    python3 tools/gen-renderer-extension-vocabulary.py \
      /path/to/example-design.json \
      --kotlin-output /path/to/ExampleDesignVocabulary.kt \
      --kotlin-package com.example.render \
      --elisp-output /path/to/example-design-vocabulary.el

Use ``--check`` in verification to reject stale generated files without
modifying the worktree.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent.parent

# The typed-member projection shares its derivation with gen-node-members.py.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import ebp_member_derivation as members_derivation  # noqa: E402
_EXTENSION_ID = re.compile(r"[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*\Z")
_TARGET_ID = re.compile(r"[a-z][a-z0-9_-]*\Z")
_KOTLIN_PACKAGE = re.compile(
    r"[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*\Z"
)
_SCHEMA_NAME = re.compile(r"[a-z][a-z0-9]*(?:[-_][a-z0-9]+)*\Z")
_MAX_FORMAT_2_BYTES = 256 * 1024
_MAX_SCHEMA_COLLECTION = 10_000
_MAX_SCHEMA_STRING = 1_048_576

# Renderer manifests may refine the closed objects in a selection-options
# array without adding downstream member names to the wire module.  Keep the
# type vocabulary deliberately scalar: node/action/aggregate shapes still
# require their owning, purpose-built admission rules.
# The swipe-side validator keys a side's trigger hook by the member that
# carries it, so only these two members may be typed as swipe sides.
_SWIPE_SIDE_MEMBERS = {"swipe_start", "swipe_end"}

_SELECTION_OPTION_MEMBER_TYPES = {
    "boolean",
    "finite-number",
    "integer",
    "integer-1-6",
    "non-empty-string",
    "non-negative-integer",
    "positive-integer",
    "string",
}


class ManifestError(ValueError):
    """The renderer manifest cannot be projected without guessing."""


def _repo_path(value: str | Path) -> Path:
    """Resolve VALUE against the repository, independent of the caller's cwd."""
    path = Path(value)
    return path if path.is_absolute() else ROOT / path


def _display_path(path: Path) -> str:
    """Return a stable repository-relative path when PATH belongs to this tree."""
    try:
        return path.resolve().relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        return path.resolve().as_posix()


def _source_label(path: Path) -> str:
    """Return a stable owner-relative label for generated-file headers."""
    if path.parent.name == "renderer-extensions":
        return f"renderer-extensions/{path.name}"
    return _display_path(path)


def _string_list(value: Any, where: str, *, allow_empty: bool = True) -> list[str]:
    if not isinstance(value, list) or any(
        not isinstance(item, str) or not item for item in value
    ):
        raise ManifestError(f"{where} must be an array of non-empty strings")
    if not allow_empty and not value:
        raise ManifestError(f"{where} must not be empty")
    if len(value) != len(set(value)):
        raise ManifestError(f"{where} must not contain duplicates")
    return value


def _mapping(value: Any, where: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ManifestError(f"{where} must be an object")
    return value


def _closed_mapping(
    value: Any,
    where: str,
    required: set[str],
    optional: set[str] | None = None,
) -> dict[str, Any]:
    """Return one closed object or report its exact missing/unknown members."""
    row = _mapping(value, where)
    allowed = required | (optional or set())
    missing = sorted(required - set(row))
    unknown = sorted(set(row) - allowed)
    if missing:
        raise ManifestError(f"{where} is missing required members: {missing}")
    if unknown:
        raise ManifestError(f"{where} is closed; unknown members: {unknown}")
    return row


def _bounded_integer(value: Any, where: str, *, maximum: int) -> int:
    if type(value) is not int or value < 0 or value > maximum:
        raise ManifestError(
            f"{where} must be an integer in 0..{maximum}"
        )
    return value


def _collection_bounds(
    row: dict[str, Any],
    where: str,
    minimum_member: str,
    maximum_member: str,
) -> tuple[int, int]:
    minimum = _bounded_integer(
        row[minimum_member],
        f"{where}.{minimum_member}",
        maximum=_MAX_SCHEMA_COLLECTION,
    )
    maximum = _bounded_integer(
        row[maximum_member],
        f"{where}.{maximum_member}",
        maximum=_MAX_SCHEMA_COLLECTION,
    )
    if minimum > maximum:
        raise ManifestError(
            f"{where}.{minimum_member} must not exceed {maximum_member}"
        )
    return minimum, maximum


def _node_member_names(row: dict[str, Any], group: str) -> list[str]:
    """Return format-1 list or format-2 typed-map member names."""
    members = row[group]
    return list(members if isinstance(members, dict) else members)


def _resolve_format_2_schemas(
    manifest: dict[str, Any],
) -> dict[str, dict[str, dict[str, Any]]]:
    """Validate and fully resolve the finite format-2 schema graph."""
    definitions = _mapping(manifest["definitions"], "manifest.definitions")
    for name in definitions:
        if not isinstance(name, str) or not _SCHEMA_NAME.fullmatch(name):
            raise ManifestError(
                f"manifest.definitions has invalid schema name {name!r}"
            )

    cache: dict[str, dict[str, Any]] = {}

    def resolve_definition(name: str, where: str, stack: tuple[str, ...]) -> dict[str, Any]:
        if name not in definitions:
            raise ManifestError(f"{where} has unresolved reference {name!r}")
        if name in stack:
            cycle = " -> ".join((*stack, name))
            raise ManifestError(f"{where} has cyclic reference: {cycle}")
        if name not in cache:
            cache[name] = resolve(
                definitions[name],
                f"manifest.definitions[{name!r}]",
                (*stack, name),
            )
        return cache[name]

    def member_map(
        value: Any,
        where: str,
        stack: tuple[str, ...],
    ) -> dict[str, dict[str, Any]]:
        members = _mapping(value, where)
        normalized: dict[str, dict[str, Any]] = {}
        for member in sorted(members):
            if not isinstance(member, str) or not member:
                raise ManifestError(f"{where} keys must be non-empty strings")
            normalized[member] = resolve(
                members[member], f"{where}[{member!r}]", stack
            )
            if (
                normalized[member]["type"] == "swipe-side"
                and member not in _SWIPE_SIDE_MEMBERS
            ):
                raise ManifestError(
                    f"{where}[{member!r}] types a swipe side, which only a "
                    "swipe_start or swipe_end member may carry"
                )
        return normalized

    def resolve(
        value: Any,
        where: str,
        stack: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        row = _mapping(value, where)
        schema_type = row.get("type")
        if not isinstance(schema_type, str):
            raise ManifestError(f"{where}.type must be a string")

        if schema_type == "boolean":
            _closed_mapping(row, where, {"type"})
            return {"type": schema_type}
        if schema_type in {"integer", "number"}:
            row = _closed_mapping(
                row, where, {"type", "minimum", "maximum"}
            )
            minimum = row["minimum"]
            maximum = row["maximum"]
            if schema_type == "integer":
                if type(minimum) is not int or type(maximum) is not int:
                    raise ManifestError(
                        f"{where} integer bounds must be integers"
                    )
            elif (
                type(minimum) not in {int, float}
                or type(maximum) not in {int, float}
                or not math.isfinite(float(minimum))
                or not math.isfinite(float(maximum))
            ):
                raise ManifestError(f"{where} number bounds must be finite")
            if minimum > maximum:
                raise ManifestError(
                    f"{where}.minimum must not exceed maximum"
                )
            return {
                "type": schema_type,
                "minimum": minimum,
                "maximum": maximum,
            }
        if schema_type == "string":
            row = _closed_mapping(
                row, where, {"type", "min_length", "max_length"}
            )
            minimum = _bounded_integer(
                row["min_length"],
                f"{where}.min_length",
                maximum=_MAX_SCHEMA_STRING,
            )
            maximum = _bounded_integer(
                row["max_length"],
                f"{where}.max_length",
                maximum=_MAX_SCHEMA_STRING,
            )
            if minimum > maximum:
                raise ManifestError(
                    f"{where}.min_length must not exceed max_length"
                )
            return {
                "type": schema_type,
                "min_length": minimum,
                "max_length": maximum,
            }
        if schema_type == "enum":
            row = _closed_mapping(row, where, {"type", "values"})
            values = _string_list(
                row["values"], f"{where}.values", allow_empty=False
            )
            if len(values) > 256:
                raise ManifestError(f"{where}.values exceeds 256 members")
            return {"type": schema_type, "values": sorted(values)}
        if schema_type == "object":
            row = _closed_mapping(
                row, where, {"type", "required", "optional"}
            )
            required = member_map(row["required"], f"{where}.required", stack)
            optional = member_map(row["optional"], f"{where}.optional", stack)
            overlap = set(required) & set(optional)
            if overlap:
                raise ManifestError(
                    f"{where} repeats members across required and optional: "
                    f"{sorted(overlap)}"
                )
            return {
                "type": schema_type,
                "required": required,
                "optional": optional,
            }
        if schema_type == "array":
            row = _closed_mapping(
                row,
                where,
                {"type", "items", "min_items", "max_items"},
            )
            minimum, maximum = _collection_bounds(
                row, where, "min_items", "max_items"
            )
            items = resolve(row["items"], f"{where}.items", stack)
            if items["type"] == "swipe-side":
                raise ManifestError(f"{where}.items must not be a swipe side")
            return {
                "type": schema_type,
                "items": items,
                "min_items": minimum,
                "max_items": maximum,
            }
        if schema_type == "identifier-map":
            row = _closed_mapping(
                row,
                where,
                {"type", "values", "min_entries", "max_entries"},
            )
            minimum, maximum = _collection_bounds(
                row, where, "min_entries", "max_entries"
            )
            values = resolve(row["values"], f"{where}.values", stack)
            if values["type"] == "swipe-side":
                raise ManifestError(f"{where}.values must not be a swipe side")
            return {
                "type": schema_type,
                "values": values,
                "min_entries": minimum,
                "max_entries": maximum,
            }
        if schema_type == "ref":
            row = _closed_mapping(row, where, {"type", "ref"})
            name = row["ref"]
            if not isinstance(name, str):
                raise ManifestError(f"{where}.ref must be a string")
            return resolve_definition(name, f"{where}.ref", stack)
        if schema_type == "node":
            _closed_mapping(row, where, {"type"})
            return {"type": schema_type}
        if schema_type == "node-array":
            row = _closed_mapping(
                row, where, {"type", "min_items", "max_items"}
            )
            minimum, maximum = _collection_bounds(
                row, where, "min_items", "max_items"
            )
            return {
                "type": schema_type,
                "min_items": minimum,
                "max_items": maximum,
            }
        if schema_type == "action":
            _closed_mapping(row, where, {"type"})
            return {"type": schema_type}
        if schema_type == "swipe-side":
            _closed_mapping(row, where, {"type"})
            return {"type": schema_type}
        raise ManifestError(f"{where}.type has unsupported value {schema_type!r}")

    # Validate even unused definitions: dead, malformed schema is still drift
    # in the owner authority and must not be silently projected.
    for definition_name in sorted(definitions):
        resolve_definition(
            definition_name,
            f"manifest.definitions[{definition_name!r}]",
            (),
        )

    resolved_nodes: dict[str, dict[str, dict[str, Any]]] = {}
    for node_type in manifest["node_types"]:
        row_where = f"manifest.node_schema[{node_type!r}]"
        row = _closed_mapping(
            manifest["node_schema"][node_type],
            row_where,
            {"required", "optional"},
        )
        required = member_map(row["required"], f"{row_where}.required", ())
        optional = member_map(row["optional"], f"{row_where}.optional", ())
        overlap = set(required) & set(optional)
        if overlap:
            raise ManifestError(
                f"{row_where} repeats members across required and optional: "
                f"{sorted(overlap)}"
            )
        if "t" in required or "t" in optional:
            raise ManifestError(f"{row_where} must not redeclare discriminator t")
        resolved_nodes[node_type] = {
            "required": required,
            "optional": optional,
        }
    return resolved_nodes


def load_manifest(path: Path) -> dict[str, Any]:
    """Read and validate the manifest fields this projection consumes."""
    try:
        source = path.read_text(encoding="utf-8")
        raw = json.loads(source)
    except OSError as error:
        raise ManifestError(f"cannot read {path}: {error}") from error
    except json.JSONDecodeError as error:
        raise ManifestError(f"invalid JSON in {path}: {error}") from error

    manifest = _mapping(raw, "manifest")
    if manifest.get("format") not in {1, 2}:
        raise ManifestError("manifest.format must be 1 or 2")
    format_version = manifest["format"]
    if format_version == 2:
        if len(source.encode("utf-8")) > _MAX_FORMAT_2_BYTES:
            raise ManifestError("format-2 manifest exceeds 256 KiB")
        _closed_mapping(
            manifest,
            "manifest",
            {
                "format",
                "extension",
                "description",
                "node_types",
                "targets",
                "definitions",
                "node_schema",
                "stateful_when_present",
                "at_least_one_non_empty",
            },
            {
                "selection_options",
                "true_requires_parent",
                "lazy_column_sticky_members",
            },
        )

    extension = manifest.get("extension")
    if not isinstance(extension, str) or not _EXTENSION_ID.fullmatch(extension):
        raise ManifestError(
            "manifest.extension must be a lowercase separated identifier"
        )
    description = manifest.get("description")
    if not isinstance(description, str) or not description.strip():
        raise ManifestError("manifest.description must be a non-empty string")

    node_types = _string_list(
        manifest.get("node_types"), "manifest.node_types", allow_empty=False
    )
    known_types = set(node_types)
    schemas = _mapping(manifest.get("node_schema"), "manifest.node_schema")
    if set(schemas) != known_types:
        raise ManifestError(
            "manifest.node_schema keys must exactly match manifest.node_types"
        )
    for node_type in node_types:
        row = _mapping(schemas[node_type], f"manifest.node_schema[{node_type!r}]")
        if set(row) != {"required", "optional"}:
            raise ManifestError(
                f"manifest.node_schema[{node_type!r}] must contain only "
                "required and optional"
            )
        if format_version == 1:
            required = _string_list(
                row["required"], f"manifest.node_schema[{node_type!r}].required"
            )
            optional = _string_list(
                row["optional"], f"manifest.node_schema[{node_type!r}].optional"
            )
        else:
            required = list(
                _mapping(
                    row["required"],
                    f"manifest.node_schema[{node_type!r}].required",
                )
            )
            optional = list(
                _mapping(
                    row["optional"],
                    f"manifest.node_schema[{node_type!r}].optional",
                )
            )
        overlap = set(required) & set(optional)
        if overlap:
            raise ManifestError(
                f"manifest.node_schema[{node_type!r}] repeats members across "
                f"required and optional: {sorted(overlap)}"
            )

    if format_version == 2:
        manifest["_resolved_node_schema"] = _resolve_format_2_schemas(manifest)

    targets = _mapping(manifest.get("targets"), "manifest.targets")
    if not targets:
        raise ManifestError("manifest.targets must not be empty")
    for target, values in targets.items():
        if not isinstance(target, str) or not _TARGET_ID.fullmatch(target):
            raise ManifestError(f"invalid target identifier: {target!r}")
        members = _string_list(values, f"manifest.targets[{target!r}]")
        unknown = set(members) - known_types
        if unknown:
            raise ManifestError(
                f"manifest.targets[{target!r}] names unknown nodes: "
                f"{sorted(unknown)}"
            )

    stateful = _mapping(
        manifest.get("stateful_when_present"),
        "manifest.stateful_when_present",
    )
    non_empty = _mapping(
        manifest.get("at_least_one_non_empty"),
        "manifest.at_least_one_non_empty",
    )
    selection_options = _mapping(
        manifest.get("selection_options", {}),
        "manifest.selection_options",
    )
    true_requires_parent = _mapping(
        manifest.get("true_requires_parent", {}),
        "manifest.true_requires_parent",
    )
    for node_type, member in stateful.items():
        if node_type not in known_types:
            raise ManifestError(
                f"manifest.stateful_when_present names unknown node {node_type!r}"
            )
        if not isinstance(member, str) or not member:
            raise ManifestError(
                f"manifest.stateful_when_present[{node_type!r}] must be a member"
            )
        row = schemas[node_type]
        if member not in (
            _node_member_names(row, "required") +
            _node_member_names(row, "optional")
        ):
            raise ManifestError(
                f"manifest.stateful_when_present[{node_type!r}] names unknown "
                f"member {member!r}"
            )
    for node_type, values in non_empty.items():
        if node_type not in known_types:
            raise ManifestError(
                f"manifest.at_least_one_non_empty names unknown node {node_type!r}"
            )
        members = _string_list(
            values,
            f"manifest.at_least_one_non_empty[{node_type!r}]",
            allow_empty=False,
        )
        row = schemas[node_type]
        unknown = set(members) - set(
            _node_member_names(row, "required") +
            _node_member_names(row, "optional")
        )
        if unknown:
            raise ManifestError(
                f"manifest.at_least_one_non_empty[{node_type!r}] names unknown "
                f"members: {sorted(unknown)}"
            )
    selection_rule_required_members = {
        "options_member",
        "selection_member",
        "label_member",
        "value_member",
    }
    selection_rule_optional_members = {
        "required_option_members",
        "optional_option_members",
    }
    for node_type, value in selection_options.items():
        if node_type not in known_types:
            raise ManifestError(
                f"manifest.selection_options names unknown node {node_type!r}"
            )
        rule = _mapping(
            value,
            f"manifest.selection_options[{node_type!r}]",
        )
        missing = selection_rule_required_members - set(rule)
        unknown = set(rule) - (
            selection_rule_required_members | selection_rule_optional_members
        )
        if missing or unknown:
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] must contain "
                f"{sorted(selection_rule_required_members)} and may contain "
                f"{sorted(selection_rule_optional_members)}"
            )
        if any(
            not isinstance(rule[key], str) or not rule[key]
            for key in selection_rule_required_members
        ):
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] members must be "
                "non-empty strings"
            )
        if rule["label_member"] == rule["value_member"]:
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] label_member and "
                "value_member must differ"
            )
        row = schemas[node_type]
        schema_members = set(
            _node_member_names(row, "required") +
            _node_member_names(row, "optional")
        )
        for member_key in ("options_member", "selection_member"):
            member = rule[member_key]
            if member not in schema_members:
                raise ManifestError(
                    f"manifest.selection_options[{node_type!r}].{member_key} "
                    f"names unknown member {member!r}"
                )
        if rule["options_member"] == rule["selection_member"]:
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] options_member and "
                "selection_member must differ"
            )
        if rule["options_member"] not in _node_member_names(row, "required"):
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}].options_member "
                "must name a required member"
            )
        required_option_members = _mapping(
            rule.get("required_option_members", {}),
            f"manifest.selection_options[{node_type!r}].required_option_members",
        )
        optional_option_members = _mapping(
            rule.get("optional_option_members", {}),
            f"manifest.selection_options[{node_type!r}].optional_option_members",
        )
        overlap = set(required_option_members) & set(optional_option_members)
        if overlap:
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] repeats option "
                f"members across required and optional: {sorted(overlap)}"
            )
        reserved = {rule["label_member"], rule["value_member"]}
        repeated = reserved & (
            set(required_option_members) | set(optional_option_members)
        )
        if repeated:
            raise ManifestError(
                f"manifest.selection_options[{node_type!r}] repeats label/value "
                f"members in typed option members: {sorted(repeated)}"
            )
        for group_name, members in (
            ("required_option_members", required_option_members),
            ("optional_option_members", optional_option_members),
        ):
            for member, field_type in members.items():
                if not isinstance(member, str) or not member:
                    raise ManifestError(
                        f"manifest.selection_options[{node_type!r}].{group_name} "
                        "keys must be non-empty strings"
                    )
                if (
                    not isinstance(field_type, str)
                    or field_type not in _SELECTION_OPTION_MEMBER_TYPES
                ):
                    raise ManifestError(
                        f"manifest.selection_options[{node_type!r}].{group_name}"
                        f"[{member!r}] has unsupported scalar type {field_type!r}"
                    )
    parent_rule_members = {"member", "parent_types"}
    for node_type, value in true_requires_parent.items():
        if node_type not in known_types:
            raise ManifestError(
                f"manifest.true_requires_parent names unknown node {node_type!r}"
            )
        rule = _mapping(
            value,
            f"manifest.true_requires_parent[{node_type!r}]",
        )
        if set(rule) != parent_rule_members:
            raise ManifestError(
                f"manifest.true_requires_parent[{node_type!r}] must contain "
                f"only {sorted(parent_rule_members)}"
            )
        member = rule["member"]
        if not isinstance(member, str) or not member:
            raise ManifestError(
                f"manifest.true_requires_parent[{node_type!r}].member must "
                "be a non-empty string"
            )
        row = schemas[node_type]
        if member not in _node_member_names(row, "optional"):
            raise ManifestError(
                f"manifest.true_requires_parent[{node_type!r}].member must "
                f"name an optional schema member, got {member!r}"
            )
        _string_list(
            rule["parent_types"],
            f"manifest.true_requires_parent[{node_type!r}].parent_types",
            allow_empty=False,
        )
    lazy_column_sticky_members = _mapping(
        manifest.get("lazy_column_sticky_members", {}),
        "manifest.lazy_column_sticky_members",
    )
    for node_type, member in lazy_column_sticky_members.items():
        if node_type not in known_types:
            raise ManifestError(
                "manifest.lazy_column_sticky_members names unknown node "
                f"{node_type!r}"
            )
        if not isinstance(member, str) or not member:
            raise ManifestError(
                f"manifest.lazy_column_sticky_members[{node_type!r}] must be "
                "a non-empty member name"
            )
        parent_rule = true_requires_parent.get(node_type)
        if not isinstance(parent_rule, dict) or parent_rule.get("member") != member:
            raise ManifestError(
                f"manifest.lazy_column_sticky_members[{node_type!r}] must match "
                "that node's true_requires_parent member"
            )
        if "lazy_column" not in parent_rule.get("parent_types", []):
            raise ManifestError(
                f"manifest.lazy_column_sticky_members[{node_type!r}] requires "
                "lazy_column in true_requires_parent.parent_types"
            )
    return manifest


def _identity(extension: str) -> tuple[str, str, str, str, str]:
    """Return Pascal, constant, Elisp, authority, and human design prefixes."""
    parts = re.split(r"[._-]", extension)
    pascal = "".join(part[0].upper() + part[1:] for part in parts)
    constant = "_".join(part.upper() for part in parts)
    elisp = "-".join(parts)

    def human(part: str) -> str:
        return re.sub(r"(?<=[a-z])(?=[0-9])", " ", part).capitalize()

    authority = human(parts[0])
    design = " ".join(human(part) for part in parts[1:]) or authority
    design_family = re.sub(r"\s+[0-9]+\Z", "", design)
    human_prefix = (
        f"{authority} {design_family}" if len(parts) > 1 else authority
    )
    return pascal, constant, elisp, authority, human_prefix


def _quoted(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def _kt_set(values: list[str]) -> str:
    return "setOf(" + ", ".join(_quoted(value) for value in values) + ")"


def _kt_string_map(values: dict[str, str]) -> str:
    return "mapOf(" + ", ".join(
        f"{_quoted(key)} to {_quoted(value)}" for key, value in values.items()
    ) + ")"


def _el_strings(values: list[str]) -> str:
    return " ".join(_quoted(value) for value in values)


def _canonical_schema_digest(manifest: dict[str, Any]) -> str:
    canonical = json.dumps(
        manifest["_resolved_node_schema"],
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(canonical).hexdigest()


def _kt_double(value: int | float) -> str:
    rendered = repr(float(value))
    return rendered.replace("e+", "e")


def _kt_schema_map(values: dict[str, dict[str, Any]]) -> str:
    return "mapOf(" + ", ".join(
        f"{_quoted(member)} to {_kt_value_schema(schema)}"
        for member, schema in values.items()
    ) + ")"


def _kt_value_schema(schema: dict[str, Any]) -> str:
    schema_type = schema["type"]
    prefix = "ExtensionValueSchema."
    if schema_type == "boolean":
        return prefix + "BooleanValue"
    if schema_type == "integer":
        return (
            prefix + "IntegerValue(" +
            f'{schema["minimum"]}L, {schema["maximum"]}L)'
        )
    if schema_type == "number":
        return (
            prefix + "NumberValue(" +
            f'{_kt_double(schema["minimum"])}, '
            f'{_kt_double(schema["maximum"])})'
        )
    if schema_type == "string":
        return (
            prefix + "StringValue(" +
            f'{schema["min_length"]}, {schema["max_length"]})'
        )
    if schema_type == "enum":
        return prefix + "EnumValue(" + _kt_set(schema["values"]) + ")"
    if schema_type == "object":
        return (
            prefix + "ObjectValue(" +
            f'{_kt_schema_map(schema["required"])}, '
            f'{_kt_schema_map(schema["optional"])})'
        )
    if schema_type == "array":
        return (
            prefix + "ArrayValue(" +
            f'{_kt_value_schema(schema["items"])}, '
            f'{schema["min_items"]}, {schema["max_items"]})'
        )
    if schema_type == "identifier-map":
        return (
            prefix + "IdentifierMapValue(" +
            f'{_kt_value_schema(schema["values"])}, '
            f'{schema["min_entries"]}, {schema["max_entries"]})'
        )
    if schema_type == "node":
        return prefix + "NodeValue"
    if schema_type == "node-array":
        return (
            prefix + "NodeArrayValue(" +
            f'{schema["min_items"]}, {schema["max_items"]})'
        )
    if schema_type == "action":
        return prefix + "ActionDescriptorValue"
    if schema_type == "swipe-side":
        return prefix + "SwipeSideValue"
    raise AssertionError(f"unprojected schema type {schema_type}")


def _el_schema_map(values: dict[str, dict[str, Any]]) -> str:
    if not values:
        return "nil"
    return "(" + " ".join(
        f'({_quoted(member)} . {_el_value_schema(schema)})'
        for member, schema in values.items()
    ) + ")"


def _el_value_schema(schema: dict[str, Any]) -> str:
    schema_type = schema["type"]
    if schema_type == "boolean":
        return "(:type boolean)"
    if schema_type in {"integer", "number"}:
        return (
            f'(:type {schema_type} :minimum {schema["minimum"]} '
            f':maximum {schema["maximum"]})'
        )
    if schema_type == "string":
        return (
            f'(:type string :min-length {schema["min_length"]} '
            f':max-length {schema["max_length"]})'
        )
    if schema_type == "enum":
        return f'(:type enum :values ({_el_strings(schema["values"])}))'
    if schema_type == "object":
        return (
            "(:type object " +
            f':required {_el_schema_map(schema["required"])} '
            f':optional {_el_schema_map(schema["optional"])})'
        )
    if schema_type == "array":
        return (
            "(:type array " +
            f':items {_el_value_schema(schema["items"])} '
            f':min-items {schema["min_items"]} '
            f':max-items {schema["max_items"]})'
        )
    if schema_type == "identifier-map":
        return (
            "(:type identifier-map " +
            f':values {_el_value_schema(schema["values"])} '
            f':min-entries {schema["min_entries"]} '
            f':max-entries {schema["max_entries"]})'
        )
    if schema_type == "node":
        return "(:type node)"
    if schema_type == "node-array":
        return (
            "(:type node-array " +
            f':min-items {schema["min_items"]} '
            f':max-items {schema["max_items"]})'
        )
    if schema_type == "action":
        return "(:type action)"
    if schema_type == "swipe-side":
        return "(:type swipe-side)"
    raise AssertionError(f"unprojected schema type {schema_type}")


def render_kotlin(
    manifest: dict[str, Any], source_label: str, package_name: str
) -> str:
    """Render one Kotlin projection without consulting an endpoint registry."""
    pascal, constant, _, _, _ = _identity(manifest["extension"])
    schema_rows = []
    for node_type in manifest["node_types"]:
        row = manifest["node_schema"][node_type]
        required = _node_member_names(row, "required")
        optional = _node_member_names(row, "optional")
        schema_rows.append(
            f'    "{node_type}" to NodeRow({_kt_set(required)}, '
            f'{_kt_set(optional)}),'
        )
    target_rows = [
        f'    "{target}" to {_kt_set(node_types)},'
        for target, node_types in manifest["targets"].items()
    ]
    stateful_rows = [
        f'    "{node_type}" to "{member}",'
        for node_type, member in manifest["stateful_when_present"].items()
    ]
    non_empty_rows = [
        f'    "{node_type}" to {_kt_set(members)},'
        for node_type, members in manifest["at_least_one_non_empty"].items()
    ]
    selection_rows = []
    for node_type, rule in manifest.get("selection_options", {}).items():
        extras = ""
        required_option_members = rule.get("required_option_members", {})
        optional_option_members = rule.get("optional_option_members", {})
        if required_option_members or optional_option_members:
            extras = (
                ", requiredOptionMembers = "
                f"{_kt_string_map(required_option_members)}, "
                "optionalOptionMembers = "
                f"{_kt_string_map(optional_option_members)}"
            )
        selection_rows.append(
            f'    "{node_type}" to SelectionOptionsRule('
            f'{_quoted(rule["options_member"])}, '
            f'{_quoted(rule["selection_member"])}, '
            f'{_quoted(rule["label_member"])}, '
            f'{_quoted(rule["value_member"])}{extras}),'
        )
    parent_rows = [
        f'    "{node_type}" to TrueRequiresParentRule('
        f'{_quoted(rule["member"])}, {_kt_set(rule["parent_types"])}),'
        for node_type, rule in manifest.get("true_requires_parent", {}).items()
    ]
    sticky_rows = [
        f'    "{node_type}" to {_quoted(member)},'
        for node_type, member in manifest.get(
            "lazy_column_sticky_members", {}
        ).items()
    ]
    kotlin_imports = ["import com.calebc42.ebp.wire.NodeRow"]
    typed_projection = ""
    if manifest["format"] == 2:
        kotlin_imports.extend([
            "import com.calebc42.ebp.wire.ExtensionNodeSchema",
            "import com.calebc42.ebp.wire.ExtensionValueSchema",
        ])
        typed_rows = []
        for node_type in manifest["node_types"]:
            row = manifest["_resolved_node_schema"][node_type]
            typed_rows.append(
                f'    {_quoted(node_type)} to ExtensionNodeSchema('
                f'{_kt_schema_map(row["required"])}, '
                f'{_kt_schema_map(row["optional"])}),'
            )
        digest = _canonical_schema_digest(manifest)
        typed_projection = f'''
val {constant}_TYPED_NODE_SCHEMA: Map<String, ExtensionNodeSchema> = mapOf(
{chr(10).join(typed_rows)}
)

const val {constant}_SCHEMA_SHA256 = "{digest}"
'''
    if selection_rows:
        kotlin_imports.append(
            "import com.calebc42.ebp.wire.SelectionOptionsRule"
        )
    if parent_rows:
        kotlin_imports.append(
            "import com.calebc42.ebp.wire.TrueRequiresParentRule"
        )
    selection_projection = (
        f'''\n\nval {constant}_SELECTION_OPTIONS: Map<String, SelectionOptionsRule> = mapOf(
{chr(10).join(selection_rows)}
)\n'''
        if selection_rows
        else "\n"
    )
    parent_projection = (
        f'''\nval {constant}_TRUE_REQUIRES_PARENT: Map<String, TrueRequiresParentRule> = mapOf(
{chr(10).join(parent_rows)}
)\n'''
        if parent_rows
        else ""
    )
    sticky_projection = (
        f'''\nval {constant}_LAZY_COLUMN_STICKY_MEMBERS: Map<String, String> = mapOf(
{chr(10).join(sticky_rows)}
)\n'''
        if sticky_rows
        else ""
    )
    return f'''// SPDX-License-Identifier: GPL-3.0-or-later
// GENERATED from {source_label} (format
// {manifest["format"]}) by tools/gen-renderer-extension-vocabulary.py.
// DO NOT EDIT: {pascal}VocabularyTest pins this projection.
package {package_name}

{chr(10).join(kotlin_imports)}

const val {constant}_EXTENSION = "{manifest["extension"]}"

val {constant}_NODE_SCHEMA: Map<String, NodeRow> = mapOf(
{chr(10).join(schema_rows)}
)

val {constant}_TARGET_NODE_TYPES: Map<String, Set<String>> = mapOf(
{chr(10).join(target_rows)}
)

val {constant}_STATEFUL_WHEN_PRESENT: Map<String, String> = mapOf(
{chr(10).join(stateful_rows)}
)

val {constant}_AT_LEAST_ONE_NON_EMPTY: Map<String, Set<String>> = mapOf(
{chr(10).join(non_empty_rows)}
){selection_projection}{parent_projection}{sticky_projection}{typed_projection}'''


def render_elisp(
    manifest: dict[str, Any], source_label: str, output_name: str
) -> str:
    """Render one Elisp projection using names derived from the extension ID."""
    _, _, prefix, authority, human_prefix = _identity(manifest["extension"])
    schema_rows = []
    for node_type in manifest["node_types"]:
        row = manifest["node_schema"][node_type]
        required = _node_member_names(row, "required")
        optional = _node_member_names(row, "optional")
        schema_rows.append(
            f'    ("{node_type}" ({_el_strings(sorted(required))}) '
            f'({_el_strings(sorted(optional))}))'
        )
    targets = [
        f'    ({target} . ({_el_strings(node_types)}))'
        for target, node_types in manifest["targets"].items()
    ]
    design_family = human_prefix.removeprefix(f"{authority} ")
    possessive = f"{authority}'" if authority.endswith("s") else f"{authority}'s"
    typed_projection = ""
    if manifest["format"] == 2:
        typed_rows = []
        for node_type in manifest["node_types"]:
            row = manifest["_resolved_node_schema"][node_type]
            typed_rows.append(
                f'    ({_quoted(node_type)} . '
                f'(:required {_el_schema_map(row["required"])} '
                f':optional {_el_schema_map(row["optional"])}))'
            )
        digest = _canonical_schema_digest(manifest)
        typed_projection = f'''
(defconst {prefix}-typed-node-schema
  '(\n{chr(10).join(typed_rows)})
  "{human_prefix} closed typed node schemas.")

(defconst {prefix}-schema-sha256 "{digest}"
  "SHA-256 of the canonical resolved typed schema projection.")
'''
    return f''';;; {output_name} --- generated renderer vocabulary -*- lexical-binding: t; -*-

;; SPDX-License-Identifier: GPL-3.0-or-later
;; Package-Requires: ((emacs "30.1"))

;;; Commentary:

;; GENERATED from {source_label} (format
;; {manifest["format"]}) by tools/gen-renderer-extension-vocabulary.py.
;; DO NOT EDIT.  This is {authority} authority, not an EBP registry.

;;; Code:

(defconst {prefix}-extension "{manifest["extension"]}"
  "Renderer extension implemented by {possessive} {design_family} design layer.")

(defconst {prefix}-node-schema
  '(
{chr(10).join(schema_rows)})
  "{human_prefix} node schemas as (TYPE REQUIRED OPTIONAL).")

(defconst {prefix}-target-node-types
  '(
{chr(10).join(targets)})
  "{human_prefix} node types supported by each renderer target.")
{typed_projection}
(provide '{prefix}-vocabulary)
;;; {output_name} ends here
'''


def render_member_projections(
    manifest: dict[str, Any],
    source_label: str,
    package_name: str,
    contract_path: Path | None,
    strict: bool,
) -> tuple[str, str, int]:
    """Render the typed `NodeMemberSet` objects and enum classes for one manifest.

    A format-2 manifest types every member itself. A format-1 manifest carries
    member names only, so each member is derived by bare name against the core
    contract (field types, bare enum domains, action hooks); a name the contract
    does not type becomes an untyped `AnyMember` and is counted, or rejects the
    run when STRICT. Returns (enums source, members source, untyped count).
    """
    md = members_derivation
    pascal, constant, _, _, _ = _identity(manifest["extension"])
    header = (
        f"// GENERATED from {source_label} (format {manifest['format']}) by\n"
        "// tools/gen-renderer-extension-vocabulary.py. DO NOT EDIT: "
        f"{pascal}VocabularyTest pins this projection.\n"
    )
    domains: dict[str, tuple[str, ...]] = {}
    class_name_for: dict[str, str] = {}
    node_sets: list[md.NodeMembersDecl] = []
    untyped = 0
    derivation = None
    if manifest["format"] != 2:
        if contract_path is None:
            raise ManifestError("--contract is required to type a format-1 manifest's members")
        derivation = md.ContractDerivation.load(contract_path)
    for node_type in manifest["node_types"]:
        decl = md.NodeMembersDecl(node_type, md.members_object_name(node_type))
        if manifest["format"] == 2:
            resolved_row = manifest["_resolved_node_schema"][node_type]
            for group, required in (("required", True), ("optional", False)):
                for member, schema in resolved_row[group].items():
                    resolved = md.resolve_manifest_schema(schema)
                    if resolved.tag == "enum":
                        key = f"{node_type}.{member}"
                        domains[key] = resolved.enum_values
                        class_name_for[key] = md.enum_class_name(key)
                        resolved = md.ResolvedType("enum", key, resolved.enum_values)
                    decl.members.append(md.MemberDecl(member, required, resolved))
        else:
            row = manifest["node_schema"][node_type]
            for group, required in (("required", True), ("optional", False)):
                for member in _node_member_names(row, group):
                    try:
                        resolved = derivation.resolve(None, member)
                    except md.DerivationError as error:
                        if strict:
                            raise ManifestError(f"{node_type}.{member}: {error}") from error
                        untyped += 1
                        resolved = md.ResolvedType("untyped")
                    if resolved.enum_key:
                        domains[resolved.enum_key] = resolved.enum_values
                        class_name_for[resolved.enum_key] = md.enum_class_name(resolved.enum_key)
                    decl.members.append(md.MemberDecl(member, required, resolved))
        node_sets.append(decl)
    enums = md.render_enums_kotlin(package_name, domains, f"{constant}_ENUMS", header, class_name_for)
    members = md.render_members_kotlin(
        package_name, node_sets, f"{constant}_NODE_MEMBERS", header, class_name_for
    )
    return enums, members, untyped


def _write_or_check(path: Path, content: str, check: bool) -> bool:
    """Write CONTENT or return whether PATH already contains it in check mode."""
    if check:
        try:
            current = path.read_text(encoding="utf-8")
        except OSError:
            return False
        return current == content
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "manifest",
        help="renderer-extension manifest",
    )
    parser.add_argument(
        "--kotlin-output",
        required=True,
        help="generated Kotlin file",
    )
    parser.add_argument(
        "--kotlin-package",
        required=True,
        help="generated Kotlin package",
    )
    parser.add_argument(
        "--elisp-output",
        required=True,
        help="generated Elisp file",
    )
    parser.add_argument(
        "--members-output",
        help="generated Kotlin file of typed NodeMemberSet objects (with --enums-output)",
    )
    parser.add_argument(
        "--enums-output",
        help="generated Kotlin file of enum domains (with --members-output)",
    )
    parser.add_argument(
        "--contract",
        help="core ebp/contract.json used to type a format-1 manifest's members "
        "(default: ../ebp/contract.json beside this checkout)",
    )
    parser.add_argument(
        "--strict-members",
        action="store_true",
        help="reject a format-1 member the core contract cannot type instead of "
        "projecting it untyped",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail when either output is missing or stale; write nothing",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _parser()
    args = parser.parse_args(argv)
    manifest_path = _repo_path(args.manifest)
    if not _KOTLIN_PACKAGE.fullmatch(args.kotlin_package):
        parser.error("--kotlin-package is not a valid Kotlin package")

    kotlin_output = _repo_path(args.kotlin_output)
    elisp_output = _repo_path(args.elisp_output)
    try:
        manifest = load_manifest(manifest_path)
    except ManifestError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2

    source_label = _source_label(manifest_path)
    kotlin = render_kotlin(manifest, source_label, args.kotlin_package)
    elisp = render_elisp(manifest, source_label, elisp_output.name)
    outputs = [(kotlin_output, kotlin), (elisp_output, elisp)]
    untyped = 0
    if bool(args.members_output) != bool(args.enums_output):
        parser.error("--members-output and --enums-output go together")
    if args.members_output:
        contract_path = (
            _repo_path(args.contract) if args.contract
            else ROOT.parent / "ebp" / "contract.json"
        )
        try:
            enums, members, untyped = render_member_projections(
                manifest, source_label, args.kotlin_package,
                contract_path if contract_path.is_file() else None,
                args.strict_members,
            )
        except (ManifestError, members_derivation.DerivationError) as error:
            print(f"error: {error}", file=sys.stderr)
            return 2
        outputs.append((_repo_path(args.enums_output), enums))
        outputs.append((_repo_path(args.members_output), members))
    stale = [
        path for path, content in outputs
        if not _write_or_check(path, content, args.check)
    ]
    if args.check and stale:
        for path in stale:
            print(f"stale generated file: {_display_path(path)}", file=sys.stderr)
        return 1

    verb = "checked" if args.check else "wrote"
    print(
        verb,
        " and ".join(_display_path(path) for path, _ in outputs),
        f'({len(manifest["node_types"])} node types'
        + (f", {untyped} untyped members" if untyped else "")
        + ")",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
