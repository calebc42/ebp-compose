#!/usr/bin/env python3
"""Hermetic CLI tests for the renderer-extension vocabulary generator."""

import json
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
GENERATOR = ROOT / "tools" / "gen-renderer-extension-vocabulary.py"


def example_manifest() -> dict:
    """Return a product-neutral manifest exercising every projection map."""
    return {
        "format": 1,
        "extension": "example.controls",
        "description": "Example controls design implementation.",
        "node_types": ["example.action", "example.choice"],
        "targets": {
            "app": ["example.action", "example.choice"],
            "dialog": ["example.action"],
            "notification": [],
        },
        "node_schema": {
            "example.action": {
                "required": ["label", "on_tap"],
                "optional": ["enabled"],
            },
            "example.choice": {
                "required": ["label", "options", "selected", "on_change"],
                "optional": ["description", "pinned"],
            },
        },
        "stateful_when_present": {"example.choice": "selected"},
        "at_least_one_non_empty": {"example.action": ["label"]},
        "selection_options": {
            "example.choice": {
                "options_member": "options",
                "selection_member": "selected",
                "label_member": "name",
                "value_member": "key",
                "required_option_members": {"level": "integer-1-6"},
                "optional_option_members": {"detail": "string"},
            },
        },
        "true_requires_parent": {
            "example.choice": {
                "member": "pinned",
                "parent_types": ["lazy_column"],
            },
        },
        "lazy_column_sticky_members": {
            "example.choice": "pinned",
        },
    }


def format_two_manifest() -> dict:
    """Return a format-2 owner manifest exercising the closed type algebra."""
    return {
        "format": 2,
        "extension": "example.typed",
        "description": "Closed typed renderer extension fixture.",
        "node_types": ["example.typed"],
        "targets": {"app": ["example.typed"], "dialog": []},
        "definitions": {
            "short-string": {
                "type": "string",
                "min_length": 1,
                "max_length": 32,
            },
            "label": {"type": "ref", "ref": "short-string"},
            "option": {
                "type": "object",
                "required": {
                    "label": {"type": "ref", "ref": "label"},
                    "value": {"type": "ref", "ref": "short-string"},
                },
                "optional": {"enabled": {"type": "boolean"}},
            },
        },
        "node_schema": {
            "example.typed": {
                "required": {
                    "flag": {"type": "boolean"},
                    "count": {"type": "integer", "minimum": 0, "maximum": 8},
                    "ratio": {"type": "number", "minimum": 0.0, "maximum": 1.0},
                    "label": {"type": "ref", "ref": "label"},
                    "mode": {"type": "enum", "values": ["quiet", "loud"]},
                    "settings": {
                        "type": "object",
                        "required": {"name": {"type": "ref", "ref": "label"}},
                        "optional": {"active": {"type": "boolean"}},
                    },
                    "options": {
                        "type": "array",
                        "items": {"type": "ref", "ref": "option"},
                        "min_items": 1,
                        "max_items": 16,
                    },
                    "tokens": {
                        "type": "identifier-map",
                        "values": {"type": "ref", "ref": "short-string"},
                        "min_entries": 0,
                        "max_entries": 64,
                    },
                    "content": {"type": "node"},
                    "children": {
                        "type": "node-array",
                        "min_items": 0,
                        "max_items": 128,
                    },
                    "on_tap": {"type": "action"},
                },
                "optional": {"swipe_end": {"type": "swipe-side"}},
            },
        },
        "stateful_when_present": {},
        "at_least_one_non_empty": {},
    }


