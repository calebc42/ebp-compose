#!/usr/bin/env python3
"""Shared derivation of typed node members from the EBP contract.

Used by ``gen-node-members.py`` (the core node vocabulary) and by
``gen-renderer-extension-vocabulary.py`` (renderer-extension manifests). The
derivation is the SPEC §17.1 resolution order projected by amendment #192:

    field_types["node.member"] -> enums["node.member"] -> enums[member]
    -> field_types[member] -> actions.hook_keys / the ``on_`` prefix
    -> ``children``

Nothing is guessed: a member the contract does not type is a generator error,
never an ``Any`` fallback (except for format-1 extension manifests, which carry
member names only and say so with the tag ``untyped``).
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


class DerivationError(ValueError):
    """A member whose type the contract cannot supply."""


# Contract enum domains that are not node members: dialogs, notifications,
# editor diagnostics, toolbars, tab items, themes, window classes, snackbar
# results, canvas ops. They are not projected as node-member enums.
NON_NODE_ENUM_DOMAINS = frozenset({
    "dialog.style", "notification.priority", "diagnostic.severity",
    "toolbar.placement", "tab_item.icon_position", "theme.layout_direction",
    "window.size_class", "snackbar.result", "canvas.op",
})

KOTLIN_PACKAGE = "com.calebc42.ebp.renderer.model.vocabulary"


@dataclass(frozen=True)
class ResolvedType:
    """One member's resolved type: the tag plus, for enums, its domain."""

    tag: str
    enum_key: str | None = None
    enum_values: tuple[str, ...] = ()
    minimum: int | None = None
    maximum: int | None = None
    fmt: str | None = None


@dataclass
class MemberDecl:
    """A member ready to render: wire key, requiredness, resolved type, default."""

    key: str
    required: bool
    resolved: ResolvedType
    default: Any = None


@dataclass
class NodeMembersDecl:
    """One generated ``NodeMemberSet`` object."""

    node_type: str
    object_name: str
    members: list[MemberDecl] = field(default_factory=list)


# --------------------------------------------------------------- naming ----


def pascal(name: str) -> str:
    """``text_input`` -> ``TextInput``; ``material3.assist_chip`` -> ``Material3AssistChip``."""
    parts = [p for p in re.split(r"[._\-/\s]+", name) if p]
    out = "".join(p[0].upper() + p[1:] for p in parts)
    if not out:
        raise DerivationError(f"cannot name {name!r}")
    return out


def enum_class_name(domain_key: str) -> str:
    """``text.style`` -> ``TextStyle``; bare ``arrange`` -> ``Arrange``."""
    return pascal(domain_key.replace(".", "_"))


def enum_constant(value: str) -> str:
    """``elevated_suggestion`` -> ``ElevatedSuggestion``; a leading digit gets ``V``."""
    out = pascal(value)
    if out[0].isdigit():
        out = "V" + out
    return out


def members_object_name(node_type: str) -> str:
    return pascal(node_type) + "Members"


# ----------------------------------------------------------- derivation ----


_INT_TAGS = {
    "positive-integer": (1, 9_007_199_254_740_991),
    "non-negative-integer": (0, 9_007_199_254_740_991),
    "integer-1-12": (1, 12),
    "integer-1-31": (1, 31),
}
_DATE_FORMATS = {"yyyy-mm", "yyyy-mm-dd", "hh-mm"}
_SCALAR_TAGS = {"option-value"}
_ANY_TAGS = {"option-value-or-array", "toolbar-id-or-item-array", "number-or-corner-object"}


