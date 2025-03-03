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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.DynamicPropsResetValueTester;
import com.facebook.litho.widget.DynamicPropsResetValueTesterSpec;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class DynamicPropsTest {
  private ComponentContext mContext;
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testDynamicAlphaApplied() {
    final float startValue = 0.8f;
    final DynamicValue<Float> alphaDV = new DynamicValue<>(startValue);

    final LithoView lithoView =
        mountComponent(
            mContext, Column.create(mContext).widthPx(80).heightPx(80).alpha(alphaDV).build());

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final View hostView = lithoView.getChildAt(0);

    assertThat(hostView.getAlpha()).isEqualTo(startValue);

    alphaDV.set(0.5f);
    assertThat(hostView.getAlpha()).isEqualTo(0.5f);

    alphaDV.set(0.f);
    assertThat(hostView.getAlpha()).isEqualTo(0.f);

    alphaDV.set(1.f);
    assertThat(hostView.getAlpha()).isEqualTo(1.f);
  }

  @Test
  public void testAttributesAndDynamicPropDuringUpdate() {
    final float startValue = 0.8f;
    final DynamicValue<Float> alphaDV = new DynamicValue<>(startValue);

    final Component component1 =
        Column.create(mContext)
            .widthPx(80)
            .heightPx(80)
            .backgroundColor(0xFFFF0000)
            .alpha(alphaDV)
            .build();

    final Component component2 =
        Column.create(mContext)
            .widthPx(80)
            .heightPx(80)
            .backgroundColor(0xFF00FF00)
            .alpha(alphaDV)
            .build();

    mLegacyLithoViewRule
        .setRoot(component1)
        .setSizeSpecs(makeSizeSpec(80, EXACTLY), makeSizeSpec(80, EXACTLY))
        .attachToWindow()
        .measure()
        .layout();

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();

    // Ensure we have one view.
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    View hostView = lithoView.getChildAt(0);

    // Ensure alpha DV is correct
    assertThat(hostView.getAlpha()).isEqualTo(startValue);

    // Ensure background attribute is present and has the correct value.
    assertThat(hostView.getBackground()).isNotNull();
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(0xFFFF0000);

    // Mount component2, which is identical to component1, except with a different bg, invoking
    // an update sequence.
    mLegacyLithoViewRule.setRoot(component2);

    // Grab the host again
    hostView = lithoView.getChildAt(0);

    // Alter the alpha DV
    alphaDV.set(0.5f);

    // Ensure the DV is properly applied on the view
    assertThat(hostView.getAlpha()).isEqualTo(0.5f);

    // Ensure background attribute is present and has the correct value.
    assertThat(hostView.getBackground()).isNotNull();
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(0xFF00FF00);
  }

  @Test
  public void testDynamicTranslationApplied() {
    final float startValueX = 100;
    final float startValueY = -100;
    final DynamicValue<Float> translationXDV = new DynamicValue<>(startValueX);
    final DynamicValue<Float> translationYDV = new DynamicValue<>(startValueY);

    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext)
                .widthPx(80)
                .heightPx(80)
                .translationX(translationXDV)
                .translationY(translationYDV)
                .build());

    assertThat(lithoView.getChildCount()).isEqualTo(1);

    final View hostView = lithoView.getChildAt(0);

    assertThat(hostView.getTranslationX()).isEqualTo(startValueX);
    assertThat(hostView.getTranslationY()).isEqualTo(startValueY);

    translationXDV.set(50.f);
    translationYDV.set(20.f);
    assertThat(hostView.getTranslationX()).isEqualTo(50.f);
    assertThat(hostView.getTranslationY()).isEqualTo(20.f);

    translationXDV.set(-50.f);
    translationYDV.set(-20.f);
    assertThat(hostView.getTranslationX()).isEqualTo(-50.f);
    assertThat(hostView.getTranslationY()).isEqualTo(-20.f);

    translationXDV.set(0f);
    translationYDV.set(0f);
    assertThat(hostView.getTranslationX()).isEqualTo(0f);
    assertThat(hostView.getTranslationY()).isEqualTo(0f);
  }

  @Test
  public void testDynamicScaleApplied() {
    final float startValueX = 1.5f;
    final float startValueY = -1.5f;
    final DynamicValue<Float> scaleXDV = new DynamicValue<>(startValueX);
    final DynamicValue<Float> scaleYDV = new DynamicValue<>(startValueY);

    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext)
                .widthPx(80)
                .heightPx(80)
                .scaleX(scaleXDV)
                .scaleY(scaleYDV)
                .build());

    assertThat(lithoView.getChildCount()).isEqualTo(1);

    final View hostView = lithoView.getChildAt(0);

    assertThat(hostView.getScaleX()).isEqualTo(startValueX);
    assertThat(hostView.getScaleY()).isEqualTo(startValueY);

    scaleXDV.set(0.5f);
    scaleYDV.set(2.f);
    assertThat(hostView.getScaleX()).isEqualTo(0.5f);
    assertThat(hostView.getScaleY()).isEqualTo(2.f);

    scaleXDV.set(2.f);
    scaleYDV.set(0.5f);
    assertThat(hostView.getScaleX()).isEqualTo(2.f);
    assertThat(hostView.getScaleY()).isEqualTo(.5f);

    scaleXDV.set(0f);
    scaleYDV.set(0f);
    assertThat(hostView.getScaleX()).isEqualTo(0f);
    assertThat(hostView.getScaleY()).isEqualTo(0f);
  }

  @Test
  public void testDynamicBackgroundColorApplied() {
    final int startValue = Color.RED;
    final DynamicValue<Integer> backgroundColorDV = new DynamicValue<>(startValue);

    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext)
                .widthPx(80)
                .heightPx(80)
                .backgroundColor(backgroundColorDV)
                .build());

    assertThat(lithoView.getChildCount()).isEqualTo(1);

    final View hostView = lithoView.getChildAt(0);

    assertThat(hostView.getBackground()).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(startValue);

    backgroundColorDV.set(Color.BLUE);
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(Color.BLUE);

    backgroundColorDV.set(0x88888888);
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(0x88888888);

    backgroundColorDV.set(Color.TRANSPARENT);
    assertThat(((ColorDrawable) hostView.getBackground()).getColor()).isEqualTo(Color.TRANSPARENT);
  }

  @Test
  public void testDynamicRotationApplied() {
    final float startValue = 0f;
    final DynamicValue<Float> rotationDV = new DynamicValue<>(startValue);

    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext).widthPx(80).heightPx(80).rotation(rotationDV).build());

    assertThat(lithoView.getChildCount()).isEqualTo(1);

    final View hostView = lithoView.getChildAt(0);

    assertThat(hostView.getRotation()).isEqualTo(startValue);

    rotationDV.set(364f);
    assertThat(hostView.getRotation()).isEqualTo(364f);

    rotationDV.set(520f);
    assertThat(hostView.getRotation()).isEqualTo(520f);

    rotationDV.set(-1.f);
    assertThat(hostView.getRotation()).isEqualTo(-1.f);
  }

  @Test
  public void testNullDynamicValue() {
    final DynamicValue<Integer> nullIntegerValue = null;
    final DynamicValue<Float> nullFloatValue = null;

    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext)
                .widthPx(80)
                .heightPx(80)
                .backgroundColor(nullIntegerValue)
                .rotation(nullFloatValue)
                .build());

    assertThat(lithoView.getBackground()).isEqualTo(null);
    assertThat(lithoView.getRotation()).isEqualTo(0.0f);
  }

  private static class DynamicElevationBuilder extends Component.Builder<DynamicElevationBuilder> {

    private Component component;

    protected DynamicElevationBuilder(
        ComponentContext c, int defStyleAttr, int defStyleRes, Component component) {
      super(c, defStyleAttr, defStyleRes, component);
      this.component = component;
    }

    @Override
    public Component build() {
      return component;
    }

    @Override
    public DynamicElevationBuilder getThis() {
      return this;
    }

    @Override
    protected void setComponent(Component component) {
      this.component = component;
    }
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void testDynamicElevationApplied() {
    final LithoView lithoView = new LithoView(mContext);

    final float startValue = 1f;
    final DynamicValue<Float> elevationDV = new DynamicValue<>(startValue);
    final Component component =
        new DynamicElevationBuilder(
                mContext,
                -1,
                -1,
                new Component() {
                  @Override
                  public MountType getMountType() {
                    return MountType.VIEW;
                  }
                })
            .shadowElevation(elevationDV)
            .build();

    final DynamicPropsManager dynamicPropsManager = new DynamicPropsManager();
    dynamicPropsManager.onBindComponentToContent(component, mContext, lithoView);

    assertThat(lithoView.getElevation()).isEqualTo(startValue);

    elevationDV.set(50f);
    assertThat(lithoView.getElevation()).isEqualTo(50f);

    elevationDV.set(-50f);
    assertThat(lithoView.getElevation()).isEqualTo(-50f);
  }

  @Test
  public void commonDynamicProps_unbindAndRebindContent_resetValues() {
    final DynamicPropsResetValueTesterSpec.Caller stateUpdateCaller =
        new DynamicPropsResetValueTesterSpec.Caller();
    final Component component =
        DynamicPropsResetValueTester.create(mContext).caller(stateUpdateCaller).build();
    mLegacyLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    final MountDelegateTarget mountDelegateTarget =
        mLegacyLithoViewRule.getLithoView().getMountDelegateTarget();

    long text1HostId = -1;
    long text2HostId = -1;

    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);

      if (mountItem != null) {
        final LayoutOutput layoutOutput = LayoutOutput.getLayoutOutput(mountItem);

        if (layoutOutput.getComponent().getSimpleName().equals("Text")) {
          final long hostMarker =
              i != 0 ? mountItem.getRenderTreeNode().getParent().getRenderUnit().getId() : -1;

          if (text1HostId == -1) {
            text1HostId = hostMarker;
          } else if (text2HostId == -1) {
            text2HostId = hostMarker;
          }
        }
      }
    }

    HostComponent text1HostComponent = null;
    HostComponent text2HostComponent = null;

    ComponentHost text1Host = null;
    ComponentHost text2Host = null;

    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);

      if (mountItem != null) {
        final LayoutOutput layoutOutput = LayoutOutput.getLayoutOutput(mountItem);
        if (text1HostId == MountItem.getId(mountItem)) {
          text1HostComponent = (HostComponent) layoutOutput.getComponent();
          text1Host = (ComponentHost) mountItem.getContent();
        }

        if (text2HostId == MountItem.getId(mountItem)) {
          text2HostComponent = (HostComponent) layoutOutput.getComponent();
          text2Host = (ComponentHost) mountItem.getContent();
        }
      }
    }

    assertThat(text1HostComponent.hasCommonDynamicProps()).isTrue();
    assertThat(text1Host.getAlpha()).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_TRANSPARENT);

    assertThat(text2HostComponent.hasCommonDynamicProps()).isFalse();
    assertThat(text2Host.getAlpha()).isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE);

    stateUpdateCaller.toggleShowChild();

    HostComponent stateUpdateText1HostComponent = null;
    HostComponent stateUpdateText2HostComponent = null;

    ComponentHost stateUpdateText1Host = null;
    ComponentHost stateUpdateText2Host = null;

    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final MountItem mountItem = mountDelegateTarget.getMountItemAt(i);

      if (mountItem != null) {
        final LayoutOutput layoutOutput = LayoutOutput.getLayoutOutput(mountItem);
        if (text1HostId == MountItem.getId(mountItem)) {
          stateUpdateText1HostComponent = (HostComponent) layoutOutput.getComponent();
          stateUpdateText1Host = (ComponentHost) mountItem.getContent();
        }

        if (text2HostId == MountItem.getId(mountItem)) {
          stateUpdateText2HostComponent = (HostComponent) layoutOutput.getComponent();
          stateUpdateText2Host = (ComponentHost) mountItem.getContent();
        }
      }
    }

    assertThat(stateUpdateText1Host).isEqualTo(text1Host);
    assertThat(stateUpdateText2Host).isEqualTo(text2Host);

    assertThat(stateUpdateText1HostComponent.hasCommonDynamicProps()).isFalse();
    assertThat(stateUpdateText1Host.getAlpha())
        .isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE);

    assertThat(stateUpdateText2HostComponent.hasCommonDynamicProps()).isFalse();
    assertThat(stateUpdateText2Host.getAlpha())
        .isEqualTo(DynamicPropsResetValueTesterSpec.ALPHA_OPAQUE);
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
