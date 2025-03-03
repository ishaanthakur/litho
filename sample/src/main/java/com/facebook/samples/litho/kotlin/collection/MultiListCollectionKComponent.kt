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

import android.graphics.Typeface
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.flexbox.flex
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.useState
import com.facebook.litho.widget.Text

class MultiListCollectionKComponent : KComponent() {

  companion object {
    private const val ALL_FRIENDS_TAG = "all_friends"
    private const val TOP_FRIENDS_TAG = "top_friends"
  }

  private val friends = "Ross Rachel Joey Phoebe Monica Chandler".split(" ")

  override fun ComponentScope.render(): Component? {

    val shouldShowTopFriends = useState { false }

    val topFriends = listOf(0, 1)
    val allFriends = listOf(0, 1, 2, 3, 4, 5)

    return Column {
      child(
          Button("Toggle Top Friends") { shouldShowTopFriends.update(!shouldShowTopFriends.value) })
      child(
          Collection(style = Style.flex(grow = 1f)) {
            if (shouldShowTopFriends.value) {
              subCollection(id = TOP_FRIENDS_TAG) {
                child(Text("Top Friends", textStyle = Typeface.BOLD))
                topFriends.forEach { child(id = it, component = Text(friends[it])) }
              }
            }

            subCollection(id = ALL_FRIENDS_TAG) {
              child(Text("All Friends", textStyle = Typeface.BOLD))
              allFriends.forEach { child(id = it, component = Text(friends[it])) }
            }
          })
    }
  }
}
