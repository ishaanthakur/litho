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

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.drawable.DrawableUtils;

@Nullsafe(Nullsafe.Mode.LOCAL)
class DrawableComponent<T extends Drawable> extends Component {

  Drawable mDrawable;
  int mDrawableWidth;
  int mDrawableHeight;

  private DrawableComponent(Drawable drawable) {
    mDrawable = drawable;
  }

  @Override
  protected void onBoundsDefined(
      final ComponentContext c,
      final ComponentLayout layout,
      final @Nullable InterStagePropsContainer interStagePropsContainer) {
    setDrawableWidth(layout.getWidth());
    setDrawableHeight(layout.getHeight());
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return new MatrixDrawable();
  }

  @Override
  protected void onMount(
      final ComponentContext context,
      final Object content,
      final @Nullable InterStagePropsContainer interStagePropsContainer) {
    MatrixDrawable drawable = (MatrixDrawable) content;

    drawable.mount(getDrawable());
  }

  @Override
  protected void onBind(
      final ComponentContext c,
      final Object mountedContent,
      final @Nullable InterStagePropsContainer interStagePropsContainer) {
    final MatrixDrawable mountedDrawable = (MatrixDrawable) mountedContent;

    mountedDrawable.bind(getDrawableWidth(), getDrawableHeight());
  }

  @Override
  protected void onUnmount(
      final ComponentContext context,
      final Object mountedContent,
      final @Nullable InterStagePropsContainer interStagePropsContainer) {
    final MatrixDrawable<T> matrixDrawable = (MatrixDrawable<T>) mountedContent;
    matrixDrawable.unmount();
  }

  @Override
  protected boolean isPureRender() {
    return true;
  }

  @Override
  public MountType getMountType() {
    return MountType.DRAWABLE;
  }

  public static DrawableComponent create(Drawable drawable) {
    return new DrawableComponent<>(drawable);
  }

  private Drawable getDrawable() {
    return mDrawable;
  }

  @Override
  public boolean isEquivalentTo(@Nullable Component o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DrawableComponent drawableComponent = (DrawableComponent) o;

    return DrawableUtils.isEquivalentTo(mDrawable, drawableComponent.mDrawable);
  }

  @Override
  public String getSimpleName() {
    return "DrawableComponent";
  }

  private void setDrawableWidth(int drawableWidth) {
    mDrawableWidth = drawableWidth;
  }

  private int getDrawableWidth() {
    return mDrawableWidth;
  }

  private void setDrawableHeight(int drawableHeight) {
    mDrawableHeight = drawableHeight;
  }

  private int getDrawableHeight() {
    return mDrawableHeight;
  }
}
