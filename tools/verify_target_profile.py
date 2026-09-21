#!/usr/bin/env python3
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PROFILE_PATH = ROOT / "compat" / "targets" / "hyperos-17.03.260226.r.json"
PROBE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "combinedstatus" / "xposed" / "SystemUiCompatibilityProbe.kt"

HEX_LENGTHS = {"md5": 32, "sha1": 40, "sha256": 64}


def fail(message: str) -> None:
    print(f"Target profile check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def validate_artifact(name: str, artifact: dict) -> None:
    for key, length in HEX_LENGTHS.items():
        value = artifact.get(key, "")
        if not re.fullmatch(rf"[0-9a-f]{{{length}}}", value):
            fail(f"{name}.{key} is not a normalized {length}-character hex digest")
    for key in ("sizeBytes", "dexCount", "classCount"):
        value = artifact.get(key)
        if not isinstance(value, int) or value <= 0:
            fail(f"{name}.{key} must be a positive integer")


profile = json.loads(PROFILE_PATH.read_text(encoding="utf-8"))
if profile.get("schemaVersion") != 1:
    fail("unsupported schemaVersion")
if not profile.get("generatedFromExactApks"):
    fail("profile must be marked as generated from exact APKs")

artifacts = profile.get("artifacts", {})
for artifact_name in ("systemUi", "systemUiComponent"):
    artifact = artifacts.get(artifact_name)
    if not isinstance(artifact, dict):
        fail(f"missing artifact metadata: {artifact_name}")
    validate_artifact(artifact_name, artifact)

runtime_markers = profile.get("runtimeMarkers")
if not isinstance(runtime_markers, dict) or not runtime_markers:
    fail("runtimeMarkers is empty")

verified_systemui = set(profile.get("verifiedSystemUiClasses", []))
missing_verified = set(runtime_markers.values()) - verified_systemui
if missing_verified:
    fail("runtime markers not verified in SystemUI APK: " + ", ".join(sorted(missing_verified)))

probe_text = PROBE_PATH.read_text(encoding="utf-8")
probe_markers = dict(re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', probe_text))
if probe_markers != runtime_markers:
    fail(
        "runtime probe markers drifted from the pinned APK profile\n"
        f"profile={runtime_markers}\nprobe={probe_markers}"
    )

component_markers = profile.get("verifiedSystemUiComponentClasses", [])
if len(component_markers) < 1:
    fail("SystemUI component APK has no verified class markers")

print(f"Target profile: {profile['profileId']}")
print(
    "SystemUI: "
    f"{artifacts['systemUi']['displayName']} "
    f"md5={artifacts['systemUi']['md5']} "
    f"sha1={artifacts['systemUi']['sha1']} "
    f"dex={artifacts['systemUi']['dexCount']} "
    f"classes={artifacts['systemUi']['classCount']}"
)
print(
    "SystemUI component: "
    f"{artifacts['systemUiComponent']['displayName']} "
    f"md5={artifacts['systemUiComponent']['md5']} "
    f"sha1={artifacts['systemUiComponent']['sha1']} "
    f"dex={artifacts['systemUiComponent']['dexCount']} "
    f"classes={artifacts['systemUiComponent']['classCount']}"
)
print(f"Runtime markers: {len(runtime_markers)}/{len(runtime_markers)}")
print(f"Component markers: {len(component_markers)}/{len(component_markers)}")
