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

/** Event triggered when the visible rect of a Component changes. */
public class VisibilityChangedEvent {
  /**
   * The visible top presents the top edge of the visible rect of the content. If the value is
   * {@code 0} then the item is fully visible from the top; if the value is {@code > 0} then some
   * part of the content is hidden from the top. Consequently if the value is {@code 0} and {@link
   * #percentVisibleHeight} < 100, then the content is hidden from the bottom edge.
   */
  public int visibleTop;

  /**
   * The visible left presents the left edge of the visible rect of the content. If the value is
   * {@code 0} then the item is fully visible from the left; if the value is {@code > 0} then some
   * part of the content is hidden from the left. Consequently if the value is {@code 0} and {@link
   * #percentVisibleWidth} < 100, then the content is hidden from the right edge.
   */
  public int visibleLeft;

  public int visibleHeight;
  public int visibleWidth;
  /** Between 0 and 100, indicates percentage of item width that is visible on screen. */
  public float percentVisibleWidth;
  /** Between 0 and 100, indicates percentage of item height that is visible on screen. */
  public float percentVisibleHeight;
}
