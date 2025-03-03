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
import android.graphics.Typeface
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.colorRes
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextAlignment
import com.facebook.samples.litho.R

class TabsCollectionKComponent : KComponent() {

  enum class Tab(val title: String) {
    Tab1("Tab1"),
    Tab2("Tab2"),
    Tab3("Tab3"),
  }

  override fun ComponentScope.render(): Component {
    val selectedTab = useState { Tab.Tab1 }

    return Collection {
      child(tabBar(selectedTab))

      when (selectedTab.value) {
        Tab.Tab1 ->
            subCollection(id = Tab.Tab1) {
              child(Text("Tab1.1"))
              child(id = 1, component = Text("Tab1.2"))
            }
        Tab.Tab2 ->
            subCollection(id = Tab.Tab2) {
              child(Text("Tab2.1"))
              child(id = 1, component = Text("Tab2.2"))
            }
        Tab.Tab3 ->
            subCollection(id = Tab.Tab3) {
              child(Text("Tab3.1"))
              child(id = 1, component = Text("Tab3.2"))
            }
      }
    }
  }

  private fun ResourcesScope.tabBar(selectedTab: State<Tab>): Component = Row {
    Tab.values().forEach { tab ->
      val isSelected = tab == selectedTab.value
      child(
          Text(
              tab.title.uppercase(),
              textSize = 16.sp,
              alignment = TextAlignment.CENTER,
              textColor = if (isSelected) colorRes(R.color.primaryColor) else Color.DKGRAY,
              textStyle = if (isSelected) Typeface.BOLD else Typeface.NORMAL,
              style =
                  Style.padding(all = 16.dp)
                      .flex(grow = 1f)
                      .backgroundColor(
                          if (isSelected) colorRes(R.color.colorPrimaryLightBg) else Color.WHITE)
                      .onClick { selectedTab.update { tab } }))
    }
  }
}
