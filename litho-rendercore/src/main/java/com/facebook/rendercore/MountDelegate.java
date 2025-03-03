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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Can be passed to a MountState to override default mounting behaviour and control which items get
 * mounted or unmounted.
 */
public class MountDelegate {

  private final LongSparseArray<Integer> mReferenceCountMap = new LongSparseArray<>();
  private final List<MountExtension> mMountExtensions = new ArrayList<>();
  private final MountDelegateTarget mMountDelegateTarget;
  private final Map<MountExtension, ExtensionState> mExtensionStates = new HashMap<>();
  private @Nullable ExtensionState mUnmountDelegateExtensionState;
  private boolean mReferenceCountingEnabled = false;
  private boolean mCollectVisibleBoundsChangedCalls = false;
  private int mNotifyVisibleBoundsChangedNestCount = 0;
  private final Set<Object> mNotifyVisibleBoundsChangedItems = new HashSet<>();
  private final List<MountExtension> mExtensionsToUpdate = new ArrayList<>();
  private final List<ExtensionState> mExtensionStatesToUpdate = new ArrayList<>();

  public MountDelegate(MountDelegateTarget mountDelegateTarget) {
    mMountDelegateTarget = mountDelegateTarget;
  }

  public void setCollectVisibleBoundsChangedCalls(boolean value) {
    mCollectVisibleBoundsChangedCalls = value;
  }

  public void addExtension(MountExtension mountExtension) {
    final ExtensionState extensionState = mountExtension.createExtensionState(this);

    if (mountExtension instanceof UnmountDelegateExtension) {
      mMountDelegateTarget.setUnmountDelegateExtension((UnmountDelegateExtension) mountExtension);
      mUnmountDelegateExtensionState = extensionState;
    }

    mReferenceCountingEnabled = mReferenceCountingEnabled || mountExtension.canPreventMount();

    mExtensionStates.put(mountExtension, extensionState);
    mMountExtensions.add(mountExtension);
  }

  public void removeExtension(MountExtension mountExtension) {
    mMountExtensions.remove(mountExtension);
    mExtensionStates.remove(mountExtension);

    if (mountExtension instanceof UnmountDelegateExtension) {
      mMountDelegateTarget.removeUnmountDelegateExtension();
      mUnmountDelegateExtensionState = null;
    }

    updateRefCountEnabled();
  }

  void unregisterAllExtensions() {
    mMountExtensions.clear();
    mExtensionStates.clear();
    mReferenceCountingEnabled = false;
  }

  public void notifyVisibleBoundsChangedForItem(Object item) {
    if (!mCollectVisibleBoundsChangedCalls) {
      RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item);
      return;
    }

    if (mNotifyVisibleBoundsChangedItems.contains(item)) {
      return;
    }

