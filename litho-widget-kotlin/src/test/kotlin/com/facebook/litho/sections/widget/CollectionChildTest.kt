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

package com.facebook.litho.sections.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.testing.LegacyLithoViewRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s children */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionChildTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

  @Test
  fun `test empty component renders`() {
    class Test : KComponent() {
      override fun ComponentScope.render(): Component {
        return Collection {}
      }
    }

    lithoViewRule.setSizePx(100, 100).render { Test() }
  }

  @Test
  fun `test add null component renders`() {
    class Test : KComponent() {
      override fun ComponentScope.render(): Component {
        return Collection { child(null) }
      }
    }

    lithoViewRule.setSizePx(100, 100).render { Test() }
  }

  @Test
  fun `test add null component lambda renders`() {
    class Test : KComponent() {
      override fun ComponentScope.render(): Component {
        return Collection { child(deps = arrayOf()) { null } }
      }
    }

    lithoViewRule.setSizePx(100, 100).render { Test() }
  }
}