class ContractDerivation:
    """Resolves node and universal member types against one contract."""

    def __init__(self, contract: dict[str, Any]) -> None:
        self.contract = contract
        self.node_schema: dict[str, dict[str, list[str]]] = contract["node_schema"]
        self.field_types: dict[str, str] = contract["field_types"]
        self.enums: dict[str, list[str]] = contract["enums"]
        self.hook_keys = set(contract["actions"]["hook_keys"])
        self.universal: list[str] = list(contract["universal_node_attributes"])

    @classmethod
    def load(cls, contract_path: Path) -> "ContractDerivation":
        return cls(json.loads(contract_path.read_text(encoding="utf-8")))

    def node_members(self, node_type: str) -> tuple[list[str], list[str]]:
        row = self.node_schema[node_type]
        return list(row["required"]), list(row["optional"])

    def resolve(self, node_type: str | None, member: str) -> ResolvedType:
        """Resolve one member; ``node_type`` None means a universal attribute."""
        scoped = f"{node_type}.{member}" if node_type else None
        tag = self.field_types.get(scoped) if scoped else None
        if tag is None or tag == "enum":
            domain = None
            if scoped and scoped in self.enums:
                domain = scoped
            elif member in self.enums:
                domain = member
            if domain is not None:
                return ResolvedType("enum", domain, tuple(self.enums[domain]))
            if tag == "enum":
                raise DerivationError(f"{scoped} is `enum` without an enums domain")
        if tag is None:
            bare = self.field_types.get(member)
            if bare is not None and bare != "varies-per-node":
                tag = bare
        if tag is None:
            if member in self.hook_keys or member.startswith("on_"):
                tag = "action-descriptor"
            elif member == "children":
                tag = "node-array"
        if tag is None:
            where = scoped or f"`{member}`"
            raise DerivationError(f"{where} has no field type, enum domain, or structural rule")
        return self._from_tag(tag)

    def _from_tag(self, tag: str) -> ResolvedType:
        if tag in _INT_TAGS:
            lo, hi = _INT_TAGS[tag]
            return ResolvedType(tag, minimum=lo, maximum=hi)
        if tag in _DATE_FORMATS:
            return ResolvedType(tag, fmt=tag)
        return ResolvedType(tag)

    def enum_domains_in_scope(self) -> dict[str, tuple[str, ...]]:
        """Every ``enums`` key a node member or universal attribute resolves to, in contract order."""
        used: dict[str, tuple[str, ...]] = {}
        for node_type in self.node_schema:
            required, optional = self.node_members(node_type)
            for member in required + optional:
                resolved = self.resolve(node_type, member)
                if resolved.enum_key:
                    used[resolved.enum_key] = resolved.enum_values
        for member in self.universal:
            resolved = self.resolve(None, member)
            if resolved.enum_key:
                used[resolved.enum_key] = resolved.enum_values
        ordered = {k: v for k, v in self.enums.items() if k in used}
        stray = {k for k in self.enums if "." in k and k not in used and k not in NON_NODE_ENUM_DOMAINS}
        if stray:
            raise DerivationError(f"enum domains that are neither node members nor listed non-node domains: {sorted(stray)}")
        return {k: tuple(v) for k, v in ordered.items()}

    # ---- contract-projected defaults -------------------------------------

    def contract_default(self, node_type: str, member: str) -> Any:
        """A default the contract itself projects for this member, else None."""
        if node_type == "text_input" and member == "variant":
            return self.contract.get("text_input_schema", {}).get("variant_default")
        semantics = self.contract.get("semantics_schema", {}).get("default_node_semantics", {})
        entry = semantics.get(node_type, {})
        if entry.get("checked_member") == member and "checked_default" in entry:
            return entry["checked_default"]
        if entry.get("selected_member") == member and "selected_default" in entry:
            return entry["selected_default"]
        if entry.get("selection_member") == member and "selection_default_index" in entry:
            return entry["selection_default_index"]
        return None


# --------------------------------------------------- format-2 manifests ----


def resolve_manifest_schema(schema: dict[str, Any]) -> ResolvedType:
    """Map one resolved format-2 value schema to a member type."""
    kind = schema["type"]
    if kind == "enum":
        return ResolvedType("enum", None, tuple(schema["values"]))
    if kind == "integer":
        return ResolvedType("integer", minimum=int(schema["minimum"]), maximum=int(schema["maximum"]))
    if kind in {"string", "number", "boolean", "node", "node-array", "action", "object", "array", "identifier-map"}:
        return ResolvedType({"action": "action-descriptor"}.get(kind, kind))
    raise DerivationError(f"unprojected manifest schema type {kind!r}")


# ------------------------------------------------------------- rendering ----


