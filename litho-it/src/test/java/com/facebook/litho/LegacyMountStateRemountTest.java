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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.EditText;
import com.facebook.litho.widget.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LegacyMountStateRemountTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testRemountOnNodeInfoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .viewTag("Alpha")
                    .enabled(true))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final View oldView = lithoView.getChildAt(0);

    final Object oldTag = oldView.getTag();
    final boolean oldIsEnabled = oldView.isEnabled();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .viewTag("Beta")
                    .enabled(false))
            .build();

    componentTree.setRootAndSizeSpecSync(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final View newView = lithoView.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final Object newTag = newView.getTag();
    final boolean newIsEnabled = newView.isEnabled();

    assertThat(newTag).isNotEqualTo(oldTag);
    assertThat(newIsEnabled).isNotEqualTo(oldIsEnabled);
  }

  @Test
  public void testRemountOnNoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(mContext)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final View oldView = lithoView.getChildAt(0);

    final Object oldTag = oldView.getTag();
    final String oldContentDescription = oldView.getContentDescription().toString();
    final Drawable oldBackground = oldView.getBackground();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(mContext)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build();

    componentTree.setRootAndSizeSpecSync(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    View newView = lithoView.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final Object newTag = newView.getTag();
    final String newContentDescription = newView.getContentDescription().toString();
    final Drawable newBackground = newView.getBackground();

    // Check that props were not set again
    assertThat(newTag).isSameAs(oldTag);
    assertThat(newContentDescription).isSameAs(oldContentDescription);
    assertThat(oldBackground).isSameAs(newBackground);
  }

  @Test
  public void testRemountOnViewNodeInfoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .backgroundColor(Color.RED))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final View oldView = lithoView.getChildAt(0);

    final ComparableDrawable oldDrawable = (ComparableDrawable) oldView.getBackground();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .backgroundColor(Color.CYAN))
            .build();

    componentTree.setRootAndSizeSpecSync(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final View newView = lithoView.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final ComparableDrawable newDrawable = (ComparableDrawable) newView.getBackground();

    assertThat(oldDrawable.isEquivalentTo(newDrawable)).isFalse();
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