class RendererExtensionGeneratorTest(unittest.TestCase):
    """Pin arbitrary projection, manifest validation, and drift checking."""

    def run_generator(self, *arguments: object) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(GENERATOR), *(str(value) for value in arguments)],
            cwd="/",
            text=True,
            capture_output=True,
            check=False,
        )

    def test_projects_an_arbitrary_manifest_and_checks_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            manifest = temp / "example-controls.json"
            kotlin = temp / "ExampleControlsVocabulary.kt"
            elisp = temp / "example-controls-vocabulary.el"
            manifest.write_text(json.dumps(example_manifest()), encoding="utf-8")

            arguments = (
                manifest,
                "--kotlin-output",
                kotlin,
                "--kotlin-package",
                "com.example.render",
                "--elisp-output",
                elisp,
            )
            generated = self.run_generator(*arguments)
            self.assertEqual(0, generated.returncode, generated.stderr)

            kotlin_text = kotlin.read_text(encoding="utf-8")
            self.assertIn("package com.example.render", kotlin_text)
            self.assertIn(
                'const val EXAMPLE_CONTROLS_EXTENSION = "example.controls"',
                kotlin_text,
            )
            self.assertIn("ExampleControlsVocabularyTest", kotlin_text)
            self.assertIn(
                '"example.choice" to SelectionOptionsRule('
                '"options", "selected", "name", "key", '
                'requiredOptionMembers = mapOf("level" to "integer-1-6"), '
                'optionalOptionMembers = mapOf("detail" to "string"))',
                kotlin_text,
            )
            self.assertIn(
                '"example.choice" to TrueRequiresParentRule('
                '"pinned", setOf("lazy_column"))',
                kotlin_text,
            )
            self.assertIn(
                "EXAMPLE_CONTROLS_LAZY_COLUMN_STICKY_MEMBERS",
                kotlin_text,
            )
            self.assertIn(
                '"example.choice" to "pinned"',
                kotlin_text,
            )

            elisp_text = elisp.read_text(encoding="utf-8")
            self.assertIn(
                '(defconst example-controls-extension "example.controls"',
                elisp_text,
            )
            self.assertIn("Example Controls node schemas", elisp_text)
            self.assertIn("implemented by Example's Controls", elisp_text)
            self.assertIn("(provide 'example-controls-vocabulary)", elisp_text)

            checked = self.run_generator(*arguments, "--check")
            self.assertEqual(0, checked.returncode, checked.stderr)
            kotlin.write_text(f"{kotlin_text}// stale\n", encoding="utf-8")
            stale = self.run_generator(*arguments, "--check")
            self.assertEqual(1, stale.returncode)
            self.assertIn("stale generated file", stale.stderr)

    def test_projects_equivalent_closed_format_two_schemas(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            manifest = temp / "example-typed.json"
            kotlin = temp / "ExampleTypedVocabulary.kt"
            elisp = temp / "example-typed-vocabulary.el"
            manifest.write_text(
                json.dumps(format_two_manifest()), encoding="utf-8"
            )
            arguments = (
                manifest,
                "--kotlin-output",
                kotlin,
                "--kotlin-package",
                "com.example.typed",
                "--elisp-output",
                elisp,
            )
            generated = self.run_generator(*arguments)
            self.assertEqual(0, generated.returncode, generated.stderr)

            kotlin_text = kotlin.read_text(encoding="utf-8")
            elisp_text = elisp.read_text(encoding="utf-8")
            for spelling in (
                "BooleanValue",
                "IntegerValue",
                "NumberValue",
                "StringValue",
                "EnumValue",
                "ObjectValue",
                "ArrayValue",
                "IdentifierMapValue",
                "NodeValue",
                "NodeArrayValue",
                "ActionDescriptorValue",
                "SwipeSideValue",
            ):
                self.assertIn(spelling, kotlin_text)
            for spelling in (
                ":type boolean",
                ":type integer",
                ":type number",
                ":type string",
                ":type enum",
                ":type object",
                ":type array",
                ":type identifier-map",
                ":type node",
                ":type node-array",
                ":type action",
                ":type swipe-side",
            ):
                self.assertIn(spelling, elisp_text)

            kotlin_digest = re.search(
                r'SCHEMA_SHA256 = "([0-9a-f]{64})"', kotlin_text
            )
            elisp_digest = re.search(
                r'-schema-sha256 "([0-9a-f]{64})"', elisp_text
            )
            self.assertIsNotNone(kotlin_digest)
            self.assertIsNotNone(elisp_digest)
            self.assertEqual(kotlin_digest.group(1), elisp_digest.group(1))
            checked = self.run_generator(*arguments, "--check")
            self.assertEqual(0, checked.returncode, checked.stderr)

    def assert_format_two_error(
        self,
        mutate,
        expected: str,
    ) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = format_two_manifest()
            mutate(authored)
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn(expected, result.stderr)

    def test_format_two_binds_swipe_sides_to_their_member_names(self) -> None:
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["optional"]
            .__setitem__("sideways", {"type": "swipe-side"}),
            "manifest.node_schema['example.typed'].optional['sideways'] types "
            "a swipe side, which only a swipe_start or swipe_end member may carry",
        )
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["optional"]
            .__setitem__(
                "swipe_start",
                {
                    "type": "array",
                    "items": {"type": "swipe-side"},
                    "min_items": 0,
                    "max_items": 2,
                },
            ),
            "manifest.node_schema['example.typed'].optional['swipe_start'].items "
            "must not be a swipe side",
        )

    def test_format_two_rejects_unresolved_and_cyclic_references(self) -> None:
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            .__setitem__("label", {"type": "ref", "ref": "missing"}),
            "manifest.node_schema['example.typed'].required['label'].ref "
            "has unresolved reference 'missing'",
        )

        def cycle(value: dict) -> None:
            value["definitions"]["cycle-a"] = {
                "type": "ref",
                "ref": "cycle-b",
            }
            value["definitions"]["cycle-b"] = {
                "type": "ref",
                "ref": "cycle-a",
            }

        self.assert_format_two_error(cycle, "has cyclic reference: cycle-a -> cycle-b -> cycle-a")

    def test_format_two_requires_all_collection_bounds(self) -> None:
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["options"].pop("max_items"),
            "manifest.node_schema['example.typed'].required['options'] "
            "is missing required members: ['max_items']",
        )
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["tokens"].pop("min_entries"),
            "manifest.node_schema['example.typed'].required['tokens'] "
            "is missing required members: ['min_entries']",
        )

    def test_format_two_rejects_closed_object_violations_and_overlaps(self) -> None:
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["flag"].__setitem__("nullable", True),
            "manifest.node_schema['example.typed'].required['flag'] is closed; "
            "unknown members: ['nullable']",
        )

        def overlap(value: dict) -> None:
            obj = value["node_schema"]["example.typed"]["required"]["settings"]
            obj["optional"]["name"] = {"type": "boolean"}

        self.assert_format_two_error(
            overlap,
            "manifest.node_schema['example.typed'].required['settings'] "
            "repeats members across required and optional: ['name']",
        )
        self.assert_format_two_error(
            lambda value: value.__setitem__("unexpected", True),
            "manifest is closed; unknown members: ['unexpected']",
        )

    def test_format_two_rejects_invalid_scalar_and_collection_limits(self) -> None:
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["count"].__setitem__("minimum", 9),
            "manifest.node_schema['example.typed'].required['count'].minimum "
            "must not exceed maximum",
        )
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["label"].__setitem__("type", "string"),
            "manifest.node_schema['example.typed'].required['label'] is missing "
            "required members: ['max_length', 'min_length']",
        )
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["children"].__setitem__("max_items", 10_001),
            "manifest.node_schema['example.typed'].required['children'].max_items "
            "must be an integer in 0..10000",
        )

    def test_format_two_rejects_node_member_overlap_and_unsupported_types(self) -> None:
        def overlap(value: dict) -> None:
            node = value["node_schema"]["example.typed"]
            node["optional"]["flag"] = {"type": "boolean"}

        self.assert_format_two_error(
            overlap,
            "manifest.node_schema['example.typed'] repeats members across "
            "required and optional: ['flag']",
        )
        self.assert_format_two_error(
            lambda value: value["node_schema"]["example.typed"]["required"]
            ["flag"].__setitem__("type", "anything"),
            "manifest.node_schema['example.typed'].required['flag'].type has "
            "unsupported value 'anything'",
        )

    def test_manifest_requires_explicit_outputs(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "example-controls.json"
            manifest.write_text(json.dumps(example_manifest()), encoding="utf-8")
            result = self.run_generator(manifest)
            self.assertEqual(2, result.returncode)
            self.assertIn("--kotlin-output", result.stderr)

    def test_rejects_an_ambiguous_schema(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["node_types"].append("example.action")
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("must not contain duplicates", result.stderr)

    def test_rejects_selection_options_that_name_unknown_members(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["selection_options"]["example.choice"][
                "selection_member"
            ] = "missing"
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("names unknown member 'missing'", result.stderr)

    def test_rejects_parent_rule_that_names_a_required_member(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["true_requires_parent"]["example.choice"][
                "member"
            ] = "label"
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("must name an optional schema member", result.stderr)

    def test_rejects_parent_rule_without_a_parent(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["true_requires_parent"]["example.choice"][
                "parent_types"
            ] = []
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("parent_types must not be empty", result.stderr)

    def test_rejects_unsupported_typed_option_member(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["selection_options"]["example.choice"][
                "required_option_members"
            ]["level"] = "integer-1-99"
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("unsupported scalar type 'integer-1-99'", result.stderr)

    def test_rejects_sticky_member_without_matching_parent_rule(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            authored = example_manifest()
            authored["lazy_column_sticky_members"]["example.choice"] = "other"
            manifest = temp / "invalid.json"
            manifest.write_text(json.dumps(authored), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output",
                temp / "Invalid.kt",
                "--kotlin-package",
                "com.example.invalid",
                "--elisp-output",
                temp / "invalid.el",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("must match that node's true_requires_parent", result.stderr)


class RendererExtensionMemberProjectionTest(unittest.TestCase):
    """Pin the typed NodeMemberSet/enum projection of both manifest formats."""

    CONTRACT = ROOT.parent / "ebp" / "contract.json"

    def run_generator(self, *arguments: object) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(GENERATOR), *(str(value) for value in arguments)],
            cwd="/",
            text=True,
            capture_output=True,
            check=False,
        )

    def test_format_two_projects_typed_members_and_enums(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            manifest = temp / "example-typed.json"
            manifest.write_text(json.dumps(format_two_manifest()), encoding="utf-8")
            members = temp / "ExampleTypedMembers.kt"
            enums = temp / "ExampleTypedEnums.kt"
            arguments = (
                manifest,
                "--kotlin-output", temp / "ExampleTypedVocabulary.kt",
                "--kotlin-package", "com.example.typed",
                "--elisp-output", temp / "example-typed-vocabulary.el",
                "--members-output", members,
                "--enums-output", enums,
            )
            generated = self.run_generator(*arguments)
            self.assertEqual(0, generated.returncode, generated.stderr)
            members_text = members.read_text(encoding="utf-8")
            enums_text = enums.read_text(encoding="utf-8")
            self.assertIn("package com.example.typed", members_text)
            self.assertIn("_NODE_MEMBERS: Map<String, NodeMemberSet>", members_text)
            self.assertIn("_ENUMS: Map<String, EnumDomain<*>>", enums_text)
            for spelling in (
                "ActionMember(", "NodeArrayMember(", "ChildNodeMember(",
                "ObjectMember(", "ArrayMember(", "override val required",
                'ObjectMember("swipe_end", required = false, tag = "swipe-object")',
            ):
                self.assertIn(spelling, members_text)
            self.assertNotIn("untyped", members_text)
            checked = self.run_generator(*arguments, "--check")
            self.assertEqual(0, checked.returncode, checked.stderr)
            members.write_text(members_text + "\n// stale\n", encoding="utf-8")
            stale = self.run_generator(*arguments, "--check")
            self.assertEqual(1, stale.returncode)
            self.assertIn("ExampleTypedMembers.kt", stale.stderr)

    def test_members_output_requires_its_enums_twin(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            manifest = temp / "example.json"
            manifest.write_text(json.dumps(example_manifest()), encoding="utf-8")
            result = self.run_generator(
                manifest,
                "--kotlin-output", temp / "V.kt",
                "--kotlin-package", "com.example.controls",
                "--elisp-output", temp / "v.el",
                "--members-output", temp / "M.kt",
            )
            self.assertEqual(2, result.returncode)
            self.assertIn("go together", result.stderr)

    def test_format_one_members_derive_by_name_and_fall_back_untyped(self) -> None:
        if not self.CONTRACT.is_file():
            self.skipTest("sibling ebp/contract.json not available")
        with tempfile.TemporaryDirectory() as directory:
            temp = Path(directory)
            manifest = temp / "example.json"
            manifest.write_text(json.dumps(example_manifest()), encoding="utf-8")
            members = temp / "ExampleControlsMembers.kt"
            arguments = (
                manifest,
                "--kotlin-output", temp / "ExampleControlsVocabulary.kt",
                "--kotlin-package", "com.example.controls",
                "--elisp-output", temp / "example-controls-vocabulary.el",
                "--members-output", members,
                "--enums-output", temp / "ExampleControlsEnums.kt",
                "--contract", self.CONTRACT,
            )
            generated = self.run_generator(*arguments)
            self.assertEqual(0, generated.returncode, generated.stderr)
            self.assertIn("untyped members", generated.stdout)
            text = members.read_text(encoding="utf-8")
            self.assertIn('StringMember("label", required = true)', text)
            self.assertIn('ActionMember("on_tap", required = true)', text)
            self.assertIn('ArrayMember("options", required = true, tag = "enum-option-array")', text)
            self.assertIn('AnyMember("pinned", required = false, tag = "untyped")', text)
            # `selected` is `varies-per-node` in the core contract, so by bare
            # name it is untyped too; strict mode rejects the first such member.
            self.assertIn('AnyMember("selected", required = true, tag = "untyped")', text)
            strict = self.run_generator(*arguments, "--strict-members")
            self.assertEqual(2, strict.returncode)
            self.assertIn("example.choice.selected", strict.stderr)
            self.assertIn("has no field type", strict.stderr)


if __name__ == "__main__":
    unittest.main()
