#!/usr/bin/env python3
"""Hermetic CLI tests for the renderer-extension vocabulary generator."""

import json
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


if __name__ == "__main__":
    unittest.main()
