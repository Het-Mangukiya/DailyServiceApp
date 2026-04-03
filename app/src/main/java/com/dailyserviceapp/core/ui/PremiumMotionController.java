package com.dailyserviceapp.core.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashSet;

/**
 * Centralized motion system for page entry, card reveals, button press feedback,
 * and recycler row animations across BaseActivity screens.
 */
public final class PremiumMotionController {

    private static final long PAGE_DURATION_MS = 360L;
    private static final long TARGET_DURATION_MS = 420L;
    private static final float PAGE_START_TRANSLATION_DP = 14f;
    private static final float TARGET_START_TRANSLATION_DP = 24f;

    private PremiumMotionController() {
    }

    public static void bind(ViewGroup contentRoot) {
        if (contentRoot == null || contentRoot.getChildCount() == 0) return;

        View root = contentRoot.getChildAt(0);
        if (root == null || Boolean.TRUE.equals(root.getTag(R.id.tag_motion_bound))) return;

        root.setTag(R.id.tag_motion_bound, true);
        root.post(() -> animateScene(root));
    }

    public static void animateStandalone(View root) {
        if (root == null || Boolean.TRUE.equals(root.getTag(R.id.tag_motion_bound))) return;

        root.setTag(R.id.tag_motion_bound, true);
        root.post(() -> animateScene(root));
    }

    private static void animateScene(View root) {
        if (!root.isAttachedToWindow()) return;

        float rootTranslation = dp(root, PAGE_START_TRANSLATION_DP);
        root.setAlpha(0f);
        root.setTranslationY(rootTranslation);
        root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(PAGE_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator(1.5f))
            .start();

        LinkedHashSet<View> targets = new LinkedHashSet<>();
        collectTargets(root, targets);

        int index = 0;
        for (View target : targets) {
            if (target == root || !target.isAttachedToWindow() || target.getHeight() == 0) continue;
            animateTarget(target, index++);
        }
    }

    private static void collectTargets(View view, LinkedHashSet<View> targets) {
        if (view == null || view.getVisibility() != View.VISIBLE) return;

        installPressFeedback(view);

        if (view instanceof RecyclerView) {
            configureRecyclerView((RecyclerView) view);
            targets.add(view);
            return;
        }

        if (shouldAnimate(view)) {
            targets.add(view);
        }

        if (view instanceof NestedScrollView) {
            View child = ((NestedScrollView) view).getChildAt(0);
            if (child instanceof ViewGroup) {
                addVisibleChildren((ViewGroup) child, targets);
                collectTargets(child, targets);
            }
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTargets(group.getChildAt(i), targets);
            }
        }
    }

    private static void addVisibleChildren(ViewGroup group, LinkedHashSet<View> targets) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (shouldAnimateContainerChild(child)) {
                targets.add(child);
            }
        }
    }

    private static boolean shouldAnimate(View view) {
        return view instanceof MaterialCardView
            || view instanceof CardView
            || view instanceof TextInputLayout
            || view instanceof MaterialButton
            || view instanceof FloatingActionButton;
    }

    private static boolean shouldAnimateContainerChild(View view) {
        return shouldAnimate(view)
            || (view instanceof ViewGroup
            && !(view instanceof RecyclerView)
            && !(view instanceof AppBarLayout)
            && !(view instanceof MaterialToolbar)
            && view.getHeight() > 0);
    }

    private static void animateTarget(View target, int index) {
        if (Boolean.TRUE.equals(target.getTag(R.id.tag_motion_item_animated))) return;

        target.setTag(R.id.tag_motion_item_animated, true);
        float translation = dp(target, TARGET_START_TRANSLATION_DP);

        target.setAlpha(0f);
        target.setTranslationY(translation);
        target.setScaleX(0.985f);
        target.setScaleY(0.985f);

        target.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(Math.min(index * 36L, 180L))
            .setDuration(TARGET_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator(1.35f))
            .start();
    }

    private static void configureRecyclerView(RecyclerView recyclerView) {
        if (Boolean.TRUE.equals(recyclerView.getTag(R.id.tag_motion_recycler_bound))) return;

        recyclerView.setTag(R.id.tag_motion_recycler_bound, true);

        RecyclerView.ItemAnimator currentAnimator = recyclerView.getItemAnimator();
        DefaultItemAnimator itemAnimator;
        if (currentAnimator instanceof DefaultItemAnimator) {
            itemAnimator = (DefaultItemAnimator) currentAnimator;
        } else {
            itemAnimator = new DefaultItemAnimator();
            recyclerView.setItemAnimator(itemAnimator);
        }
        itemAnimator.setAddDuration(260L);
        itemAnimator.setMoveDuration(220L);
        itemAnimator.setChangeDuration(180L);

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                if (Boolean.TRUE.equals(view.getTag(R.id.tag_motion_row_animated))) return;

                view.setTag(R.id.tag_motion_row_animated, true);
                int position = recyclerView.getChildAdapterPosition(view);
                long delay = position >= 0 && position < 6 ? position * 28L : 0L;
                float translation = dp(view, 18f);

                view.setAlpha(0f);
                view.setTranslationY(translation);
                view.setScaleX(0.99f);
                view.setScaleY(0.99f);
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delay)
                    .setDuration(320L)
                    .setInterpolator(new DecelerateInterpolator(1.3f))
                    .start();
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // Keep the animation one-time per bound row to avoid noisy replays while scrolling.
            }
        });
    }

    private static void installPressFeedback(View view) {
        if (!isPressableTarget(view) || Boolean.TRUE.equals(view.getTag(R.id.tag_motion_press_bound))) return;

        view.setTag(R.id.tag_motion_press_bound, true);
        view.setStateListAnimator(buildPressAnimator(view));
    }

    private static boolean isPressableTarget(View view) {
        return view instanceof MaterialButton
            || view instanceof FloatingActionButton
            || view instanceof MaterialCardView
            || (view instanceof CardView && view.isClickable());
    }

    private static StateListAnimator buildPressAnimator(View view) {
        float pressedScale = 0.985f;
        float lift = dp(view, 3f);

        AnimatorSet pressed = new AnimatorSet();
        pressed.playTogether(
            ObjectAnimator.ofFloat(view, View.SCALE_X, pressedScale),
            ObjectAnimator.ofFloat(view, View.SCALE_Y, pressedScale),
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, lift)
        );
        pressed.setDuration(140L);
        pressed.setInterpolator(new DecelerateInterpolator(1.25f));

        AnimatorSet released = new AnimatorSet();
        released.playTogether(
            ObjectAnimator.ofFloat(view, View.SCALE_X, 1f),
            ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f),
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, 0f)
        );
        released.setDuration(180L);
        released.setInterpolator(new DecelerateInterpolator(1.25f));

        StateListAnimator stateListAnimator = new StateListAnimator();
        stateListAnimator.addState(new int[]{android.R.attr.state_pressed}, pressed);
        stateListAnimator.addState(new int[]{}, released);
        return stateListAnimator;
    }

    private static float dp(View view, float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
