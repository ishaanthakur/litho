/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation;

import static com.facebook.litho.testing.TestLithoViewKt.DEFAULT_HEIGHT_SPEC;
import static com.facebook.litho.testing.TestLithoViewKt.DEFAULT_WIDTH_SPEC;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Row;
import com.facebook.litho.StateCaller;
import com.facebook.litho.Transition;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.widget.TestAnimationMount;
import com.facebook.litho.widget.TestAnimationsComponent;
import com.facebook.litho.widget.TestAnimationsComponentSpec;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.LooperMode;

/**
 * This tests validate how different kind of animations modify the view. The values asserted here
 * are specific to the number of frames and the type of animation.
 *
 * <p>All transitions are timed to 144 ms which translates to 9 frames (we actually step 10 because
 * the first one does not count as it only initializes the values in the {@link
 * com.facebook.litho.dataflow.TimingNode}) based on {@link MockTimingSource}.FRAME_TIME_MS) which
 * is 16ms per frame.
 *
 * <p>Formula for the specific values found in this tests:
 *
 * <p>- First we calculate the timing doing: timing = (frames*frame_time - frame_time) /
 * ((frame_time + duration_ms) - frame_time)
 *
 * <p>- Then we run the AccelerateDecelerateInterpolator: fraction = cos((timing + 1) * PI / 2) +
 * 0.5 f
 *
 * <p>- Finally the actual result is: result = initial_position + fraction * (final_position -
 * initial_position)
 *
 * <p>Example for X axis animation after 5 frames: (5*16 - 16) / ((16 + 144) - 16) = 0.44444445.
 *
 * <p>AccelerateDecelareteInterpolator: cos((0.44444445 + 1) * PI / 2) + * 0.5 f = 0.4131759
 *
 * <p>Final position: 160 + 0.4131759 * (-160) = 93.891856
 */
@SuppressLint("ColorConstantUsageIssue")
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner.class)
public class AnimationTest {
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();
  private static final String TRANSITION_KEY = "TRANSITION_KEY";
  private final StateCaller mStateCaller = new StateCaller();
  private ActivityController<Activity> mActivityController;

  final boolean mDelegateToRenderCoreMount;

  boolean mConfigDelegateToRenderCoreMount;

  @ParameterizedRobolectricTestRunner.Parameters(name = "delegateToRenderCoreMount={0}")
  public static Collection data() {
    return Arrays.asList(new Object[][] {{false}, {true}});
  }

  public AnimationTest(boolean delegateToRenderCoreMount) {
    mDelegateToRenderCoreMount = delegateToRenderCoreMount;
  }

  @Before
  public void setUp() {
    mActivityController = Robolectric.buildActivity(Activity.class, new Intent());
    mConfigDelegateToRenderCoreMount = ComponentsConfiguration.delegateToRenderCoreMount;

    ComponentsConfiguration.delegateToRenderCoreMount = mDelegateToRenderCoreMount;
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.delegateToRenderCoreMount = mConfigDelegateToRenderCoreMount;
  }

