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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DynamicPropsExtension
    extends MountExtension<Void, DynamicPropsExtension.DynamicPropsExtensionState> {

  private static final DynamicPropsExtension sInstance = new DynamicPropsExtension();

  @Override
  protected DynamicPropsExtensionState createState() {
    return new DynamicPropsExtensionState();
  }

  private DynamicPropsExtension() {}

  public static DynamicPropsExtension getInstance() {
    return sInstance;
  }

  @Override
  public void onBindItem(
      final ExtensionState<DynamicPropsExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.output;
      final DynamicPropsExtensionState state = extensionState.getState();

      state.mDynamicPropsManager.onBindComponentToContent(
          output.getComponent(),
          LithoRenderUnit.getComponentContext((LithoRenderUnit) renderUnit),
          content);
    }
  }

  @Override
  public void onUnbindItem(
      final ExtensionState<DynamicPropsExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    if (renderUnit instanceof LithoRenderUnit) {
      final LithoRenderUnit lithoRenderUnit = (LithoRenderUnit) renderUnit;
      final LayoutOutput output = lithoRenderUnit.output;
      final DynamicPropsExtensionState state = extensionState.getState();

      state.mDynamicPropsManager.onUnbindComponent(output.getComponent(), content);
    }
  }

  @Override
  public boolean shouldUpdateItem(
      final RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData) {
    return true;
  }

  static class DynamicPropsExtensionState {
    private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();

    @VisibleForTesting
    public DynamicPropsManager getDynamicPropsManager() {
      return mDynamicPropsManager;
    }
  }
}
