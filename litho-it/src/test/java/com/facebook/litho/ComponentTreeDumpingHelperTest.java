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

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentTreeDumpingHelperTest {

  @Before
  public void skipIfRelease() {
    Assume.assumeTrue(
        "These tests cover debug functionality and can only be run " + "for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD);
  }

  @Test
  public void testBasicComponentTreeDumping() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build();
          }
        };

    ComponentContext componentContext = new ComponentContext(getApplicationContext());
    final ComponentTree componentTree = ComponentTree.create(componentContext, component).build();

    componentContext = ComponentContext.withComponentTree(componentContext, componentTree);

    final LithoView lithoView = new LithoView(getApplicationContext());
    lithoView.setComponentTree(componentTree);
    lithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));

    final String string = ComponentTreeDumpingHelper.dumpContextTree(componentContext);
    assertThat(string)
        .containsPattern("InlineLayout\\{V}\n" + "  SimpleMountSpecTester\\{V 100.0x100.0}");
  }
}
