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

package com.facebook.litho

/**
 * The implicit receiver for [KComponent.render] call. This class exposes the ability to use hooks,
 * like [useState], and convenience functions, like [dp].
 */
class ComponentScope(override val context: ComponentContext) : ResourcesScope {
  // TODO: Extract into more generic container to track hooks when needed
  internal var useStateIndex = 0
  internal var useCachedIndex = 0
  internal var transitions: MutableList<Transition>? = null
  internal var useEffectEntries: MutableList<Attachable>? = null
}