  @Test
  public void animationProperties_animatingPropertyX_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component = getAnimatingXPropertyComponent();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(93.89186f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_animatingPropertyY_elementShouldAnimateInTheYAxis() {

    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.Y))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X moves without animating
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(0);
    // Y after state update should be at 160 because is going to be animated
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(160);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(0);
    // Check java doc for how we calculate this value.
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(93.89186f);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_animatingPropertyScale_elementShouldAnimateXandYScale() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.SCALE))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .scale(state ? 1 : 2)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getScaleX()).describedAs("view scale X initial position").isEqualTo(2);
    assertThat(view.getScaleY()).describedAs("view scale Y initial position").isEqualTo(2);

    mStateCaller.update();

    assertThat(view.getScaleX()).describedAs("view X axis after toggle").isEqualTo(2);
    assertThat(view.getScaleY()).describedAs("view Y axis after toggle").isEqualTo(2);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getScaleX()).describedAs("view X axis after 5 frames").isEqualTo(1.5868242f);
    assertThat(view.getScaleY()).describedAs("view Y axis after 5 frames").isEqualTo(1.5868242f);

    mTransitionTestRule.step(5);

    assertThat(view.getScaleX()).describedAs("view X axis after 10 frames").isEqualTo(1);
    assertThat(view.getScaleY()).describedAs("view Y axis after 10 frames").isEqualTo(1);
  }

  @Test
  public void animationProperties_animatingPropertyAlpha_elementShouldAnimateAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .alpha(state ? 1 : 0.5f)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getAlpha()).describedAs("view alpha initial state").isEqualTo(0.5f);

    mStateCaller.update();

    assertThat(view.getAlpha()).describedAs("view alpha after toggle").isEqualTo(0.5f);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view alpha after 5 frames").isEqualTo(0.7065879f);

    mTransitionTestRule.step(5);

    assertThat(view.getAlpha()).describedAs("view alpha after 10 frames").isEqualTo(1);
  }

  @Test
  public void animationProperties_animatingPropertyRotation_elementShouldAnimateRotation() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ROTATION))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .rotation(state ? 45 : 0)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getRotation()).describedAs("view rotation initial state").isEqualTo(0);

    mStateCaller.update();

    assertThat(view.getRotation()).describedAs("view rotation after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getRotation())
        .describedAs("view rotation after 5 frames")
        .isEqualTo(18.592915f);

    mTransitionTestRule.step(5);

    assertThat(view.getRotation()).describedAs("view rotation after 10 frames").isEqualTo(45);
  }

  @Test
  public void animationProperties_animatingPropertyHeight_elementShouldAnimateHeight() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.HEIGHT))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .child(
                            Row.create(componentContext)
                                .heightDip(state ? 80 : 40)
                                .widthDip(state ? 80 : 40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view.getHeight()).describedAs("view height initial state").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width initial state").isEqualTo(40);

    mStateCaller.update();

    assertThat(view.getHeight()).describedAs("view height after toggle").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width after toggle").isEqualTo(80);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getHeight()).describedAs("view height after 5 frames").isEqualTo(56);

    mTransitionTestRule.step(5);

    assertThat(view.getHeight()).describedAs("view height after 10 frames").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after 10 frames").isEqualTo(80);
  }

  @Test
  public void animationProperties_animatingPropertyWidth_elementShouldAnimateWidth() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.WIDTH))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .child(
                            Row.create(componentContext)
                                .heightDip(state ? 80 : 40)
                                .widthDip(state ? 80 : 40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view.getHeight()).describedAs("view height initial state").isEqualTo(40);
    assertThat(view.getWidth()).describedAs("view width initial state").isEqualTo(40);

    mStateCaller.update();

    assertThat(view.getHeight()).describedAs("view height after toggle").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after toggle").isEqualTo(40);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getWidth()).describedAs("view width after 5 frames").isEqualTo(56);

    mTransitionTestRule.step(5);

    assertThat(view.getHeight()).describedAs("view height after 10 frames").isEqualTo(80);
    assertThat(view.getWidth()).describedAs("view width after 10 frames").isEqualTo(80);
  }

  @Test
  public void animation_appearAnimation_elementShouldAppearAnimatingAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY);

    // View should be null as state is null
    assertThat(view).describedAs("view before appearing").isNull();
    mStateCaller.update();

    // After state update we should have the view added but with alpha equal to 0
    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view).describedAs("view after toggle").isNotNull();
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 5 frames").isEqualTo(0.41317588f);

    mTransitionTestRule.step(5);
    assertThat(view.getAlpha()).describedAs("view after 10 frames").isEqualTo(1);
  }

  @Test
  public void animation_disappearAnimation_elementShouldDisappearAnimatingAlpha() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 10 frames to account for the appear animation.
    mTransitionTestRule.step(10);
    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    // The view is not null
    assertThat(view).describedAs("view before disappearing").isNotNull();
    assertThat(view.getAlpha()).describedAs("view before disappearing").isEqualTo(1);
    mStateCaller.update();
    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // After state update, even if the row was removed from the component, it still not null as we
    // are going to animate it. Alpha stays at 1 before advancing frames.
    assertThat(view).describedAs("view after toggle").isNotNull();
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(1);

    mTransitionTestRule.step(5);
    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 5 frames").isEqualTo(0.5868241f);

    // We move only 4 more frames because after 5 the view should be removed from the hierarchy.
    mTransitionTestRule.step(4);
    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 10 frames").isEqualTo(0.030153751f);
    mTransitionTestRule.step(1);

    view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY);
    assertThat(view).describedAs("view after last re-measure and re-layout").isNull();
  }

  @Test
  public void
      animation_disappearAnimationWithRemountToRoot_elementShouldDisappearWithoutCrashing() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.parallel(
                    Transition.create("comment_editText")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0)
                        .animate(AnimatedProperties.X)
                        .appearFrom(DimensionValue.widthPercentageOffset(-50))
                        .disappearTo(DimensionValue.widthPercentageOffset(-50)),
                    Transition.create("cont_comment")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0),
                    Transition.create("icon_like", "icon_share").animate(AnimatedProperties.X),
                    Transition.create("text_like", "text_share")
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)
                        .disappearTo(0)
                        .animate(AnimatedProperties.X)
                        .appearFrom(DimensionValue.widthPercentageOffset(50))
                        .disappearTo(DimensionValue.widthPercentageOffset(50))))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext c, boolean state) {
                    return !state
                        ? Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .wrapInView()
                                    .testKey("like_button")
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)
                                            .transitionKey("icon_like"))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Like")
                                            .transitionKey("text_like")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .transitionKey("cont_comment")
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_share")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Share")
                                            .transitionKey("text_share")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build()
                        : Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .wrapInView()
                                    .paddingDip(YogaEdge.HORIZONTAL, 16)
                                    .testKey("like_button")
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_like")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .child(
                                Column.create(c)
                                    .flexGrow(1)
                                    .transitionKey("comment_editText")
                                    .child(Text.create(c).text("Input here").textSizeSp(16)))
                            .child(
                                Row.create(c)
                                    .transitionKey("cont_share")
                                    .alignItems(YogaAlign.CENTER)
                                    .wrapInView()
                                    .paddingDip(YogaEdge.ALL, 16)
                                    .backgroundColor(0xff0000ff)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_share")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 100 frames to be sure any appearing animation finished.
    mTransitionTestRule.step(100);

    mStateCaller.update();
    // We move an other 100 frames to be sure disappearing animations are done.
    mTransitionTestRule.step(100);

    mTransitionTestRule.step(100);

    // Do not crash.
  }

  @Test
  public void animationProperties_differentInterpolator_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144, new AccelerateInterpolator()))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value. NOTE: this is using a different interpolator
    // so the fraction is different, check AccelerateInterpolator class
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(128.39507f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animationProperties_nullInterpolator_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144, null))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(200)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .backgroundColor(Color.parseColor("#ee1111"))
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // This is not using any interpolator so after 5 frames
    // 160(movement)/144(time_frame)*5(frames)*16(frame_time)
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(88.888885f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    mTransitionTestRule.step(5);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animation_unmountingLithoViewMidAnimation_shouldNotCrash() {
    mLegacyLithoViewRule.setRoot(getAnimatingXPropertyComponent());
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(view.getX()).describedAs("view X axis after 5 frames").isEqualTo(93.89186f);
    assertThat(view.getY()).describedAs("view Y axis after 5 frames").isEqualTo(0);

    // This line would unmount the animating mountitem and the framework should stop the animation.
    mLegacyLithoViewRule.getLithoView().unmountAllItems();

    // After unmounting all items it should not crash.
    mTransitionTestRule.step(5);
  }

  @Test
  public void animation_reUsingLithoViewWithDifferentComponentTrees_shouldNotCrash() {
    ComponentContext componentContext =
        new ComponentContext(ApplicationProvider.getApplicationContext());

    mLegacyLithoViewRule.setRoot(getNonAnimatingComponent());
    // We measure and layout this non animating component to initialize the transition extension.
    mLegacyLithoViewRule.measure().layout();

    // We need an other litho view where we are going to measure and layout an other similar tree
    // (the real difference here is that the root components are not animating)
    LithoView lithoView = new LithoView(componentContext);
    ComponentTree nonAnimatingComponentTree = ComponentTree.create(componentContext).build();
    nonAnimatingComponentTree.setRoot(getNonAnimatingComponent());
    lithoView.setComponentTree(nonAnimatingComponentTree);
    lithoView.measure(DEFAULT_WIDTH_SPEC, DEFAULT_HEIGHT_SPEC);
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    // Now we need a new component tree that will hold a component tree that holds an animating root
    // component.
    ComponentTree animatingComponentTree = ComponentTree.create(componentContext).build();
    animatingComponentTree.setRoot(getAnimatingXPropertyComponent());

    mLegacyLithoViewRule.useComponentTree(animatingComponentTree);
    // We measure this component tree so we initialize the mRootTransition in the extension, but we
    // end up not running a layout here.
    mLegacyLithoViewRule.measure();

    // Finally we set a new animating component tree to the initial litho view and run measure and
    // layout.
    mLegacyLithoViewRule.useComponentTree(nonAnimatingComponentTree);
    mLegacyLithoViewRule.measure().layout();
    // Should not crash.
  }

  @Test
  public void animation_unmountParentBeforeChildDisappearAnimation_shouldNotCrash() {
    // Disabling drawable outputs to ensure a nest heirachy rather than a list of drawables.
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);

    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW)
                                .viewTag("parent_of_parent")
                                .child(
                                    Row.create(componentContext)
                                        .heightDip(25)
                                        .widthDip(25)
                                        .backgroundColor(Color.RED)
                                        .viewTag("parent") // This is the parent that will unmount
                                        .child(
                                            !state
                                                // Disappearing child
                                                ? Row.create(componentContext)
                                                    .heightDip(10)
                                                    .widthDip(10)
                                                    .backgroundColor(Color.BLUE)
                                                    .transitionKey(TRANSITION_KEY)
                                                    .viewTag(TRANSITION_KEY)
                                                : null)))
                        .build();
                  }
                })
            .build();

    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    mTransitionTestRule.step(1);

    // Grab the parent of the parent
    final ComponentHost parentOfParent =
        (ComponentHost) mLegacyLithoViewRule.findViewWithTagOrNull("parent_of_parent");

    // Grab the id of the 1st child - this is the parent we will unmount
    final long id = parentOfParent.getMountItemAt(0).getRenderTreeNode().getRenderUnit().getId();

    // Manually unmount the parent of the disappearing item
    mLegacyLithoViewRule.getLithoView().getMountDelegateTarget().notifyUnmount(id);

    // Update so the disappearing item triggers a disappear animation.
    // If there's a problem, a crash will occur here.
    mStateCaller.update();

    // Restoring disable drawable outputs configuration
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }

  @Test
  public void animation_unmountElementMidAppearAnimation_elementShouldBeUnmounted() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.stagger(
                    144,
                    Transition.create(TRANSITION_KEY + 0)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0),
                    Transition.create(TRANSITION_KEY + 1)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.ALPHA)
                        .appearFrom(0)))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    Column.Builder builder =
                        Column.create(componentContext)
                            .child(
                                Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.YELLOW));
                    if (state) {
                      for (int i = 0; i < 2; i++) {
                        builder.child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.RED)
                                .viewTag(TRANSITION_KEY + i)
                                .transitionKey(TRANSITION_KEY + i));
                      }
                    }
                    return builder.build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // The bug only happens if you start the animation in the middle twice.
    View view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY + 1);

    // View should be null as state is null
    assertThat(view).describedAs("view before appearing").isNull();
    mStateCaller.update();

    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view after toggle").isNotNull();
    // After state update we should have the view added but with alpha equal to 0
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);

    mTransitionTestRule.step(11);
    // Update state again the element should not be there.
    mStateCaller.update();

    view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view unmount mid animation").isNull();

    mTransitionTestRule.step(1);
    // Now if we do this again we expect the appearing items to the same thing.
    mStateCaller.update();
    mTransitionTestRule.step(1);
    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view after toggle").isNotNull();
    // After state update we should have the view added but with alpha equal to 0
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(0);
    mStateCaller.update();
    view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY + 1);
    assertThat(view).describedAs("view unmount mid animation").isNull();
  }

  @Test
  public void
      animation_disappearAnimationMovingAnItemToTheSameIndex_elementShouldDisappearWithoutCrashing() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create("text_like")
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0)
                    .animate(AnimatedProperties.X)
                    .appearFrom(DimensionValue.widthPercentageOffset(50))
                    .disappearTo(DimensionValue.widthPercentageOffset(50)))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext c, boolean state) {
                    return !state
                        ? Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Column.create(c)
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED))
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Like")
                                            .transitionKey("text_like")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build()
                        : Row.create(c)
                            .backgroundColor(Color.WHITE)
                            .heightDip(56)
                            .child(
                                Row.create(c)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .paddingDip(YogaEdge.HORIZONTAL, 16)
                                    .child(
                                        Column.create(c)
                                            .transitionKey("icon_like")
                                            .heightDip(24)
                                            .widthDip(24)
                                            .backgroundColor(Color.RED)))
                            .child(
                                Row.create(c)
                                    .widthPercent(33.3f)
                                    .alignItems(YogaAlign.CENTER)
                                    .justifyContent(YogaJustify.CENTER)
                                    .child(
                                        Text.create(c)
                                            .textSizeSp(16)
                                            .text("Comment")
                                            .marginDip(YogaEdge.LEFT, 8)))
                            .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 100 frames to be sure any appearing animation finished.
    mTransitionTestRule.step(100);

    mStateCaller.update();
    // We move an other 100 frames to be sure disappearing animations are done.
    mTransitionTestRule.step(100);

    mTransitionTestRule.step(100);

    // Do not crash.
  }

  @Test
  public void
      animationTransitionsExtension_reUsingLithoViewWithSameComponentTrees_shouldNotCrash() {
    ComponentContext componentContext = mLegacyLithoViewRule.getContext();

    ComponentTree animatingComponentTree = ComponentTree.create(componentContext).build();
    animatingComponentTree.setRoot(getAnimatingXPropertyComponent());

    mLegacyLithoViewRule.useComponentTree(animatingComponentTree);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    mStateCaller.update();

    mTransitionTestRule.step(5);

    LithoView lithoView1 = mLegacyLithoViewRule.getLithoView();
    lithoView1.setComponentTree(null);
    LithoView lithoView2 = new LithoView(componentContext);
    lithoView2.setComponentTree(animatingComponentTree);
    mLegacyLithoViewRule.useLithoView(lithoView2).attachToWindow().measure().layout();
    animatingComponentTree.setRoot(getNonAnimatingComponent());
    mStateCaller.update();
    mTransitionTestRule.step(1);

    lithoView2.setComponentTree(null);
    lithoView1.setComponentTree(animatingComponentTree);
    mLegacyLithoViewRule.useLithoView(lithoView1);

    mTransitionTestRule.step(1000);
  }

  @Test
  public void
      animationProperties_animatingPropertyOnRootComponent_elementShouldAnimateInTheXAxis() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X)
                    .animate(AnimatedProperties.Y)
                    .animate(AnimatedProperties.WIDTH)
                    .animate(AnimatedProperties.HEIGHT))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .heightDip(state ? 200 : 100)
                        .widthDip(state ? 200 : 100)
                        .backgroundColor(Color.RED)
                        .positionPx(YogaEdge.LEFT, !state ? 0 : 100)
                        .positionPx(YogaEdge.TOP, !state ? 0 : 100)
                        .transitionKey(TRANSITION_KEY)
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    View lithoView = mLegacyLithoViewRule.getLithoView();

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(lithoView.getX())
        .describedAs("view X axis should be at start position")
        .isEqualTo(0);
    assertThat(lithoView.getY())
        .describedAs("view Y axis should be at start position")
        .isEqualTo(0);
    assertThat(lithoView.getWidth())
        .describedAs("view Width should be at start position")
        .isEqualTo(320);
    assertThat(lithoView.getHeight())
        .describedAs("view Height should be at start position")
        .isEqualTo(422);

    mStateCaller.update();

    // X after state update should be at 0 because is going to be animated.
    assertThat(lithoView.getX()).describedAs("view X axis after toggle").isEqualTo(0);
    // Y after state update should be at 0 because is going to be animated.
    assertThat(lithoView.getY()).describedAs("view Y axis after toggle").isEqualTo(0);
    // Width after state update should be at 320 because is going to be animated.
    assertThat(lithoView.getWidth()).describedAs("view Width after toggle").isEqualTo(320);
    // Height after state update should be at 422 because is going to be animated.
    assertThat(lithoView.getHeight()).describedAs("view Height after toggle").isEqualTo(422);
    mTransitionTestRule.step(5);

    // Check java doc for how we calculate this value.
    assertThat(lithoView.getX()).describedAs("view X axis after 5 frames").isEqualTo(41.31759f);
    assertThat(lithoView.getY()).describedAs("view Y axis after 5 frames").isEqualTo(28.238356f);
    assertThat(lithoView.getWidth()).describedAs("view Width axis after 5 frames").isEqualTo(128);
    assertThat(lithoView.getHeight()).describedAs("view Height axis after 5 frames").isEqualTo(128);

    // Enough frames to finish all animations
    mTransitionTestRule.step(500);

    assertThat(lithoView.getX()).describedAs("view X axis after animation finishes").isEqualTo(100);
    assertThat(lithoView.getY()).describedAs("view Y axis after animation finishes").isEqualTo(100);
    assertThat(lithoView.getWidth())
        .describedAs("view Width after animation finishes")
        .isEqualTo(200);
    assertThat(lithoView.getHeight())
        .describedAs("view Height after animation finishes")
        .isEqualTo(200);
  }

  private TestAnimationsComponent getAnimatingXPropertyComponent() {
    return TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
        .stateCaller(mStateCaller)
        .transition(
            Transition.create(TRANSITION_KEY)
                .animator(Transition.timing(144))
                .animate(AnimatedProperties.X))
        .testComponent(
            new TestAnimationsComponentSpec
                .TestComponent() { // This could be a lambda but it fails ci.
              @Override
              public Component getComponent(ComponentContext componentContext, boolean state) {
                return Row.create(componentContext)
                    .heightDip(200)
                    .widthDip(200)
                    .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                    .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                    .child(
                        Row.create(componentContext)
                            .heightDip(40)
                            .widthDip(40)
                            .backgroundColor(Color.parseColor("#ee1111"))
                            .transitionKey(TRANSITION_KEY)
                            .viewTag(TRANSITION_KEY)
                            .build())
                    .build();
              }
            })
        .build();
  }

  private Component getNonAnimatingComponent() {
    return TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
        .stateCaller(mStateCaller)
        .transition(null)
        .testComponent(
            new TestAnimationsComponentSpec
                .TestComponent() { // This could be a lambda but it fails ci.
              @Override
              public Component getComponent(ComponentContext componentContext, boolean state) {
                return Row.create(componentContext)
                    .heightDip(200)
                    .widthDip(200)
                    .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                    .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                    .child(
                        Row.create(componentContext)
                            .heightDip(40)
                            .widthDip(40)
                            .backgroundColor(Color.parseColor("#ee1111"))
                            .build())
                    .build();
              }
            })
        .build();
  }

  @Test
  public void transitionAnimation_interruption_overridesCurrentTransition() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(160, null))
                    .animate(AnimatedProperties.X))
            .testComponent(
                new TestAnimationsComponentSpec.TestComponent() {
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Row.create(componentContext)
                        .widthDip(200)
                        .justifyContent(state ? YogaJustify.FLEX_START : YogaJustify.FLEX_END)
                        .child(
                            Row.create(componentContext)
                                .heightDip(40)
                                .widthDip(40)
                                .transitionKey(TRANSITION_KEY)
                                .viewTag(TRANSITION_KEY)
                                .build())
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    final View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    assertThat(view.getX()).describedAs("x pos before transition").isEqualTo(160);

    // Start the transition by changing the state
    mStateCaller.update();

    // Advance to the mid point of the transition
    mTransitionTestRule.step(6);
    assertThat(view.getX()).describedAs("x pos at transition midpoint").isEqualTo(80f);

    // Trigger a new transition that interrupts the current transition and  returns the component to
    // its original position. NB: The Transition is fixed time, so it will take longer to return
    mStateCaller.update();

    // Advance to the mid point of the return transition
    mTransitionTestRule.step(6);
    assertThat(view.getX()).describedAs("x pos at return transition midpoint").isEqualTo(120);

    // Advance to the end of the return transition
    mTransitionTestRule.step(5);
    assertThat(view.getX()).describedAs("x pos after return transition").isEqualTo(160);
  }

  @Test
  public void
      animation_disappearAnimationOnNestedLithoViews_elementShouldDisappearAnimatingAlpha() {
    final StateCaller innerStateCaller = new StateCaller();
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            TestAnimationMount.create(componentContext)
                                .stateCaller(innerStateCaller))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 10 frames to account for the appear animation.
    mTransitionTestRule.step(10);

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    View innerView = mLegacyLithoViewRule.findViewWithTag("TestAnimationMount");
    // Here we get inner LithoView
    LithoView innerLithoView = (LithoView) innerView.getParent();

    // Update state on both
    mStateCaller.update();
    innerStateCaller.update();
    // We look for the same view
    innerView = mLegacyLithoViewRule.findViewWithTag("TestAnimationMount");

    assertThat(innerLithoView)
        .describedAs("We mantain the same LithoView")
        .isEqualTo(innerView.getParent());
  }

  @Test
  public void animation_animatingComponentAndChangingHost_elementShouldAnimateOnlyOnce() {
    LithoView secondLithoView = new LithoView(mActivityController.get());
    secondLithoView.setComponentTree(null);
    final TestAnimationsComponent component = getAnimatingXPropertyComponent();
    mLegacyLithoViewRule.setRoot(component);

    FrameLayout fl = new FrameLayout(mActivityController.get());
    ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();

    fl.addView(mLegacyLithoViewRule.getLithoView());
    fl.addView(secondLithoView);
    mActivityController.get().setContentView(fl);
    mActivityController.resume().visible();

    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // 160 is equal to height and width of 200 - 40 for the size of the row.
    assertThat(view.getX()).describedAs("view X axis should be at start position").isEqualTo(160);
    assertThat(view.getY()).describedAs("view Y axis should be at start position").isEqualTo(160);

    mLegacyLithoViewRule.useComponentTree(null);

    secondLithoView.setComponentTree(componentTree);

    view = secondLithoView.findViewWithTag(TRANSITION_KEY);

    mStateCaller.update();

    // X after state update should be at 160 because is going to be animated.
    assertThat(view.getX()).describedAs("view X axis after toggle").isEqualTo(160);
    // Y moves without animating
    assertThat(view.getY()).describedAs("view Y axis after toggle").isEqualTo(0);

    mTransitionTestRule.step(10);

    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);

    secondLithoView.setComponentTree(null);
    mLegacyLithoViewRule.useComponentTree(componentTree);

    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    assertThat(view.getX()).describedAs("view X axis after 10 frames").isEqualTo(0);
    assertThat(view.getY()).describedAs("view Y axis after 10 frames").isEqualTo(0);
  }

  @Test
  public void animation_disappearAnimationNewComponentTree_disappearingElementsContentsRemoved() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLegacyLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.create(TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .appearFrom(0)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .viewTag(TRANSITION_KEY)
                                    .transitionKey(TRANSITION_KEY)
                                    .key(TRANSITION_KEY)
                                : null)
                        .build();
                  }
                })
            .build();
    final Component nonAnimatingComponent = getNonAnimatingComponent();
    ComponentTree newComponentTree =
        ComponentTree.create(mLegacyLithoViewRule.getContext()).build();
    newComponentTree.setRoot(nonAnimatingComponent);
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    // We move 10 frames to account for the appear animation.
    mTransitionTestRule.step(10);
    View view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);
    // The view is not null
    assertThat(view).describedAs("view before disappearing").isNotNull();
    assertThat(view.getAlpha()).describedAs("view before disappearing").isEqualTo(1);
    mStateCaller.update();
    view = mLegacyLithoViewRule.findViewWithTag(TRANSITION_KEY);

    // After state update, even if the row was removed from the component, it still not null as we
    // are going to animate it. Alpha stays at 1 before advancing frames.
    assertThat(view).describedAs("view after toggle").isNotNull();
    assertThat(view.getAlpha()).describedAs("view after toggle").isEqualTo(1);

    mTransitionTestRule.step(5);
    // Check java doc for how we calculate this value.
    assertThat(view.getAlpha()).describedAs("view after 5 frames").isEqualTo(0.5868241f);

    assertThat(
            (Boolean)
                Whitebox.invokeMethod(mLegacyLithoViewRule.getLithoView(), "hasDisappearingItems"))
        .describedAs("root host has disappearing items before updating the tree")
        .isTrue();

    // Change component tree mid animation.
    mLegacyLithoViewRule.useComponentTree(newComponentTree);

    assertThat(
            (Boolean)
                Whitebox.invokeMethod(mLegacyLithoViewRule.getLithoView(), "hasDisappearingItems"))
        .describedAs("root host does not have disappearing items after setting tree")
        .isFalse();

    view = mLegacyLithoViewRule.findViewWithTagOrNull(TRANSITION_KEY);
    assertThat(view).describedAs("view after setting new tree").isNull();
  }

  @Test
  public void animation_clipChildren_shouldBeFalseDuringAnimation() {
    final TestAnimationsComponent component = getAnimatingXPropertyComponent();
    mLegacyLithoViewRule.setRoot(component);
    mActivityController.get().setContentView(mLegacyLithoViewRule.getLithoView());
    mActivityController.resume().visible();

    assertThat(mLegacyLithoViewRule.getLithoView().getClipChildren())
        .describedAs("before animation, clip children is set to true")
        .isTrue();

    mStateCaller.update();

    mTransitionTestRule.step(5);

    assertThat(mLegacyLithoViewRule.getLithoView().getClipChildren())
        .describedAs("during animation, clip children is set to false")
        .isFalse();

    mTransitionTestRule.step(5);

    assertThat(mLegacyLithoViewRule.getLithoView().getClipChildren())
        .describedAs("after animation, clip children is set to true")
        .isTrue();
  }
}
