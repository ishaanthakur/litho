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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sections.widget.CrossAxisWrapMode
import com.facebook.litho.sections.widget.LinearSpacing
import com.facebook.litho.sp
import com.facebook.litho.view.background
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class HorizontalScrollKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Collection(
        itemDecoration = LinearSpacing(all = 20.dp),
    ) {
      child(FixedHeightHScroll())
      child(WrapFirstItemHeightHScroll())
      child(WrapDynamicHScroll())
    }
  }
}

// start_example
class FixedHeightHScroll : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Collection(
        layout = Collection.Linear(orientation = RecyclerView.HORIZONTAL),
        itemDecoration = LinearSpacing(all = 10.dp),
        style = Style.height(100.dp),
    ) {
      (0..10).forEach {
        child(
            id = it,
            component =
                Text(
                    text = "$it",
                    textSize = 24.sp,
                    style =
                        Style.padding(all = 30.dp).background(RoundedRect(Color.LTGRAY, 10.dp))))
      }
    }
  }
}
// end_example

class WrapFirstItemHeightHScroll : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Collection(
        layout =
            Collection.Linear(
                orientation = RecyclerView.HORIZONTAL,
                crossAxisWrapMode = CrossAxisWrapMode.MatchFirstChild),
        itemDecoration = LinearSpacing(all = 10.dp),
    ) {
      (0..10).forEach {
        child(
            id = it,
            component =
                Text(
                    text = "$it",
                    textSize = 24.sp,
                    style =
                        Style.padding(all = 30.dp).background(RoundedRect(Color.LTGRAY, 10.dp))))
      }
    }
  }
}

class WrapDynamicHScroll : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Collection(
        layout =
            Collection.Linear(
                orientation = RecyclerView.HORIZONTAL,
                crossAxisWrapMode = CrossAxisWrapMode.Dynamic),
        itemDecoration = LinearSpacing(all = 10.dp),
    ) {
      (0..10).forEach {
        child(
            id = it,
            component =
                Text(
                    text = "$it",
                    textSize = 24.sp,
                    style =
                        Style.padding(horizontal = 30.dp, vertical = (it * 5).dp)
                            .background(RoundedRect(Color.LTGRAY, 10.dp))))
      }
    }
  }
}
