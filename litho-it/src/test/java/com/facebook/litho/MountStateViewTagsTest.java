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
import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.util.SparseArray;
import android.view.View;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateViewTagsTest {
  private static final int DUMMY_ID = 0x10000000;

  private ComponentContext mContext;

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testInnerComponentHostViewTags() {
    final Object tag1 = new Object();
    final SparseArray<Object> tags1 = new SparseArray<>(1);
    tags1.put(DUMMY_ID, tag1);

    final Object tag2 = new Object();
    final SparseArray<Object> tags2 = new SparseArray<>(1);
    tags2.put(DUMMY_ID, tag2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        create(c)
                            .viewTags(tags1)
                            .child(SimpleMountSpecTester.create(c))
                            .child(SimpleMountSpecTester.create(c)))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c).viewTags(tags2))
                    .build();
              }
            });

    final View innerHost1 = lithoView.getChildAt(0);
    final View innerHost2 = lithoView.getChildAt(1);

    assertThat(innerHost1.getTag(DUMMY_ID)).isEqualTo(tag1);
    assertThat(innerHost2.getTag(DUMMY_ID)).isEqualTo(tag2);
  }

  @Test
  public void testRootHostViewTags() {
    final Object tag = new Object();
    final SparseArray<Object> tags = new SparseArray<>(1);
    tags.put(DUMMY_ID, tag);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .viewTags(tags)
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    final View innerHost = lithoView.getChildAt(0);
    assertThat(innerHost.getTag(DUMMY_ID)).isEqualTo(tag);
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
