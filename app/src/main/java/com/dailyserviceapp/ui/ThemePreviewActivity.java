package com.dailyserviceapp.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Visual preview screen for comparing three theme directions before rollout.
 */
public class ThemePreviewActivity extends BaseActivity {

    public static Intent createIntent(android.content.Context context) {
        return new Intent(context, ThemePreviewActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_preview);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Theme Demo", true);

        MaterialCardView optionOne = findViewById(R.id.cardOptionOne);
        MaterialCardView optionTwo = findViewById(R.id.cardOptionTwo);
        MaterialCardView optionThree = findViewById(R.id.cardOptionThree);

        MaterialButton pickOne = findViewById(R.id.btnPickOptionOne);
        MaterialButton pickTwo = findViewById(R.id.btnPickOptionTwo);
        MaterialButton pickThree = findViewById(R.id.btnPickOptionThree);

        pickOne.setOnClickListener(v -> showToast("Option 1 selected: Emerald + Ivory + Ink"));
        pickTwo.setOnClickListener(v -> showToast("Option 2 selected: Royal Blue + Sand + Charcoal"));
        pickThree.setOnClickListener(v -> showToast("Option 3 selected: Forest + Copper + Mist"));

        animateCard(optionOne, 0L);
        animateCard(optionTwo, 110L);
        animateCard(optionThree, 220L);
    }

    private void animateCard(View view, long startDelay) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setTranslationY(56f);
        view.setScaleX(0.97f);
        view.setScaleY(0.97f);

        ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        ObjectAnimator lift = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 56f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.97f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.97f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, lift, scaleX, scaleY);
        set.setDuration(560L);
        set.setStartDelay(startDelay);
        set.setInterpolator(new OvershootInterpolator(0.85f));
        set.start();
    }
}
