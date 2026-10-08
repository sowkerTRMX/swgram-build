package org.nqe.sw;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SwCore {
    public static Context ctx;
    static View cur;
    static float dx0, dy0;

    public static SharedPreferences sp() { return ctx.getSharedPreferences("swgram", 0); }
    public static boolean on(String k) { return ctx != null && sp().getBoolean(k, k.equals("jelly")); }

    public static void init(Application a) {
        ctx = a.getApplicationContext();
        a.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity x, Bundle b) { hook(x); secure(x); }
            public void onActivityStarted(Activity x) {}
            public void onActivityResumed(Activity x) { secure(x); }
            public void onActivityPaused(Activity x) {}
            public void onActivityStopped(Activity x) {}
            public void onActivitySaveInstanceState(Activity x, Bundle b) {}
            public void onActivityDestroyed(Activity x) {}
        });
    }

    static void secure(Activity x) {
        try { if (on("noshot")) x.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); } catch (Throwable t) {}
    }

    static void hook(final Activity x) {
        try {
            final Window w = x.getWindow();
            final Window.Callback o = w.getCallback();
            if (o == null) return;
            w.setCallback((Window.Callback) Proxy.newProxyInstance(Window.Callback.class.getClassLoader(), new Class[]{Window.Callback.class}, new InvocationHandler() {
                public Object invoke(Object p, Method m, Object[] a) throws Throwable {
                    if (a != null && a.length == 1 && a[0] instanceof MotionEvent && m.getName().equals("dispatchTouchEvent")) {
                        try { jelly(x, (MotionEvent) a[0]); } catch (Throwable t) {}
                    }
                    try { return m.invoke(o, a); } catch (InvocationTargetException e) { throw e.getCause(); }
                }
            }));
        } catch (Throwable t) {}
    }

    static View hit(View v, float x, float y) {
        if (v.getVisibility() != View.VISIBLE) return null;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View c = g.getChildAt(i);
                float cx = x + g.getScrollX() - c.getLeft() - c.getTranslationX();
                float cy = y + g.getScrollY() - c.getTop() - c.getTranslationY();
                if (cx >= 0 && cy >= 0 && cx < c.getWidth() && cy < c.getHeight()) {
                    View r = hit(c, cx, cy);
                    if (r != null) return r;
                }
            }
        }
        return v.isClickable() ? v : null;
    }

    static void jelly(Activity act, MotionEvent e) {
        if (!on("jelly")) return;
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            View d = act.getWindow().getDecorView();
            View v = hit(d, e.getX(), e.getY());
            if (v != null && Math.abs(v.getScaleX() - 1f) < 0.01f && v.getWidth() < d.getWidth() * 0.9f && v.getHeight() < d.getHeight() * 0.4f) {
                cur = v; dx0 = e.getX(); dy0 = e.getY();
                v.animate().cancel();
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(110).start();
            } else cur = null;
        } else if (cur != null && a == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - dx0, dy = e.getY() - dy0;
            cur.setScaleX(0.9f + Math.min(0.2f, Math.abs(dx) / 700f));
            cur.setScaleY(0.9f + Math.min(0.2f, Math.abs(dy) / 700f));
        } else if (cur != null && (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)) {
            cur.animate().cancel();
            cur.animate().scaleX(1f).scaleY(1f).setDuration(520).setInterpolator(new OvershootInterpolator(5f)).start();
            cur = null;
        }
    }

    public static boolean block(Object o) {
        if (ctx == null || o == null) return false;
        String n = o.getClass().getSimpleName();
        if (on("ghost") && (n.equals("TL_messages_readHistory") || n.equals("TL_channels_readHistory") || n.equals("TL_messages_readMessageContents") || n.equals("TL_channels_readMessageContents"))) return true;
        if (on("ghoststory") && (n.equals("TL_stories_readStories") || n.equals("TL_stories_incrementStoryViews"))) return true;
        if (on("ghosttype") && n.equals("TL_messages_setTyping")) return true;
        if (on("offline") && n.equals("TL_account_updateStatus")) {
            try { return !o.getClass().getField("offline").getBoolean(o); } catch (Throwable t) {}
        }
        return false;
    }
}
