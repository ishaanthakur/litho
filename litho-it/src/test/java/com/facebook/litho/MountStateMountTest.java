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

import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.CardHeaderComponent;
import com.facebook.rendercore.MountDelegateTarget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateMountTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = mLegacyLithoViewRule.getContext();
  }

  @Test
  public void unmountAll_mountStateNeedsRemount() {
    final Component root =
        Column.create(mContext).child(CardHeaderComponent.create(mContext).title("Title")).build();

    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final MountDelegateTarget mountDelegateTarget =
        mLegacyLithoViewRule.getLithoView().getMountDelegateTarget();
    assertThat(mountDelegateTarget.needsRemount()).isFalse();

    mLegacyLithoViewRule.getLithoView().unmountAllItems();
    assertThat(mountDelegateTarget.needsRemount()).isTrue();
  }
}
