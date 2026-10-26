#!/usr/bin/env python3
"""Static regressions for the restored, independent Settings touch strip."""
from pathlib import Path

strip = Path("tools/dynamic/SettingsBottomStrip.java").read_text()
controller = Path("tools/dynamic/BreakPauseController.java").read_text()
dex = Path("tools/build_dynamic_dex.sh").read_text()

required = {
    "sky-blue color": "Color.rgb(135, 206, 235)",
    "bottom 5 percent": "Math.round(size.y * 0.05f)",
    "65 percent width": "Math.round(size.x * 0.65f)",
    "left-aligned bottom": "Gravity.BOTTOM | Gravity.LEFT",
    "keep partial width on resize": "params.width = width",
    "touches outside pass through": "FLAG_NOT_TOUCH_MODAL",
    "accessibility overlay": "WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY",
    "consume touches": "view.setOnTouchListener((v, event) -> true)",
    "touchable window": "view.setClickable(true)",
    "focused Settings window": "window.isFocused()",
    "Settings package": "com.android.settings",
    "session + break gate": "SettingsSessionGate.isActive()",
    "dismiss on foreground exit": "if (!settingsForeground())",
    "remove the window": "manager.removeViewImmediate(strip)",
}
for name, snippet in required.items():
    assert snippet in strip, f"Missing {name}"

assert "params.width = size.x" not in strip, "The strip must not expand to full width"
assert "FLAG_NOT_TOUCHABLE" not in strip
assert "schedulePoll(" not in strip, "Must reuse the existing service tick"
assert "postDelayed(" not in strip, "Must not introduce extra poll timers"
assert controller.count("SettingsBottomStrip.update(current)") == 3
assert controller.count("SettingsBottomStrip.detach();") == 2
assert "SettingsBottomStrip.java" in dex
print("SettingsBottomStrip regression checks passed")
