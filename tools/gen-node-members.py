#!/usr/bin/env python3
"""Generate the core node member descriptors and enum classes from ebp/contract.json.

Writes two Kotlin files into `:renderer:model`:

    renderer/model/src/commonMain/kotlin/com/calebc42/ebp/renderer/model/vocabulary/generated/
        CoreNodeEnums.kt    one `enum class` per enum domain a node member uses
        CoreNodeMembers.kt  one `NodeMemberSet` object per node type, plus
                            `UniversalMembers` and the `CORE_NODE_MEMBERS` map

The derivation (amendment #192, SPEC §17.1) lives in ebp_member_derivation.py
and is shared with the renderer-extension generator. Run from the ebp-compose
root:

    python3 tools/gen-node-members.py

Use ``--check`` in verification to fail without rewriting a stale projection;
`CoreNodeMembersDriftTest` re-reads the contract and pins the committed output.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from ebp_member_derivation import (  # noqa: E402
    ContractDerivation,
    DerivationError,
    MemberDecl,
    NodeMembersDecl,
    enum_class_name,
    members_object_name,
    render_enums_kotlin,
    render_members_kotlin,
)

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = "com.calebc42.ebp.renderer.model.vocabulary.generated"
DEFAULT_OUTPUT_DIR = ROOT / "renderer/model/src/commonMain/kotlin/com/calebc42/ebp/renderer/model/vocabulary/generated"


def build(contract_path: Path) -> tuple[str, str, int, int]:
    derivation = ContractDerivation.load(contract_path)
    contract = derivation.contract
    header = (
        f"// GENERATED from ebp/contract.json (contract_format "
        f"{contract['contract_format']}, spec {contract['spec_version']}) by\n"
        "// tools/gen-node-members.py. DO NOT EDIT: CoreNodeMembersDriftTest pins it.\n"
    )
    domains = derivation.enum_domains_in_scope()
    class_name_for = {key: enum_class_name(key) for key in domains}
    if len(set(class_name_for.values())) != len(class_name_for):
        raise DerivationError("enum class names collide")
    enums = render_enums_kotlin(PACKAGE, domains, "CORE_NODE_ENUMS", header, class_name_for)

    node_sets: list[NodeMembersDecl] = []
    for node_type in contract["node_types"]:
        required, optional = derivation.node_members(node_type)
        decl = NodeMembersDecl(node_type, members_object_name(node_type))
        for member in required:
            decl.members.append(MemberDecl(member, True, derivation.resolve(node_type, member),
                                           derivation.contract_default(node_type, member)))
        for member in optional:
            decl.members.append(MemberDecl(member, False, derivation.resolve(node_type, member),
                                           derivation.contract_default(node_type, member)))
        node_sets.append(decl)
    universal = NodeMembersDecl("*", "UniversalMembers")
    for member in derivation.universal:
        universal.members.append(MemberDecl(member, False, derivation.resolve(None, member)))
    node_sets.append(universal)
    members = render_members_kotlin(PACKAGE, node_sets, "CORE_NODE_MEMBERS", header, class_name_for)
    return enums, members, len(domains), len(node_sets) - 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec-dir", type=Path, default=ROOT.parent / "ebp",
                        help="canonical EBP specification checkout (default: sibling ../ebp)")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR,
                        help="directory receiving CoreNodeEnums.kt and CoreNodeMembers.kt")
    parser.add_argument("--check", action="store_true",
                        help="fail instead of writing when a committed projection is stale")
    args = parser.parse_args(argv)
    try:
        enums, members, n_domains, n_nodes = build(args.spec_dir.resolve() / "contract.json")
    except DerivationError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    outputs = {
        args.output_dir / "CoreNodeEnums.kt": enums,
        args.output_dir / "CoreNodeMembers.kt": members,
    }
    stale = [p for p, body in outputs.items()
             if not p.exists() or p.read_text(encoding="utf-8") != body]
    if args.check:
        if stale:
            for p in stale:
                print(f"stale generated file: {p}", file=sys.stderr)
            return 1
        print(f"checked {args.output_dir}: {n_nodes} nodes, {n_domains} enum domains")
        return 0
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for p, body in outputs.items():
        p.write_text(body, encoding="utf-8")
    print(f"wrote {args.output_dir}: {n_nodes} nodes, {n_domains} enum domains")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