    mNotifyVisibleBoundsChangedItems.add(item);
  }

  public void startNotifyVisibleBoundsChangedSection() {
    if (!mCollectVisibleBoundsChangedCalls) {
      return;
    }

    mNotifyVisibleBoundsChangedNestCount++;
  }

  public void endNotifyVisibleBoundsChangedSection() {
    if (!mCollectVisibleBoundsChangedCalls) {
      return;
    }

    mNotifyVisibleBoundsChangedNestCount--;

    if (mNotifyVisibleBoundsChangedNestCount == 0) {
      for (Object item : mNotifyVisibleBoundsChangedItems) {
        RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item);
      }

      mNotifyVisibleBoundsChangedItems.clear();
    }
  }

  private void updateRefCountEnabled() {
    mReferenceCountingEnabled = false;
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mReferenceCountingEnabled =
          mReferenceCountingEnabled || mMountExtensions.get(i).canPreventMount();
    }
  }

  void unBind() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.onUnbind(getExtensionState(mountExtension));
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void unMount() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.onUnmount(getExtensionState(mountExtension));
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void onBindItem(final RenderUnit renderUnit, final Object content, final Object layoutData) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      extension.onBindItem(getExtensionState(extension), renderUnit, content, layoutData);
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void onUnbindItem(final RenderUnit renderUnit, final Object content, final Object layoutData) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      extension.onUnbindItem(getExtensionState(extension), renderUnit, content, layoutData);
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void onUpdateItemsIfNeeded(
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Object content) {
    startNotifyVisibleBoundsChangedSection();

    mExtensionsToUpdate.clear();
    mExtensionStatesToUpdate.clear();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      if (extension.shouldUpdateItem(
          previousRenderUnit, previousLayoutData, nextRenderUnit, nextLayoutData)) {
        mExtensionsToUpdate.add(extension);
        mExtensionStatesToUpdate.add(getExtensionState(extension));
      }
    }

    if (!mExtensionsToUpdate.isEmpty()) {
      final int size = mExtensionsToUpdate.size();

      // Unbind
      for (int i = 0; i < size; i++) {
        final MountExtension extension = mExtensionsToUpdate.get(i);
        final ExtensionState state = mExtensionStatesToUpdate.get(i);
        extension.onUnbindItem(state, previousRenderUnit, content, previousLayoutData);
      }

      // Unmount
      for (int i = 0; i < size; i++) {
        final MountExtension extension = mExtensionsToUpdate.get(i);
        final ExtensionState state = mExtensionStatesToUpdate.get(i);
        extension.onUnmountItem(state, previousRenderUnit, content, previousLayoutData);
      }

      // Mount
      for (int i = 0; i < size; i++) {
        final MountExtension extension = mExtensionsToUpdate.get(i);
        final ExtensionState state = mExtensionStatesToUpdate.get(i);
        extension.onMountItem(state, nextRenderUnit, content, nextLayoutData);
      }

      // Bind
      for (int i = 0; i < size; i++) {
        final MountExtension extension = mExtensionsToUpdate.get(i);
        final ExtensionState state = mExtensionStatesToUpdate.get(i);
        extension.onBindItem(state, nextRenderUnit, content, nextLayoutData);
      }

      mExtensionsToUpdate.clear();
      mExtensionStatesToUpdate.clear();
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onMountItem(
      final RenderUnit renderUnit, final Object content, final Object layoutData) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      extension.onMountItem(getExtensionState(extension), renderUnit, content, layoutData);
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onUnmountItem(
      final RenderUnit renderUnit, final Object content, final @Nullable Object layoutData) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      extension.onUnmountItem(getExtensionState(extension), renderUnit, content, layoutData);
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onBoundsAppliedToItem(RenderTreeNode node, Object content) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension extension = mMountExtensions.get(i);
      extension.onBoundsAppliedToItem(
          getExtensionState(extension), node.getRenderUnit(), content, node.getLayoutData());
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public ExtensionState getExtensionState(MountExtension mountExtension) {
    return mExtensionStates.get(mountExtension);
  }

  @Nullable
  public ExtensionState getUnmountDelegateExtensionState() {
    return mUnmountDelegateExtensionState;
  }

  public Object getContentAt(int position) {
    return mMountDelegateTarget.getContentAt(position);
  }

  public @Nullable Object getContentById(long id) {
    return mMountDelegateTarget.getContentById(id);
  }

  public boolean isRootItem(int position) {
    return mMountDelegateTarget.isRootItem(position);
  }

  /** @return true if this item needs to be mounted. */
  public boolean maybeLockForMount(RenderTreeNode renderTreeNode, int index) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      final MountExtension mountExtension = mMountExtensions.get(i);
      mountExtension.beforeMountItem(getExtensionState(mountExtension), renderTreeNode, index);
    }

    endNotifyVisibleBoundsChangedSection();

    return hasAcquiredRef(renderTreeNode.getRenderUnit().getId());
  }

  public boolean isLockedForMount(RenderTreeNode renderTreeNode) {
    return isLockedForMount(renderTreeNode.getRenderUnit().getId());
  }

  public boolean isLockedForMount(long id) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    return hasAcquiredRef(id);
  }

  private boolean hasAcquiredRef(long renderUnitId) {
    final Integer refCount = mReferenceCountMap.get(renderUnitId);

    return refCount != null && refCount > 0;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void acquireMountRef(final RenderTreeNode node) {
    acquireMountRef(node.getRenderUnit().getId());
  }

  public void acquireMountRef(final long id) {
    incrementExtensionRefCount(id);
  }

  public void acquireAndMountRef(final RenderTreeNode node) {
    acquireAndMountRef(node.getRenderUnit().getId());
  }

  public void acquireAndMountRef(final long id) {
    acquireMountRef(id);

    // Only mount if we're during a mounting phase, otherwise the mounting phase will take care of
    // that.
    mMountDelegateTarget.notifyMount(id);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void releaseMountRef(final RenderTreeNode renderTreeNode) {
    releaseMountRef(renderTreeNode.getRenderUnit().getId());
  }

  public void releaseMountRef(final long id) {
    decrementExtensionRefCount(id);
  }

  public void releaseAndUnmountRef(final RenderTreeNode renderTreeNode) {
    releaseAndUnmountRef(renderTreeNode.getRenderUnit().getId());
  }

  public void releaseAndUnmountRef(final long id) {
    final boolean wasLockedForMount = isLockedForMount(id);
    releaseMountRef(id);

    if (wasLockedForMount && !isLockedForMount(id)) {
      mMountDelegateTarget.notifyUnmount(id);
    }
  }

  public void releaseAllAcquiredReferences() {
    if (!mReferenceCountingEnabled) {
      return;
    }

    for (MountExtension<?, ?> extension : mMountExtensions) {
      final ExtensionState state = getExtensionState(extension);
      state.releaseAllAcquiredReferences();
    }

    mReferenceCountMap.clear();
  }

  private void incrementExtensionRefCount(long renderUnitId) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null) {
      refCount = 0;
    }

    mReferenceCountMap.put(renderUnitId, refCount + 1);
  }

  private void decrementExtensionRefCount(final long renderUnitId) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null || refCount == 0) {
      throw new IllegalStateException(
          "Trying to decrement reference count for an item you don't own.");
    }

    mReferenceCountMap.put(renderUnitId, refCount - 1);
  }

  public MountDelegateTarget getMountDelegateTarget() {
    return mMountDelegateTarget;
  }

  @VisibleForTesting
  public int getRefCount(long id) {
    return mReferenceCountMap.get(id);
  }
}
