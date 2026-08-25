package com.ryusgua.app;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.Button;

import java.util.List;

/** Exposes the Canvas UI as virtual buttons for TalkBack and switch access. */
final class GuaAccessibilityProvider extends AccessibilityNodeProvider {
    private static final int HOST_ID = View.NO_ID;
    private final GuaView view;
    private int accessibilityFocusId = HOST_ID;

    GuaAccessibilityProvider(GuaView view) {
        this.view = view;
    }

    @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        if (virtualViewId == HOST_ID) return hostNode();
        GuaView.AccessibleNode spec = find(virtualViewId);
        if (spec == null) return null;
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        info.setPackageName(view.getContext().getPackageName());
        info.setClassName(Button.class.getName());
        info.setSource(view, spec.id);
        info.setParent(view);
        info.setText(spec.label);
        info.setContentDescription(spec.label);
        info.setEnabled(spec.enabled);
        info.setClickable(spec.enabled);
        info.setFocusable(true);
        info.setVisibleToUser(view.isShown());
        if (spec.enabled) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        if (accessibilityFocusId == spec.id) {
            info.setAccessibilityFocused(true);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
        } else {
            info.setAccessibilityFocused(false);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
        }
        RectF source = spec.rect;
        float min = dp(48);
        float dx = Math.max(0f, (min - source.width()) / 2f);
        float dy = Math.max(0f, (min - source.height()) / 2f);
        Rect bounds = new Rect(
                Math.round(source.left - dx + view.accessibilityInsetLeft()),
                Math.round(source.top - dy + view.accessibilityInsetTop()),
                Math.round(source.right + dx + view.accessibilityInsetLeft()),
                Math.round(source.bottom + dy + view.accessibilityInsetTop()));
        info.setBoundsInParent(bounds);
        return info;
    }

    @Override public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        if (virtualViewId == HOST_ID) return view.performAccessibilityAction(action, arguments);
        if (action == AccessibilityNodeInfo.ACTION_CLICK) {
            boolean handled = view.performAccessibleClick(virtualViewId);
            if (handled) sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
            return handled;
        }
        if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
            if (accessibilityFocusId == virtualViewId) return false;
            int previous = accessibilityFocusId;
            accessibilityFocusId = virtualViewId;
            if (previous != HOST_ID) sendEvent(previous, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            view.invalidate();
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS
                && accessibilityFocusId == virtualViewId) {
            accessibilityFocusId = HOST_ID;
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            view.invalidate();
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo hostNode() {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(view);
        info.setPackageName(view.getContext().getPackageName());
        info.setClassName(View.class.getName());
        info.setContentDescription(view.accessibilityScreenLabel());
        info.setFocusable(true);
        List<GuaView.AccessibleNode> nodes = view.accessibilityNodes();
        for (GuaView.AccessibleNode node : nodes) info.addChild(view, node.id);
        return info;
    }

    private GuaView.AccessibleNode find(int id) {
        for (GuaView.AccessibleNode node : view.accessibilityNodes()) if (node.id == id) return node;
        return null;
    }

    private void sendEvent(int id, int type) {
        ViewParent parent = view.getParent();
        if (parent == null) return;
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        event.setPackageName(view.getContext().getPackageName());
        event.setClassName(Button.class.getName());
        GuaView.AccessibleNode node = find(id);
        if (node != null) event.getText().add(node.label);
        event.setSource(view, id);
        parent.requestSendAccessibilityEvent(view, event);
    }

    private float dp(float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
