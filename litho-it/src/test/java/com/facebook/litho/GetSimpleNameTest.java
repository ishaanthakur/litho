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
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class GetSimpleNameTest {

  private static class TestWrapperComponent extends SpecGeneratedComponent {

    private final Component mDelegate;

    protected TestWrapperComponent(Component delegate) {
      super("TestWrapper");
      mDelegate = delegate;
    }

    @Override
    protected Component getSimpleNameDelegate() {
      return mDelegate;
    }
  }

  private ComponentContext mContext;

  @Before
  public void setUp() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testGetSimpleName() {
    final Component testComponent = SimpleMountSpecTester.create(mContext).build();
    assertThat(testComponent.getSimpleName()).isEqualTo("SimpleMountSpecTester");
  }

  @Test
  public void testGetSimpleNameWithOneWrapper() {
    final Component inner = SimpleMountSpecTester.create(mContext).build();
    TestWrapperComponent wrapper = new TestWrapperComponent(inner);
    assertThat(wrapper.getSimpleName()).isEqualTo("TestWrapper(SimpleMountSpecTester)");
  }

  @Test
  public void testGetSimpleNameWithMultipleWrapper() {
    final Component inner = SimpleMountSpecTester.create(mContext).build();
    TestWrapperComponent wrapper = new TestWrapperComponent(inner);
    TestWrapperComponent wrapper2 = new TestWrapperComponent(wrapper);
    TestWrapperComponent wrapper3 = new TestWrapperComponent(wrapper2);
    assertThat(wrapper3.getSimpleName()).isEqualTo("TestWrapper(SimpleMountSpecTester)");
  }
}
