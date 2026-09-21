#!/usr/bin/env python3
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PROFILE_PATH = ROOT / "compat" / "targets" / "hyperos-17.03.260226.r.json"
PROBE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "combinedstatus" / "xposed" / "SystemUiCompatibilityProbe.kt"
STATUS_HOST_CAPTURE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "combinedstatus" / "xposed" / "StatusBarHostCapture.kt"
NATIVE_STATUS_INVENTORY_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "combinedstatus" / "xposed" / "SystemUiNativeStatusInventory.kt"
NETWORK_PIPELINE_PROBE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "combinedstatus" / "xposed" / "SystemUiNetworkPipelineProbe.kt"

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
if set(artifacts) != {"systemUi"}:
    fail("profile must contain only the SystemUI artifact")
validate_artifact("systemUi", artifacts["systemUi"])

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

hook_points = profile.get("hookPoints", {})
verified_methods = profile.get("verifiedSystemUiMethods", {})
for hook_name, hook_point in hook_points.items():
    if not isinstance(hook_point, dict):
        fail(f"invalid hook point: {hook_name}")
    hook_class = hook_point.get("className")
    hook_signature = f"{hook_point.get('methodName', '')}{hook_point.get('descriptor', '')}"
    if hook_point.get("sourceArtifact") != "systemUi":
        fail(f"{hook_name} must originate from the SystemUI APK")
    if hook_class not in verified_systemui:
        fail(f"{hook_name} class is not verified in the SystemUI APK")
    if hook_signature not in set(verified_methods.get(hook_class, [])):
        fail(f"{hook_name} method is not verified in the SystemUI APK")

status_hook = hook_points.get("statusHostInflated")
if not isinstance(status_hook, dict):
    fail("missing statusHostInflated hook point")

status_hook_class = status_hook.get("className")

capture_text = STATUS_HOST_CAPTURE_PATH.read_text(encoding="utf-8")
capture_class = re.search(r'HOST_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"', capture_text)
capture_method = re.search(r'HOST_READY_METHOD_NAME\s*=\s*"([^"]+)"', capture_text)
if not capture_class or not capture_method:
    fail("status host capture constants are missing")
if capture_class.group(1) != status_hook_class:
    fail("status host capture class drifted from the pinned APK profile")
if capture_method.group(1) != status_hook.get("methodName"):
    fail("status host capture method drifted from the pinned APK profile")

network_probe_text = NETWORK_PIPELINE_PROBE_PATH.read_text(encoding="utf-8")
network_hook_constants = {
    "wifiBinderBind": ("WIFI_BINDER_CLASS_NAME", "WIFI_BIND_METHOD_NAME"),
    "wifiIconCollected": ("WIFI_ICON_EMITTER_CLASS_NAME", "WIFI_ICON_EMIT_METHOD_NAME"),
    "mobileBinderBind": ("MOBILE_BINDER_CLASS_NAME", "MOBILE_BIND_METHOD_NAME"),
    "mobileSignalCollected": (
        "MOBILE_SIGNAL_EMITTER_CLASS_NAME",
        "MOBILE_SIGNAL_EMIT_METHOD_NAME",
    ),
}
for hook_name, (class_constant, method_constant) in network_hook_constants.items():
    hook_point = hook_points.get(hook_name)
    if not isinstance(hook_point, dict):
        fail(f"missing network hook point: {hook_name}")
    class_match = re.search(
        rf'{class_constant}\s*=\s*\n?\s*"([^"]+)"',
        network_probe_text,
    )
    method_match = re.search(
        rf'{method_constant}\s*=\s*"([^"]+)"',
        network_probe_text,
    )
    if not class_match or not method_match:
        fail(f"network probe constants are missing: {hook_name}")
    probe_class_name = class_match.group(1).replace("\\$", "$")
    if probe_class_name != hook_point.get("className"):
        fail(f"network probe class drifted from profile: {hook_name}")
    if method_match.group(1) != hook_point.get("methodName"):
        fail(f"network probe method drifted from profile: {hook_name}")

native_status_views = profile.get("nativeStatusViews", {})
expected_native_roles = {"mobileNetwork", "wifi", "battery"}
if set(native_status_views) != expected_native_roles:
    fail("nativeStatusViews must define mobileNetwork, wifi, and battery")
if not set(native_status_views.values()).issubset(verified_systemui):
    fail("native status view classes are not all verified in the SystemUI APK")

inventory_text = NATIVE_STATUS_INVENTORY_PATH.read_text(encoding="utf-8")
inventory_constants = {
    "mobileNetwork": re.search(
        r'MOBILE_NETWORK_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "wifi": re.search(
        r'WIFI_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "battery": re.search(
        r'BATTERY_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
}
for role, match in inventory_constants.items():
    if not match:
        fail(f"native status inventory constant is missing: {role}")
    if match.group(1) != native_status_views[role]:
        fail(f"native status inventory class drifted from profile: {role}")

native_status_containers = profile.get("nativeStatusContainers", {})
expected_native_containers = {"miuiStatusIcons", "statusIcons", "batteryContainer"}
if set(native_status_containers) != expected_native_containers:
    fail("nativeStatusContainers must define miuiStatusIcons, statusIcons, and batteryContainer")
if not set(native_status_containers.values()).issubset(verified_systemui):
    fail("native status container classes are not all verified in the SystemUI APK")

container_constants = {
    "miuiStatusIcons": re.search(
        r'(?m)^\s*const val MIUI_STATUS_ICON_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "statusIcons": re.search(
        r'(?m)^\s*const val STATUS_ICON_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "batteryContainer": re.search(
        r'(?m)^\s*const val BATTERY_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
}
for role, match in container_constants.items():
    if not match:
        fail(f"native status container constant is missing: {role}")
    if match.group(1) != native_status_containers[role]:
        fail(f"native status container class drifted from profile: {role}")


print(f"Target profile: {profile['profileId']}")
print(
    "SystemUI: "
    f"{artifacts['systemUi']['displayName']} "
    f"md5={artifacts['systemUi']['md5']} "
    f"sha1={artifacts['systemUi']['sha1']} "
    f"dex={artifacts['systemUi']['dexCount']} "
    f"classes={artifacts['systemUi']['classCount']}"
)
print(f"Runtime markers: {len(runtime_markers)}/{len(runtime_markers)}")
print(f"Verified hook points: {len(hook_points)}/{len(hook_points)}")
print(f"Native status views: {len(native_status_views)}/{len(native_status_views)}")
print(f"Native status containers: {len(native_status_containers)}/{len(native_status_containers)}")