def _quoted(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def kotlin_default(resolved: ResolvedType, default: Any, enum_class: str | None) -> str | None:
    if default is None:
        return None
    if resolved.tag == "enum":
        return f"{enum_class}.{enum_constant(str(default))}"
    if isinstance(default, bool):
        return "true" if default else "false"
    if isinstance(default, int):
        return str(default)
    if isinstance(default, float):
        return repr(default)
    return _quoted(str(default))


def kotlin_member(
    decl: MemberDecl, enum_class_for: dict[str, str], enums_package: str
) -> tuple[str, str]:
    """Return (Kotlin type, constructor expression) for one member.

    Enum domains are referenced by fully qualified name in expression position:
    a member such as `arrange` becomes the property `Arrange`, which would
    otherwise shadow the enum class `Arrange` it is typed with.
    """
    r = decl.resolved
    key = _quoted(decl.key)
    req = "true" if decl.required else "false"
    tag = _quoted(r.tag)
    if r.tag == "enum":
        cls = enum_class_for[r.enum_key or decl.key]
        qualified = f"{enums_package}.{cls}"
        default = kotlin_default(r, decl.default, qualified)
        extra = f", default = {default}" if default else ""
        return f"EnumMember<{cls}>", f"EnumMember({key}, required = {req}, domain = {qualified}{extra})"
    if r.tag in {"string"} or r.fmt:
        default = kotlin_default(r, decl.default, None)
        parts = [key, f"required = {req}"]
        if r.tag != "string":
            parts.append(f"tag = {tag}")
        if r.fmt:
            parts.append(f"format = {_quoted(r.fmt)}")
        if default:
            parts.append(f"default = {default}")
        return "StringMember", f"StringMember({', '.join(parts)})"
    if r.tag == "identifier":
        return "IdentifierMember", f"IdentifierMember({key}, required = {req})"
    if r.tag == "color":
        return "ColorMember", f"ColorMember({key}, required = {req})"
    if r.tag == "boolean":
        default = kotlin_default(r, decl.default, None)
        extra = f", default = {default}" if default else ""
        return "BooleanMember", f"BooleanMember({key}, required = {req}{extra})"
    if r.tag == "boolean-or-auto":
        return "BooleanOrAutoMember", f"BooleanOrAutoMember({key}, required = {req})"
    if r.tag == "number":
        default = kotlin_default(r, decl.default, None)
        extra = f", default = {default}" if default else ""
        return "NumberMember", f"NumberMember({key}, required = {req}{extra})"
    if r.tag == "dp":
        return "DpMember", f"DpMember({key}, required = {req})"
    if r.minimum is not None:
        default = kotlin_default(r, decl.default, None)
        extra = f", default = {default}" if default else ""
        return "IntMember", (
            f"IntMember({key}, required = {req}, tag = {tag}, "
            f"minimum = {r.minimum}L, maximum = {r.maximum}L{extra})"
        )
    if r.tag == "action-descriptor":
        return "ActionMember", f"ActionMember({key}, required = {req})"
    if r.tag == "node":
        return "ChildNodeMember", f"ChildNodeMember({key}, required = {req})"
    if r.tag == "node-array":
        return "NodeArrayMember", f"NodeArrayMember({key}, required = {req})"
    if r.tag == "two-number-array":
        return "TwoNumberArrayMember", f"TwoNumberArrayMember({key}, required = {req})"
    if r.tag == "number-array":
        return "NumberArrayMember", f"NumberArrayMember({key}, required = {req})"
    if r.tag in {"string-or-number", "font-weight"}:
        return "StringOrNumberMember", f"StringOrNumberMember({key}, required = {req}, tag = {tag})"
    if r.tag in _SCALAR_TAGS:
        return "ScalarMember", f"ScalarMember({key}, required = {req}, tag = {tag})"
    if r.tag in _ANY_TAGS or r.tag == "untyped":
        return "AnyMember", f"AnyMember({key}, required = {req}, tag = {tag})"
    if r.tag.endswith("-object") or r.tag in {"object", "identifier-map"}:
        return "ObjectMember", f"ObjectMember({key}, required = {req}, tag = {tag})"
    if r.tag.endswith("-array") or r.tag == "array":
        return "ArrayMember", f"ArrayMember({key}, required = {req}, tag = {tag})"
    raise DerivationError(f"no member class for tag {r.tag!r} ({decl.key})")


def render_enums_kotlin(
    package: str,
    domains: dict[str, tuple[str, ...]],
    map_name: str | None,
    header: str,
    class_name_for: dict[str, str],
) -> str:
    """Render one Kotlin file of ``enum class`` domains (and an optional registry map)."""
    blocks = []
    for key, values in domains.items():
        cls = class_name_for[key]
        constants = [enum_constant(v) for v in values]
        if len(set(constants)) != len(constants):
            raise DerivationError(f"enum domain {key} has colliding constant names")
        entries = ",\n".join(f"    {c}({_quoted(v)})" for c, v in zip(constants, values))
        blocks.append(
            f"/** Contract enum domain `{key}`. */\n"
            f"enum class {cls}(override val wire: String) : WireEnum {{\n"
            f"{entries};\n\n"
            f"    companion object : EnumDomain<{cls}> {{\n"
            f"        override val contractKey: String = {_quoted(key)}\n"
            f"        override val values: List<{cls}> get() = entries\n"
            f"    }}\n"
            f"}}"
        )
    registry = ""
    if map_name:
        rows = ",\n".join(f"    {_quoted(k)} to {class_name_for[k]}" for k in domains)
        value = f"mapOf(\n{rows},\n)" if domains else "emptyMap()"
        registry = (
            f"\n\n/** Every generated enum domain, keyed by its contract key. */\n"
            f"val {map_name}: Map<String, EnumDomain<*>> = {value}\n"
        )
    body = "\n\n".join(blocks) if blocks else "// This manifest declares no enum member."
    return (
        f"// SPDX-License-Identifier: GPL-3.0-or-later\n{header}"
        f"package {package}\n\n"
        f"import {KOTLIN_PACKAGE}.EnumDomain\n"
        f"import {KOTLIN_PACKAGE}.WireEnum\n\n"
        f"{body}{registry}"
    )


def render_members_kotlin(
    package: str,
    node_sets: list[NodeMembersDecl],
    map_name: str | None,
    header: str,
    enum_class_for: dict[str, str],
    enums_package: str | None = None,
) -> str:
    """Render one Kotlin file of ``NodeMemberSet`` objects (and an optional registry map)."""
    used_classes: set[str] = {"NodeMember", "NodeMemberSet"}
    blocks = []
    for decl in node_sets:
        lines = []
        names: list[str] = []
        for member in decl.members:
            kt_type, expr = kotlin_member(member, enum_class_for, enums_package or package)
            used_classes.add(kt_type.split("<")[0])
            name = pascal(member.key)
            if name in names:
                raise DerivationError(f"{decl.node_type} member names collide on {name}")
            names.append(name)
            lines.append(f"    val {name}: {kt_type} = {expr}")
        required = [pascal(m.key) for m in decl.members if m.required]
        optional = [pascal(m.key) for m in decl.members if not m.required]
        lines.append(
            f"    override val required: List<NodeMember<*>> = listOf({', '.join(required)})"
        )
        lines.append(
            f"    override val optional: List<NodeMember<*>> = listOf({', '.join(optional)})"
        )
        doc = (
            "Universal node attributes (SPEC §16.5), readable on every node."
            if decl.node_type == "*"
            else f"Typed members of the `{decl.node_type}` node."
        )
        blocks.append(
            f"/** {doc} */\n"
            f"object {decl.object_name} : NodeMemberSet({_quoted(decl.node_type)}) {{\n"
            + "\n".join(lines)
            + "\n}"
        )
    registry = ""
    if map_name:
        rows = ",\n".join(
            f"    {_quoted(d.node_type)} to {d.object_name}" for d in node_sets if d.node_type != "*"
        )
        registry = (
            f"\n\n/** Every generated member set, keyed by node type. */\n"
            f"val {map_name}: Map<String, NodeMemberSet> = mapOf(\n{rows},\n)\n"
        )
    imports = [f"import {KOTLIN_PACKAGE}.{c}" for c in sorted(used_classes)]
    if enums_package and enums_package != package:
        imports.append(f"import {enums_package}.*")
    body = "\n\n".join(blocks)
    return (
        f"// SPDX-License-Identifier: GPL-3.0-or-later\n{header}"
        f"package {package}\n\n"
        + "\n".join(imports)
        + f"\n\n{body}{registry}"
    )
