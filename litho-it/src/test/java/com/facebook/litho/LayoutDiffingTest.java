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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.widget.MountSpecWithShouldUpdate;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.litho.widget.TextViewCounter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner.class)
public class LayoutDiffingTest {

  private final boolean mUsesInputOnlyInternalNode;
  private final boolean mOriginalValueOfUseInputOnlyInternalNodes;

  public @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  @ParameterizedRobolectricTestRunner.Parameters(name = "usesInputOnlyInternalNode={0}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false}, {true},
        });
  }

  public LayoutDiffingTest(boolean usesInputOnlyInternalNode) {
    mUsesInputOnlyInternalNode = usesInputOnlyInternalNode;
    mOriginalValueOfUseInputOnlyInternalNodes =
        ComponentsConfiguration.getDefaultComponentsConfiguration().getUseInputOnlyInternalNodes();
  }

  @Before
  public void before() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useInputOnlyInternalNodes(mUsesInputOnlyInternalNode));
  }

  @After
  public void after() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create()
            .useInputOnlyInternalNodes(mOriginalValueOfUseInputOnlyInternalNodes));
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that we
   * still properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  public void
      layoutDiffing_multipleStateUpdatesInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLegacyLithoViewRule
        .setRoot(
            createRootComponentWithStateUpdater(
                mLegacyLithoViewRule.getContext(),
                firstObjectForShouldUpdate,
                operations,
                stateUpdater))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    stateUpdater.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLegacyLithoViewRule.layout();

    assertThat(operations).isEmpty();
  }

  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLegacyLithoViewRule
        .setRoot(
            createRootComponent(
                mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    // Do two prop updates sequentially without draining the main thread queue
    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLegacyLithoViewRule.layout();

    assertThat(operations).isEmpty();
  }

  /**
   * In this scenario, we make sure that if a setRoot happens in the background followed by a second
   * setRoot in the background before the first can commit on the main thread, that we still
   * properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForFirstLayout_mountContentIsRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLegacyLithoViewRule
        .setRoot(
            createRootComponent(
                mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    final Object secondObjectForShouldUpdate = new Object();

    // Do two prop updates sequentially without draining the main thread queue
    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLegacyLithoViewRule.layout();

    // In this case, we did change the object for shouldUpdate in layout 1 even though
    // it was the same for layouts 2. We expect to see unmount and mount.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT);
    operations.clear();
  }

  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForSecondLayout_mountContentIsRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLegacyLithoViewRule
        .setRoot(
            createRootComponent(
                mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    final Object secondObjectForShouldUpdate = new Object();

    // Do two prop updates sequentially without draining the main thread queue
    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLegacyLithoViewRule.setRootAsync(
        createRootComponent(
            mLegacyLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLegacyLithoViewRule.layout();

    // Similar to the previous test, but the object changes on the second layout instead.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT);
  }

  @Test
  public void whenStateUpdateOnPureRenderMountSpec_shouldRemountItem() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component =
        Column.create(c)
            .child(TextViewCounter.create(c).viewWidth(200).viewHeight(200).build())
            .build();
    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    final View view = mLegacyLithoViewRule.getLithoView().getChildAt(0);
    assertThat(view).isNotNull();
    assertThat(view).isInstanceOf(TextView.class);
    assertThat(((TextView) view).getText()).isEqualTo("0");
    view.callOnClick();
    assertThat(((TextView) view).getText()).isEqualTo("1");
  }

  @Test
  public void whenStateUpdateOnPureRenderMountSpec_shouldRemountItem_with_reuse() {
    TempComponentsConfigurations.setReuseInternalNode(true);

    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component =
        Column.create(c)
            .child(TextViewCounter.create(c).viewWidth(200).viewHeight(200).build())
            .build();
    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    final View view = mLegacyLithoViewRule.getLithoView().getChildAt(0);
    assertThat(view).isNotNull();
    assertThat(view).isInstanceOf(TextView.class);
    assertThat(((TextView) view).getText()).isEqualTo("0");
    view.callOnClick();
    assertThat(((TextView) view).getText()).isEqualTo("1");

    TempComponentsConfigurations.restoreReuseInternalNode();
  }

  @Test
  public void onSetRootWithSameComponent_thenShouldNotRemeasureMountSpec() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object objectForShouldUpdate = new Object();

    final Component component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT);

    operations.clear();

    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    assertThat(operations).isEmpty();
  }

  @Test
  public void onSetRootWithSimilarComponent_thenShouldNotRemeasureMountSpec() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object objectForShouldUpdate = new Object();

    final Component component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT);

    operations.clear();

    final Component next =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(next).measure().layout();

    assertThat(operations).isEmpty();
  }

  @Test
  public void onSetRootWithSimilarComponentWithShouldUpdateTrue_thenShouldRemeasureMountSpec() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object objectForShouldUpdate = new Object();

    final Component component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT);

    operations.clear();

    final Component next =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(new Object())
                            .operationsOutput(operations)))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(next).measure().layout();

    assertThat(operations)
        .containsExactly(
            LifecycleStep.ON_MEASURE, LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT);
  }

  private static Component createRootComponentWithStateUpdater(
      ComponentContext c,
      Object objectForShouldUpdate,
      List<LifecycleStep> operationsOutput,
      SimpleStateUpdateEmulatorSpec.Caller stateUpdateCaller) {
    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    SimpleStateUpdateEmulator.create(c)
                        .caller(stateUpdateCaller)
                        .widthPx(100)
                        .heightPx(100)
                        .background(new ColorDrawable(Color.RED)))
                .child(
                    MountSpecWithShouldUpdate.create(c)
                        .objectForShouldUpdate(objectForShouldUpdate)
                        .operationsOutput(operationsOutput)
                        .widthPx(10)
                        .heightPx(10)))
        .build();
  }

  private static Component createRootComponent(
      ComponentContext c, Object objectForShouldUpdate, List<LifecycleStep> operationsOutput) {
    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    Row.create(c)
                        .widthPx(100)
                        .heightPx(100)
                        .background(new ColorDrawable(Color.RED)))
                .child(
                    MountSpecWithShouldUpdate.create(c)
                        .objectForShouldUpdate(objectForShouldUpdate)
                        .operationsOutput(operationsOutput)
                        .widthPx(10)
                        .heightPx(10)))
        .build();
  }
}
