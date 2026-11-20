package dev.madusha.shangos;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The soft entrance of a page: its parts fade in while sliding a little from the side the tab bar
 * moved towards. Real element bounds are animated, not two full-screen textures blended, which
 * keeps the entrance within a small TV chip's fill-rate budget.
 */
final class PageMotion {
    private final Ui ui;
    private final ViewGroup content;
    private final ArrayList<Animator> running = new ArrayList<>();

    PageMotion(Ui ui, ViewGroup content) {
        this.ui = ui;
        this.content = content;
    }

    /** Stops every entrance that is still running (the parts are reset by the caller). */
    @MainThread
    void cancel() {
        for (Animator animation : running) animation.cancel();
        running.clear();
    }

    /**
     * Brings the page in. {@code extra} are parts that are not children of the content view (the
     * tiles and messages of Home and Apps); {@code direction} is +1 to come from the right, -1 from
     * the left; {@code quick}: browsing with the tab bar, so the page changes at once (150 ms).
     */
    @MainThread
    void enter(@NonNull List<View> extra, int direction, boolean quick) {
        ArrayList<View> entering = new ArrayList<>();
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (!(child instanceof ScrollView) && !"scroll-frame".equals(child.getTag()))
                entering.add(child);
        }
        entering.addAll(extra);
        float offset = ui.px(16) * direction;
        for (int i = 0; i < entering.size(); i++) {
            View view = entering.get(i);
            view.setAlpha(0);
            view.setTranslationX(offset);
            ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0, 1);
            fade.setDuration(quick ? 150 : 270);
            ObjectAnimator slide = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, offset, 0);
            slide.setDuration(quick ? 200 : 340);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(fade, slide);
            set.setInterpolator(Motion.EASE);
            set.setStartDelay(Math.min(i, 5) * 10);
            running.add(set);
            set.start();
        }
    }
}
