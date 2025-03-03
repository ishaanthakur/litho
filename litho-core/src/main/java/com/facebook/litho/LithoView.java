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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityManagerCompat;
import androidx.core.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.RenderState;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RootHost;
import com.facebook.rendercore.transitions.AnimatedRootHost;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import com.facebook.rendercore.visibility.VisibilityOutput;
import com.facebook.rendercore.visibility.VisibilityUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** A {@link ViewGroup} that can host the mounted state of a {@link Component}. */
public class LithoView extends ComponentHost implements RootHost, AnimatedRootHost {

  public static final String ZERO_HEIGHT_LOG = "LithoView:0-height";
  public static final String SET_ALREADY_ATTACHED_COMPONENT_TREE =
      "LithoView:SetAlreadyAttachedComponentTree";
  private static final String LITHO_LIFECYCLE_FOUND = "lithoView:LithoLifecycleProviderFound";
  private static final String TAG = LithoView.class.getSimpleName();
  private boolean mIsMountStateDirty;
  private final boolean mDelegateToRenderCore;
  private final @Nullable MountDelegateTarget mMountDelegateTarget;
  private boolean mHasVisibilityHint;
  private boolean mPauseMountingWhileVisibilityHintFalse;
  private boolean mVisibilityHintIsVisible;
  private boolean mSkipMountingIfNotVisible;
  private @Nullable LithoLifecycleProvider mLifecycleProvider;

  public interface OnDirtyMountListener {
    /**
     * Called when finishing a mount where the mount state was dirty. This indicates that there were
     * new props/state in the tree, or the LithoView was mounting a new ComponentTree
     */
    void onDirtyMount(LithoView view);
  }

  public interface OnPostDrawListener {
    void onPostDraw();
  }

  @Nullable private ComponentTree mComponentTree;
  private final @Nullable MountState mMountState;
  private final ComponentContext mComponentContext;
  private boolean mIsAttached;
  // The bounds of the visible rect that was used for the previous incremental mount.
  private final Rect mPreviousMountVisibleRectBounds = new Rect();

  private boolean mForceLayout;
  private boolean mSuppressMeasureComponentTree;
  private boolean mIsMeasuring = false;
  private boolean mHasNewComponentTree = false;
  private int mAnimatedWidth = -1;
  private int mAnimatedHeight = -1;
  private OnDirtyMountListener mOnDirtyMountListener = null;
  private final Rect mRect = new Rect();
  @Nullable private OnPostDrawListener mOnPostDrawListener = null;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListener mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListener(this);

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;
  private int mTransientStateCount;
  private boolean mDoMeasureInLayout;
  @Nullable private Map<String, ComponentLogParams> mInvalidStateLogParams;
  @Nullable private String mPreviousComponentSimpleName;
  @Nullable private String mNullComponentCause;
  @Nullable private MountStartupLoggingInfo mMountStartupLoggingInfo;
  @Nullable private LithoHostListenerCoordinator mLithoHostListenerCoordinator;

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @param isReconciliationEnabled should enable reconciliation.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   * @deprecated Use {@link #create(Context, Component)} instead and set config explicitly on the
   *     {@link ComponentTree} using {@link ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public static LithoView create(
      Context context, Component component, boolean isReconciliationEnabled) {
    return create(new ComponentContext(context), component, isReconciliationEnabled);
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(Context context, Component component) {
    return create(context, component, null);
  }

  public static LithoView create(
      Context context, Component component, LithoLifecycleProvider lifecycleProvider) {
    return create(new ComponentContext(context), component, lifecycleProvider);
  }
  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context {@link ComponentContext}.
   * @param component The root component to draw.
   * @param isReconciliationEnabled should enable reconciliation.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   * @deprecated Use {@link #create(Context, Component)} instead and set config explicitly on the
   *     {@link ComponentTree} using {@link ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public static LithoView create(
      ComponentContext context, Component component, boolean isReconciliationEnabled) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(
        ComponentTree.create(context, component)
            .isReconciliationEnabled(isReconciliationEnabled)
            .build());

    return lithoView;
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context {@link ComponentContext}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(ComponentContext context, Component component) {
    return create(context, component, null);
  }

  /**
   * Creates a new LithoView and sets a new ComponentTree on it. The ComponentTree is subscribed to
   * the given LithoLifecycleProvider instance.
   *
   * @return
   */
  public static LithoView create(
      ComponentContext context,
      Component component,
      @Nullable LithoLifecycleProvider lifecycleProvider) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(ComponentTree.create(context, component, lifecycleProvider).build());
    return lithoView;
  }

  public LithoView(Context context) {
    this(context, null);
  }

  public LithoView(Context context, @Nullable AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public LithoView(ComponentContext context) {
    this(context, null);
  }

  public LithoView(ComponentContext context, boolean delegateToRenderCore) {
    this(context, null, delegateToRenderCore);
  }

  public LithoView(ComponentContext context, @Nullable AttributeSet attrs) {
    this(context, attrs, ComponentsConfiguration.delegateToRenderCoreMount);
  }

  public LithoView(
      ComponentContext context, @Nullable AttributeSet attrs, final boolean delegateToRenderCore) {
    super(context, attrs);
    mComponentContext = context;

    mDelegateToRenderCore = delegateToRenderCore;

    if (mDelegateToRenderCore) {

      com.facebook.rendercore.MountState renderCoreMountState =
          new com.facebook.rendercore.MountState(this);

      renderCoreMountState.setEnsureParentMounted(
          ComponentsConfiguration.ensureParentMountedInRenderCoreMountState);

      mMountDelegateTarget = renderCoreMountState;

      mMountState = null;
    } else {
      mMountDelegateTarget = null;
      mMountState = new MountState(this);
    }

    mAccessibilityManager =
        (AccessibilityManager) context.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
  }

  private static void performLayoutOnChildrenIfNecessary(ComponentHost host) {
    final int childCount = host.getChildCount();
    if (childCount == 0) {
      return;
    }

    // Snapshot the children before traversal as measure/layout could trigger events which cause
    // children to be mounted/unmounted.
    View[] children = new View[childCount];
    for (int i = 0; i < childCount; i++) {
      children[i] = host.getChildAt(i);
    }

    for (int i = 0; i < childCount; i++) {
      final View child = children[i];
      if (child.getParent() != host) {
        // child has been removed
        continue;
      }

      if (child.isLayoutRequested()) {
        // The hosting view doesn't allow children to change sizes dynamically as
        // this would conflict with the component's own layout calculations.
        child.measure(
            MeasureSpec.makeMeasureSpec(child.getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(child.getHeight(), MeasureSpec.EXACTLY));
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }

      if (child instanceof ComponentHost) {
        performLayoutOnChildrenIfNecessary((ComponentHost) child);
      }
    }
  }

  protected void forceRelayout() {
    mForceLayout = true;
    requestLayout();
  }

  public void startTemporaryDetach() {
    mTemporaryDetachedComponent = mComponentTree;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    onAttach();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    onDetach();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onAttachedToWindowForTest() {
    onAttachedToWindow();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onDetachedFromWindowForTest() {
    onDetachedFromWindow();
  }

  @Override
  public void onStartTemporaryDetach() {
    super.onStartTemporaryDetach();
    onDetach();
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    onAttach();
  }

  private void onAttach() {
    if (!mIsAttached) {
      mIsAttached = true;

      if (mComponentTree != null) {
        mComponentTree.attach();
      }

      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));

      AccessibilityManagerCompat.addAccessibilityStateChangeListener(
          mAccessibilityManager, mAccessibilityStateChangeListener);
    }
  }

  private void onDetach() {
    if (mIsAttached) {
      mIsAttached = false;

      if (mDelegateToRenderCore) {
        mMountDelegateTarget.detach();
      } else {
        mMountState.detach();
      }

      if (mComponentTree != null) {
        mComponentTree.detach();
      }

      AccessibilityManagerCompat.removeAccessibilityStateChangeListener(
          mAccessibilityManager, mAccessibilityStateChangeListener);

      mSuppressMeasureComponentTree = false;
    }
  }

  /**
   * If set to true, the onMeasure(..) call won't measure the ComponentTree with the given measure
   * specs, but it will just use them as measured dimensions.
   */
  public void suppressMeasureComponentTree(boolean suppress) {
    mSuppressMeasureComponentTree = suppress;
  }

  /**
   * Sets the width that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  public void setAnimatedWidth(int width) {
    mAnimatedWidth = width;
    requestLayout();
  }

  /**
   * Sets the height that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  public void setAnimatedHeight(int height) {
    mAnimatedHeight = height;
    requestLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("LithoView.onMeasure");
      }
      onMeasureInternal(widthMeasureSpec, heightMeasureSpec);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void onMeasureInternal(int widthMeasureSpec, int heightMeasureSpec) {
    widthMeasureSpec =
        DoubleMeasureFixUtil.correctWidthSpecForAndroidDoubleMeasureBug(
            getResources(), getContext().getPackageManager(), widthMeasureSpec);

    // mAnimatedWidth/mAnimatedHeight >= 0 if something is driving a width/height animation.
    final boolean animating = mAnimatedWidth != -1 || mAnimatedHeight != -1;
    // up to date view sizes, taking into account running animations
    final int upToDateWidth = (mAnimatedWidth != -1) ? mAnimatedWidth : getWidth();
    final int upToDateHeight = (mAnimatedHeight != -1) ? mAnimatedHeight : getHeight();
    mAnimatedWidth = -1;
    mAnimatedHeight = -1;

    if (animating) {
      // If the mount state is dirty, we want to ignore the current animation and calculate the
      // new LayoutState as normal below. That LayoutState has the opportunity to define its own
      // transition to a new width/height from the current height of the LithoView, or if not we
      // will jump straight to that width/height.
      if (!isMountStateDirty()) {
        setMeasuredDimension(upToDateWidth, upToDateHeight);
        return;
      }
    }

    LayoutParams layoutParams = getLayoutParams();
    if (layoutParams instanceof LayoutManagerOverrideParams) {
      LayoutManagerOverrideParams layoutManagerOverrideParams =
          (LayoutManagerOverrideParams) layoutParams;
      final int overrideWidthSpec = layoutManagerOverrideParams.getWidthMeasureSpec();
      if (overrideWidthSpec != LayoutManagerOverrideParams.UNINITIALIZED) {
        widthMeasureSpec = overrideWidthSpec;
      }
      final int overrideHeightSpec = layoutManagerOverrideParams.getHeightMeasureSpec();
      if (overrideHeightSpec != LayoutManagerOverrideParams.UNINITIALIZED) {
        heightMeasureSpec = overrideHeightSpec;
      }
    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
    }

    if (!mForceLayout
        && SizeSpec.getMode(widthMeasureSpec) == SizeSpec.EXACTLY
        && SizeSpec.getMode(heightMeasureSpec) == SizeSpec.EXACTLY) {
      // If the measurements are exact, postpone LayoutState calculation from measure to layout.
      // This is part of the fix for android's double measure bug. Doing this means that if we get
      // remeasured with different exact measurements, we don't compute two layouts.
      mDoMeasureInLayout = true;
      setMeasuredDimension(width, height);
      return;
    }

    mIsMeasuring = true;

    if (mComponentTree != null && !mSuppressMeasureComponentTree) {
      boolean forceRelayout = mForceLayout;
      mForceLayout = false;
      mComponentTree.measure(
          adjustMeasureSpecForPadding(widthMeasureSpec, getPaddingRight() + getPaddingLeft()),
          adjustMeasureSpecForPadding(heightMeasureSpec, getPaddingTop() + getPaddingBottom()),
          sLayoutSize,
          forceRelayout);

      width = sLayoutSize[0];
      height = sLayoutSize[1];
      mDoMeasureInLayout = false;
    }

    if (height == 0) {
      maybeLogInvalidZeroHeight();
    }

    final boolean canAnimateRootBounds =
        !mSuppressMeasureComponentTree
            && mComponentTree != null
            && (!mHasNewComponentTree || !mComponentTree.hasMounted());

    if (canAnimateRootBounds) {
      // We might need to collect transitions before mount to know whether this LithoView has
      // width or height animation.
      mComponentTree.maybeCollectTransitions();

      final int initialAnimatedWidth =
          mComponentTree.getInitialAnimatedLithoViewWidth(upToDateWidth, mHasNewComponentTree);
      if (initialAnimatedWidth != -1) {
        width = initialAnimatedWidth;
      }

      final int initialAnimatedHeight =
          mComponentTree.getInitialAnimatedLithoViewHeight(upToDateHeight, mHasNewComponentTree);
      if (initialAnimatedHeight != -1) {
        height = initialAnimatedHeight;
      }
    }
    setMeasuredDimension(width, height);

    mHasNewComponentTree = false;
    mIsMeasuring = false;
  }

  void maybeCollectAllTransitions(LayoutState layoutState, ComponentTree componentTree) {
    if (mDelegateToRenderCore) {
      if (mIsMountStateDirty) {
        // TODO: can this be a generic callback?
        mLithoHostListenerCoordinator.collectAllTransitions(layoutState);
      }
    } else {
      if (mMountState.isDirty()) {
        mMountState.collectAllTransitions(layoutState, componentTree);
      }
    }
  }

  @Override
  protected void performLayout(boolean changed, int left, int top, int right, int bottom) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("LithoView.performLayout");
      }
      performLayoutInternal(changed, left, top, right, bottom);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void performLayoutInternal(boolean changed, int left, int top, int right, int bottom) {
    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Trying to layout a LithoView holding onto a released ComponentTree");
      }

      if (mDoMeasureInLayout || mComponentTree.getMainThreadLayoutState() == null) {
        final int widthWithoutPadding =
            Math.max(0, right - left - getPaddingRight() - getPaddingLeft());
        final int heightWithoutPadding =
            Math.max(0, bottom - top - getPaddingTop() - getPaddingBottom());

        // Call measure so that we get a layout state that we can use for layout.
        mComponentTree.measure(
            MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY),
            sLayoutSize,
            false);
        mHasNewComponentTree = false;
        mDoMeasureInLayout = false;
      }

      boolean wasMountTriggered = mComponentTree.layout();

      // If this happens the LithoView might have moved on Screen without a scroll event
      // triggering incremental mount. We trigger one here to be sure all the content is visible.
      if (!wasMountTriggered) {
        notifyVisibleBoundsChanged();
      }

      if (!wasMountTriggered || shouldAlwaysLayoutChildren()) {
        // If the layout() call on the component didn't trigger a mount step,
        // we might need to perform an inner layout traversal on children that
        // requested it as certain complex child views (e.g. ViewPager,
        // RecyclerView, etc) rely on that.
        performLayoutOnChildrenIfNecessary(this);
      }
    }
  }

  private static int adjustMeasureSpecForPadding(int measureSpec, int padding) {
    final int mode = MeasureSpec.getMode(measureSpec);
    if (mode == MeasureSpec.UNSPECIFIED) {
      return measureSpec;
    }
    final int size = Math.max(0, MeasureSpec.getSize(measureSpec) - padding);
    return MeasureSpec.makeMeasureSpec(size, mode);
  }

  /**
   * Indicates if the children of this view should be laid regardless to a mount step being
   * triggered on layout. This step can be important when some of the children in the hierarchy are
   * changed (e.g. resized) but the parent wasn't.
   *
   * <p>Since the framework doesn't expect its children to resize after being mounted, this should
   * be used only for extreme cases where the underline views are complex and need this behavior.
   *
   * @return boolean Returns true if the children of this view should be laid out even when a mount
   *     step was not needed.
   */
  protected boolean shouldAlwaysLayoutChildren() {
    return false;
  }

  /**
   * @return {@link ComponentContext} associated with this LithoView. It's a wrapper on the {@link
   *     Context} originally used to create this LithoView itself.
   */
  public ComponentContext getComponentContext() {
    return mComponentContext;
  }

  @Override
  protected boolean shouldRequestLayout() {
    // Don't bubble up layout requests while mounting.
    if (mComponentTree != null && mComponentTree.isMounting()) {
      return false;
    }

    return super.shouldRequestLayout();
  }

  void assertNotInMeasure() {
    if (mIsMeasuring) {
      // If the ComponentTree is updated during measure, the following .layout() call will not run
      // on the ComponentTree that was prepared in measure.
      throw new RuntimeException("Cannot update ComponentTree while in the middle of measure");
    }
  }

  @Nullable
  public ComponentTree getComponentTree() {
    return mComponentTree;
  }

  public synchronized void setOnDirtyMountListener(OnDirtyMountListener onDirtyMountListener) {
    mOnDirtyMountListener = onDirtyMountListener;
  }

  public void setOnPostDrawListener(@Nullable OnPostDrawListener onPostDrawListener) {
    mOnPostDrawListener = onPostDrawListener;
  }

  synchronized void onDirtyMountComplete() {
    if (mOnDirtyMountListener != null) {
      mOnDirtyMountListener.onDirtyMount(this);
    }
  }

  public void setComponentTree(@Nullable ComponentTree componentTree) {
    setComponentTree(componentTree, true);
  }

  public void setComponentTree(
      @Nullable ComponentTree componentTree, boolean unmountAllWhenComponentTreeSetToNull) {
    assertMainThread();
    assertNotInMeasure();

    mTemporaryDetachedComponent = null;
    if (mComponentTree == componentTree) {
      if (mIsAttached) {
        rebind();
      }
      return;
    }

    mHasNewComponentTree =
        mComponentTree == null || componentTree == null || mComponentTree.mId != componentTree.mId;
    setMountStateDirty();

    if (mComponentTree != null) {
      if (componentTree == null && unmountAllWhenComponentTreeSetToNull) {
        unmountAllItems();
      } else if (componentTree != null) {
        clearVisibilityItems();
        clearLastMountedTree();
      }

      if (mInvalidStateLogParams != null) {
        mPreviousComponentSimpleName = mComponentTree.getSimpleName();
      }
      if (componentTree != null
          && componentTree.getLithoView() != null
          && mInvalidStateLogParams != null
          && mInvalidStateLogParams.containsKey(SET_ALREADY_ATTACHED_COMPONENT_TREE)) {
        logSetAlreadyAttachedComponentTree(
            mComponentTree,
            componentTree,
            mInvalidStateLogParams.get(SET_ALREADY_ATTACHED_COMPONENT_TREE));
      }
      if (mIsAttached) {
        mComponentTree.detach();
      }

      mComponentTree.clearLithoView();
    }

    if (componentTree != null && !mDelegateToRenderCore) {
      mMountState.setRecyclingMode(componentTree.getRecyclingMode());
    }

    mComponentTree = componentTree;

    if (mHasNewComponentTree && mDelegateToRenderCore) {
      setupMountExtensions(mComponentTree);
    }

    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Setting a released ComponentTree to a LithoView, "
                + "released component was: "
                + mComponentTree.getReleasedComponent());
      }
      mComponentTree.setLithoView(this);

      if (mIsAttached) {
        mComponentTree.attach();
      } else {
        requestLayout();
      }
    }
    mNullComponentCause = mComponentTree == null ? "set_CT" : null;
  }

  boolean delegateToRenderCore() {
    return mDelegateToRenderCore;
  }

  private void setupMountExtensions(ComponentTree componentTree) {
    if (!mDelegateToRenderCore) {
      throw new IllegalStateException("Using mount extensions is disabled on this LithoView.");
    }

    if (mLithoHostListenerCoordinator == null) {
      mLithoHostListenerCoordinator = new LithoHostListenerCoordinator(mMountDelegateTarget);

      mLithoHostListenerCoordinator.enableNestedLithoViewsExtension();
      mLithoHostListenerCoordinator.enableTransitions();

      if (ComponentsConfiguration.isEndToEndTestRun) {
        mLithoHostListenerCoordinator.enableEndToEndTestProcessing();
      }

      mLithoHostListenerCoordinator.enableViewAttributes();
      mLithoHostListenerCoordinator.enableDynamicProps();
    }

    if (componentTree != null) {
      if (componentTree.isVisibilityProcessingEnabled()) {
        mLithoHostListenerCoordinator.enableVisibilityProcessing(this);
      } else {
        mLithoHostListenerCoordinator.disableVisibilityProcessing();
      }

      if (componentTree.isIncrementalMountEnabled()) {
        mLithoHostListenerCoordinator.enableIncrementalMount();
      } else {
        mLithoHostListenerCoordinator.disableIncrementalMount();
      }
    }

    mLithoHostListenerCoordinator.setCollectNotifyVisibleBoundsChangedCalls(true);
  }

  /** Change the root component synchronously. */
  public void setComponent(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRoot(component);
    }
  }

  /**
   * Change the root component synchronously. Creates a new component tree with reconciliation
   * disabled if required. <b>DO NOT USE</b> this method; it was added only to deprecate the current
   * usages of {@link #setComponent(Component)}.
   *
   * @deprecated Use {@link #getComponentTree()} and {@link ComponentTree#setRoot(Component)}
   *     instead; set the config explicitly on the {@link ComponentTree} using {@link
   *     ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public void setComponentWithoutReconciliation(Component component) {
    if (mComponentTree == null) {
      setComponentTree(
          ComponentTree.create(getComponentContext(), component)
              .isReconciliationEnabled(false)
              .build());
    } else {
      mComponentTree.setRoot(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI. If this
   * {@link LithoView} doesn't have a ComponentTree initialized, the root will be computed
   * synchronously.
   */
  public void setComponentAsync(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI. If this
   * {@link LithoView} doesn't have a ComponentTree initialized, the root will be computed
   * synchronously with reconciliation disabled. <b>DO NOT USE</b> this method; it was added only to
   * deprecate the current usages of {@link #setComponentAsync(Component)}.
   *
   * @deprecated Use {@link #getComponentTree()} and {@link ComponentTree#setRootAsync(Component)}
   *     instead; set the config explicitly on the {@link ComponentTree} using {@link
   *     ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public void setComponentAsyncWithoutReconciliation(Component component) {
    if (mComponentTree == null) {
      setComponentTree(
          ComponentTree.create(getComponentContext(), component)
              .isReconciliationEnabled(false)
              .build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  public void rebind() {
    if (mDelegateToRenderCore) {
      mMountDelegateTarget.attach();
    } else {
      mMountState.rebind();
    }
  }

  /**
   * To be called this when the LithoView is about to become inactive. This means that either the
   * view is about to be recycled or moved off-screen.
   */
  public void unbind() {
    if (mDelegateToRenderCore) {
      mMountDelegateTarget.detach();
    } else {
      mMountState.unbind();
    }
  }

  /**
   * If true, calling {@link #setVisibilityHint(boolean, boolean)} will delegate to {@link
   * #setVisibilityHint(boolean)} and skip mounting if the visibility hint was set to false. You
   * should not need this unless you don't have control over calling setVisibilityHint on the
   * LithoView you own.
   */
  public void setSkipMountingIfNotVisible(boolean skipMountingIfNotVisible) {
    assertMainThread();
    mSkipMountingIfNotVisible = skipMountingIfNotVisible;
  }

  void resetVisibilityHint() {
    mHasVisibilityHint = false;
    mPauseMountingWhileVisibilityHintFalse = false;
  }

  void setVisibilityHintNonRecursive(boolean isVisible) {
    assertMainThread();

    if (mComponentTree == null) {
      return;
    }

    if (!mHasVisibilityHint && isVisible) {
      return;
    }

    // If the LithoView previously had the visibility hint set to false, then when it's set back
    // to true we should trigger a mount, in case the visible bounds changed while mounting was
    // paused.
    mHasVisibilityHint = true;
    mPauseMountingWhileVisibilityHintFalse = true;

    final boolean forceMount = shouldPauseMountingWithVisibilityHintFalse();
    mVisibilityHintIsVisible = isVisible;

    if (isVisible) {
      if (forceMount) {
        notifyVisibleBoundsChanged();
      } else if (getLocalVisibleRect(mRect)) {
        processVisibilityOutputs(mRect);
      }
      // if false: no-op, doesn't have visible area, is not ready or not attached
    } else {
      clearVisibilityItems();
    }
  }

  /**
   * @return true if this LithoView has a ComponentTree attached and a LithoLifecycleProvider is set
   *     on it, false otherwise.
   */
  public synchronized boolean componentTreeHasLifecycleProvider() {
    return mComponentTree != null && mComponentTree.isSubscribedToLifecycleProvider();
  }

  /**
   * If this LithoView has a ComponentTree attached to it, set a LithoLifecycleProvider if it
   * doesn't already have one.
   *
   * @return true if the LithoView's ComponentTree was subscribed as listener to the given
   *     LithoLifecycleProvider, false otherwise.
   */
  public synchronized boolean subscribeComponentTreeToLifecycleProvider(
      LithoLifecycleProvider lifecycleProvider) {
    if (mComponentTree == null) {
      return false;
    }

    if (mComponentTree.isSubscribedToLifecycleProvider()) {
      return false;
    }

    mComponentTree.subscribeToLifecycleProvider(lifecycleProvider);
    return true;
  }

  /**
   * Deprecated: Consider subscribing the LithoView to a LithoLifecycleOwner instead.
   *
   * <p>Call this to tell the LithoView whether it is visible or not. In general, you shouldn't
   * require this as the system will do this for you. However, when a new activity/fragment is added
   * on top of the one hosting this view, the LithoView remains in the backstack but receives no
   * callback to indicate that it is no longer visible.
   *
   * <p>While the LithoView has the visibility hint set to false, it will be treated by the
   * framework as not in the viewport, so no new mounting events will be processed until the
   * visibility hint is set back to true.
   *
   * @param isVisible if true, this will find the current visible rect and process visibility
   *     outputs using it. If false, any invisible and unfocused events will be called.
   */
  @Deprecated
  public void setVisibilityHint(boolean isVisible) {
    setVisibilityHintInternal(isVisible, true);
  }

  /**
   * Marked as @Deprecated. {@link #setVisibilityHint(boolean)} should be used instead, which by
   * default does not process new mount events while the visibility hint is set to false
   * (skipMountingIfNotVisible should be set to true). This method should only be used to maintain
   * the contract with the usages of setVisibilityHint before `skipMountingIfNotVisible` was made to
   * default to true. All usages should be audited and migrated to {@link
   * #setVisibilityHint(boolean)}.
   */
  @Deprecated
  public void setVisibilityHint(boolean isVisible, boolean skipMountingIfNotVisible) {
    if (mSkipMountingIfNotVisible) {
      setVisibilityHint(isVisible);

      return;
    }

    setVisibilityHintInternal(isVisible, skipMountingIfNotVisible);
  }

  private void setVisibilityHintInternal(boolean isVisible, boolean skipMountingIfNotVisible) {
    assertMainThread();
    if (componentTreeHasLifecycleProvider()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          LITHO_LIFECYCLE_FOUND,
          "Setting visibility hint but a LithoLifecycleProvider was found, ignoring.");

      return;
    }

    if (mComponentTree == null) {
      return;
    }

    // If the LithoView previously had the visibility hint set to false, then when it's set back
    // to true we should trigger a mount, in case the visible bounds changed while mounting was
    // paused.
    mHasVisibilityHint = true;
    mPauseMountingWhileVisibilityHintFalse = skipMountingIfNotVisible;

    final boolean forceMount = shouldPauseMountingWithVisibilityHintFalse();
    mVisibilityHintIsVisible = isVisible;

    if (isVisible) {
      if (forceMount) {
        notifyVisibleBoundsChanged();
      } else if (getLocalVisibleRect(mRect)) {
        processVisibilityOutputs(mRect);
      }
      recursivelySetVisibleHint(true, skipMountingIfNotVisible);
      // if false: no-op, doesn't have visible area, is not ready or not attached
    } else {
      recursivelySetVisibleHint(false, skipMountingIfNotVisible);
      clearVisibilityItems();
    }
  }

  private void clearVisibilityItems() {
    if (mDelegateToRenderCore && mLithoHostListenerCoordinator != null) {
      final VisibilityMountExtension visibilityOutputsExtension =
          mLithoHostListenerCoordinator.getVisibilityExtension();
      if (visibilityOutputsExtension != null) {
        VisibilityMountExtension.clearVisibilityItems(
            mMountDelegateTarget.getExtensionState(visibilityOutputsExtension));
      }
    } else {
      mMountState.clearVisibilityItems();
    }
  }

  /** This should be called when setting a null component tree to the litho view. */
  private void clearLastMountedTree() {
    if (mDelegateToRenderCore && mLithoHostListenerCoordinator != null) {
      mLithoHostListenerCoordinator.clearLastMountedTreeId();
    } else {
      mMountState.clearLastMountedTree();
    }
  }

  private void recursivelySetVisibleHint(boolean isVisible, boolean skipMountingIfNotVisible) {
    final List<LithoView> childLithoViews = getChildLithoViewsFromCurrentlyMountedItems();
    for (int i = childLithoViews.size() - 1; i >= 0; i--) {
      final LithoView lithoView = childLithoViews.get(i);
      lithoView.setVisibilityHint(isVisible, skipMountingIfNotVisible);
    }
  }

  @Override
  public void setHasTransientState(boolean hasTransientState) {
    super.setHasTransientState(hasTransientState);

    if (hasTransientState) {
      if (mTransientStateCount == 0 && mComponentTree != null) {
        notifyVisibleBoundsChanged(new Rect(0, 0, getWidth(), getHeight()), false);
      }
      mTransientStateCount++;
    } else {
      mTransientStateCount--;
      if (mTransientStateCount == 0 && mComponentTree != null) {
        // We mounted everything when the transient state was set on this view. We need to do this
        // partly to unmount content that is not visible but mostly to get the correct visibility
        // events to be fired.
        notifyVisibleBoundsChanged();
      }
      if (mTransientStateCount < 0) {
        mTransientStateCount = 0;
      }
    }
  }

  @Override
  public void offsetTopAndBottom(int offset) {
    super.offsetTopAndBottom(offset);

    onOffsetOrTranslationChange();
  }

  @Override
  public void offsetLeftAndRight(int offset) {
    super.offsetLeftAndRight(offset);

    onOffsetOrTranslationChange();
  }

  @Override
  public void setTranslationX(float translationX) {
    if (translationX == getTranslationX()) {
      return;
    }
    super.setTranslationX(translationX);

    onOffsetOrTranslationChange();
  }

  @Override
  public void setTranslationY(float translationY) {
    if (translationY == getTranslationY()) {
      return;
    }
    super.setTranslationY(translationY);

    onOffsetOrTranslationChange();
  }

  @Override
  public void draw(Canvas canvas) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("LithoView.draw");
      }
      drawInternal(canvas);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void drawInternal(Canvas canvas) {
    try {
      canvas.translate(getPaddingLeft(), getPaddingTop());
      super.draw(canvas);
    } catch (Throwable t) {
      throw new LithoMetadataExceptionWrapper(mComponentTree, t);
    }

    if (mOnPostDrawListener != null) {
      mOnPostDrawListener.onPostDraw();
    }
  }

  private void onOffsetOrTranslationChange() {
    if (mComponentTree == null || !(getParent() instanceof View)) {
      return;
    }

    int parentWidth = ((View) getParent()).getWidth();
    int parentHeight = ((View) getParent()).getHeight();

    final int translationX = (int) getTranslationX();
    final int translationY = (int) getTranslationY();
    final int top = getTop() + translationY;
    final int bottom = getBottom() + translationY;
    final int left = getLeft() + translationX;
    final int right = getRight() + translationX;
    final Rect previousRect = mPreviousMountVisibleRectBounds;

    if (left >= 0
        && top >= 0
        && right <= parentWidth
        && bottom <= parentHeight
        && previousRect.left >= 0
        && previousRect.top >= 0
        && previousRect.right <= parentWidth
        && previousRect.bottom <= parentHeight
        && previousRect.width() == getWidth()
        && previousRect.height() == getHeight()) {
      // View is fully visible, and has already been completely mounted.
      return;
    }

    final Rect rect = new Rect();
    if (!getLocalVisibleRect(rect)) {
      // View is not visible at all, nothing to do.
      return;
    }

    notifyVisibleBoundsChanged(rect, true);
  }

  public void notifyVisibleBoundsChanged(Rect visibleRect, boolean processVisibilityOutputs) {
    if (mComponentTree == null || mComponentTree.getMainThreadLayoutState() == null) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.mountComponent(visibleRect, processVisibilityOutputs);
    } else if (processVisibilityOutputs) {
      processVisibilityOutputs(visibleRect);
    }
  }

  @Override
  public void notifyVisibleBoundsChanged() {
    if (mComponentTree == null || mComponentTree.getMainThreadLayoutState() == null) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.incrementalMountComponent();
    } else {
      processVisibilityOutputs();
    }
  }

  public boolean isIncrementalMountEnabled() {
    return (mComponentTree != null && mComponentTree.isIncrementalMountEnabled());
  }

  /** Deprecated: Consider subscribing the LithoView to a LithoLifecycleOwner instead. */
  @Deprecated
  public void release() {
    assertMainThread();
    if (componentTreeHasLifecycleProvider()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          LITHO_LIFECYCLE_FOUND,
          "Trying to release a LithoView but a LithoLifecycleProvider was found, ignoring.");

      return;
    }

    final List<LithoView> childrenLithoViews = getChildLithoViewsFromCurrentlyMountedItems();
    if (childrenLithoViews != null) {
      for (LithoView child : childrenLithoViews) {
        child.release();
      }
    }

    if (mComponentTree != null) {
      mComponentTree.release();
      mComponentTree = null;
      mNullComponentCause = "release_CT";
    }
  }

  // We pause mounting while the visibility hint is set to false, because the visible rect of
  // the LithoView is not consistent with what's currently on screen.
  private boolean shouldPauseMountingWithVisibilityHintFalse() {
    return mPauseMountingWhileVisibilityHintFalse
        && mHasVisibilityHint
        && !mVisibilityHintIsVisible;
  }

  void mount(
      LayoutState layoutState,
      @Nullable Rect currentVisibleArea,
      boolean processVisibilityOutputs) {

    if (shouldPauseMountingWithVisibilityHintFalse()) {
      return;
    }

    if (mTransientStateCount > 0
        && mComponentTree != null
        && mComponentTree.isIncrementalMountEnabled()) {
      // If transient state is set but the MountState is dirty we want to re-mount everything.
      // Otherwise, we don't need to do anything as the entire LithoView was mounted when the
      // transient state was set.
      if (!isMountStateDirty()) {
        return;
      } else {
        currentVisibleArea = new Rect(0, 0, getWidth(), getHeight());
        processVisibilityOutputs = false;
      }
    }

    if (currentVisibleArea == null) {
      mPreviousMountVisibleRectBounds.setEmpty();
    } else {
      mPreviousMountVisibleRectBounds.set(currentVisibleArea);
    }

    final boolean loggedFirstMount =
        MountStartupLoggingInfo.maybeLogFirstMountStart(mMountStartupLoggingInfo);
    final boolean loggedLastMount =
        MountStartupLoggingInfo.maybeLogLastMountStart(mMountStartupLoggingInfo, this);

    if (mDelegateToRenderCore) {
      mountWithMountDelegateTarget(layoutState, currentVisibleArea);
    } else {
      mMountState.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
    }

    mIsMountStateDirty = false;

    if (loggedFirstMount) {
      MountStartupLoggingInfo.logFirstMountEnd(mMountStartupLoggingInfo);
    }
    if (loggedLastMount) {
      MountStartupLoggingInfo.logLastMountEnd(mMountStartupLoggingInfo);
    }
  }

  private void mountWithMountDelegateTarget(
      LayoutState layoutState, @Nullable Rect currentVisibleArea) {
    final boolean needsMount = isMountStateDirty() || mountStateNeedsRemount();
    if (currentVisibleArea != null && !needsMount) {
      mLithoHostListenerCoordinator.onVisibleBoundsChanged(currentVisibleArea);
    } else if (mDelegateToRenderCore) {
      // Generate the renderTree here so that any operations
      // that occur in toRenderTree() happen prior to "beforeMount".
      final RenderTree renderTree = layoutState.toRenderTree();
      mLithoHostListenerCoordinator.beforeMount(layoutState, currentVisibleArea);
      mMountDelegateTarget.mount(renderTree);
      mLithoHostListenerCoordinator.afterMount();
    } else {
      ((MountState) mMountDelegateTarget).mount(layoutState);
    }
  }

  /**
   * Dispatch a visibility events to all the components hosted in this LithoView.
   *
   * <p>Marked as @Deprecated to indicate this method is experimental and should not be widely used.
   *
   * <p>NOTE: Can only be used when Incremental Mount is disabled! Call this method when the
   * LithoView is considered eligible for the visibility event (i.e. only dispatch VisibleEvent when
   * the LithoView is visible in its container).
   *
   * @param visibilityEventType The class type of the visibility event to dispatch. Supported:
   *     VisibleEvent.class, InvisibleEvent.class, FocusedVisibleEvent.class,
   *     UnfocusedVisibleEvent.class, FullImpressionVisibleEvent.class.
   */
  @Deprecated
  public void dispatchVisibilityEvent(Class<?> visibilityEventType) {
    if (isIncrementalMountEnabled()) {
      throw new IllegalStateException(
          "dispatchVisibilityEvent - "
              + "Can't manually trigger visibility events when incremental mount is enabled");
    }

    LayoutState layoutState =
        mComponentTree == null ? null : mComponentTree.getMainThreadLayoutState();

    if (layoutState != null && visibilityEventType != null) {
      for (int i = 0; i < layoutState.getVisibilityOutputCount(); i++) {
        dispatchVisibilityEvent(layoutState.getVisibilityOutputAt(i), visibilityEventType);
      }

      List<LithoView> childViews = getChildLithoViewsFromCurrentlyMountedItems();
      for (LithoView lithoView : childViews) {
        lithoView.dispatchVisibilityEvent(visibilityEventType);
      }
    }
  }

  private List<LithoView> getChildLithoViewsFromCurrentlyMountedItems() {
    if (mDelegateToRenderCore) {
      return getChildLithoViewsFromCurrentlyMountedItems(mMountDelegateTarget);
    }

    return mMountState.getChildLithoViewsFromCurrentlyMountedItems();
  }

  private static List<LithoView> getChildLithoViewsFromCurrentlyMountedItems(
      MountDelegateTarget mountDelegateTarget) {
    final ArrayList<LithoView> childLithoViews = new ArrayList<>();

    for (int i = 0, size = mountDelegateTarget.getMountItemCount(); i < size; i++) {
      final Object content = mountDelegateTarget.getContentAt(i);
      if (content instanceof HasLithoViewChildren) {
        ((HasLithoViewChildren) content).obtainLithoViewChildren(childLithoViews);
      }
    }

    return childLithoViews;
  }

  private void dispatchVisibilityEvent(
      VisibilityOutput visibilityOutput, Class<?> visibilityEventType) {
    final MountDelegateTarget target =
        mMountDelegateTarget != null ? mMountDelegateTarget : mMountState;
    final Object content =
        visibilityOutput.hasMountableContent
            ? target.getContentById(visibilityOutput.mRenderUnitId)
            : null;
    if (visibilityEventType == VisibleEvent.class) {
      if (visibilityOutput.getVisibleEventHandler() != null) {
        VisibilityUtils.dispatchOnVisible(visibilityOutput.getVisibleEventHandler(), content);
      }
    } else if (visibilityEventType == InvisibleEvent.class) {
      if (visibilityOutput.getInvisibleEventHandler() != null) {
        VisibilityUtils.dispatchOnInvisible(visibilityOutput.getInvisibleEventHandler());
      }
    } else if (visibilityEventType == FocusedVisibleEvent.class) {
      if (visibilityOutput.getFocusedEventHandler() != null) {
        VisibilityUtils.dispatchOnFocused(visibilityOutput.getFocusedEventHandler());
      }
    } else if (visibilityEventType == UnfocusedVisibleEvent.class) {
      if (visibilityOutput.getUnfocusedEventHandler() != null) {
        VisibilityUtils.dispatchOnUnfocused(visibilityOutput.getUnfocusedEventHandler());
      }
    } else if (visibilityEventType == FullImpressionVisibleEvent.class) {
      if (visibilityOutput.getFullImpressionEventHandler() != null) {
        VisibilityUtils.dispatchOnFullImpression(visibilityOutput.getFullImpressionEventHandler());
      }
    }
  }

  // This only gets called if extensions are disabled.
  private void processVisibilityOutputs() {
    final Rect currentVisibleArea = new Rect();
    final boolean visible = getLocalVisibleRect(currentVisibleArea);
    if (!visible) {
      currentVisibleArea.setEmpty();
    }

    processVisibilityOutputs(currentVisibleArea);
  }

  @VisibleForTesting
  void processVisibilityOutputs(Rect currentVisibleArea) {
    if (mComponentTree == null || !mComponentTree.isVisibilityProcessingEnabled()) {
      return;
    }

    final LayoutState layoutState = mComponentTree.getMainThreadLayoutState();

    if (layoutState == null) {
      Log.w(TAG, "Main Thread Layout state is not found");
      return;
    }

    if (mLithoHostListenerCoordinator != null) {
      mLithoHostListenerCoordinator.processVisibilityOutputs(
          currentVisibleArea, isMountStateDirty());
    } else {
      mMountState.processVisibilityOutputs(
          layoutState,
          currentVisibleArea,
          mPreviousMountVisibleRectBounds,
          isMountStateDirty(),
          null);
    }

    mPreviousMountVisibleRectBounds.set(currentVisibleArea);
  }

  /** Deprecated: Consider subscribing the LithoView to a LithoLifecycleOwner instead. */
  @Deprecated
  public void unmountAllItems() {
    if (mDelegateToRenderCore) {
      mMountDelegateTarget.unmountAllItems();
    } else {
      mMountState.unmountAllItems();
    }

    mPreviousMountVisibleRectBounds.setEmpty();
  }

  public Rect getPreviousMountBounds() {
    return mPreviousMountVisibleRectBounds;
  }

  void setMountStateDirty() {
    if (mDelegateToRenderCore) {
      mIsMountStateDirty = true;
    } else {
      mMountState.setDirty();
    }

    mPreviousMountVisibleRectBounds.setEmpty();
  }

  boolean isMountStateDirty() {
    if (mDelegateToRenderCore) {
      return mIsMountStateDirty;
    }

    return mMountState.isDirty();
  }

  boolean mountStateNeedsRemount() {
    if (mDelegateToRenderCore) {
      return mMountDelegateTarget.needsRemount();
    }

    return mMountState.needsRemount();
  }

  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public MountDelegateTarget getMountDelegateTarget() {
    return mDelegateToRenderCore ? mMountDelegateTarget : mMountState;
  }

  @Nullable
  @VisibleForTesting
  public DynamicPropsManager getDynamicPropsManager() {
    if (mMountState != null) {
      return mMountState.getDynamicPropsManager();
    } else if (mLithoHostListenerCoordinator != null) {
      return mLithoHostListenerCoordinator.getDynamicPropsManager();
    } else {
      return null;
    }
  }

  public void setMountStartupLoggingInfo(
      LithoStartupLogger startupLogger,
      String startupLoggerAttribution,
      boolean[] firstMountCalled,
      boolean[] lastMountCalled,
      boolean isLastAdapterItem,
      boolean isOrientationVertical) {

    mMountStartupLoggingInfo =
        new MountStartupLoggingInfo(
            startupLogger,
            startupLoggerAttribution,
            firstMountCalled,
            lastMountCalled,
            isLastAdapterItem,
            isOrientationVertical);
  }

  public void resetMountStartupLoggingInfo() {
    mMountStartupLoggingInfo = null;
  }

  /** Register for particular invalid state logs. */
  public void setInvalidStateLogParamsList(@Nullable List<ComponentLogParams> logParamsList) {
    if (logParamsList == null) {
      mInvalidStateLogParams = null;
    } else {
      mInvalidStateLogParams = new HashMap<>();
      for (int i = 0, size = logParamsList.size(); i < size; i++) {
        final ComponentLogParams logParams = logParamsList.get(i);
        mInvalidStateLogParams.put(logParams.logType, logParams);
      }
    }
  }

  private void maybeLogInvalidZeroHeight() {
    if (mComponentTree != null
        && mComponentTree.getMainThreadLayoutState() != null
        && mComponentTree.getMainThreadLayoutState().mLayoutRoot == null) {
      // Valid case for 0-height, onCreateLayout of root component returned null.
      return;
    }

    final ComponentLogParams logParams =
        mInvalidStateLogParams == null ? null : mInvalidStateLogParams.get(ZERO_HEIGHT_LOG);
    if (logParams == null) {
      // surface didn't subscribe for this type of logging.
      return;
    }

    final LayoutParams layoutParams = getLayoutParams();
    final boolean isViewBeingRemovedInPreLayoutOfPredictiveAnim =
        layoutParams instanceof LayoutManagerOverrideParams
            && ((LayoutManagerOverrideParams) layoutParams).hasValidAdapterPosition();

    if (isViewBeingRemovedInPreLayoutOfPredictiveAnim) {
      return;
    }

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(logParams.logProductId);
    messageBuilder.append("-");
    messageBuilder.append(ZERO_HEIGHT_LOG);
    messageBuilder.append(", current=");
    messageBuilder.append(
        (mComponentTree == null ? "null_" + mNullComponentCause : mComponentTree.getSimpleName()));
    messageBuilder.append(", previous=");
    messageBuilder.append(mPreviousComponentSimpleName);
    messageBuilder.append(", view=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(this));
    logError(messageBuilder.toString(), ZERO_HEIGHT_LOG, logParams);
  }

  private void logSetAlreadyAttachedComponentTree(
      ComponentTree currentComponentTree,
      ComponentTree newComponentTree,
      ComponentLogParams logParams) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(logParams.logProductId);
    messageBuilder.append("-");
    messageBuilder.append(SET_ALREADY_ATTACHED_COMPONENT_TREE);
    messageBuilder.append(", currentView=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(currentComponentTree.getLithoView()));
    messageBuilder.append(", newComponent.LV=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(newComponentTree.getLithoView()));
    messageBuilder.append(", currentComponent=");
    messageBuilder.append(currentComponentTree.getSimpleName());
    messageBuilder.append(", newComponent=");
    messageBuilder.append(newComponentTree.getSimpleName());
    logError(messageBuilder.toString(), SET_ALREADY_ATTACHED_COMPONENT_TREE, logParams);
  }

  private static void logError(String message, String categoryKey, ComponentLogParams logParams) {
    final ComponentsReporter.LogLevel logLevel =
        logParams.failHarder
            ? ComponentsReporter.LogLevel.FATAL
            : ComponentsReporter.LogLevel.ERROR;
    ComponentsReporter.emitMessage(logLevel, categoryKey, message, logParams.samplingFrequency);
  }

  @DoNotStrip
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Deque<TestItem> findTestItems(String testKey) {
    if (mDelegateToRenderCore) {
      if (mLithoHostListenerCoordinator == null) {
        return new LinkedList<>();
      }

      if (mLithoHostListenerCoordinator.getEndToEndTestingExtension() == null) {
        throw new IllegalStateException(
            "Trying to access TestItems while "
                + "ComponentsConfiguration.isEndToEndTestRun is false.");
      }

      return mLithoHostListenerCoordinator.getEndToEndTestingExtension().findTestItems(testKey);
    } else {
      return mMountState.findTestItems(testKey);
    }
  }

  private static class AccessibilityStateChangeListener
      extends AccessibilityStateChangeListenerCompat {
    private final WeakReference<LithoView> mLithoView;

    private AccessibilityStateChangeListener(LithoView lithoView) {
      mLithoView = new WeakReference<>(lithoView);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled();
      final LithoView lithoView = mLithoView.get();
      if (lithoView == null) {
        return;
      }

      lithoView.rerenderForAccessibility(enabled);
    }
  }

  public void rerenderForAccessibility(boolean enabled) {
    refreshAccessibilityDelegatesIfNeeded(enabled);
    // must force (not just request)
    forceRelayout();
  }

  /**
   * LayoutParams that override the LayoutManager.
   *
   * <p>If you set LayoutParams on a LithoView that implements this interface, the view will
   * completely ignore the layout specs given to it by its LayoutManager and use these specs
   * instead. To use, set the LayoutParams height and width to {@link
   * ViewGroup.LayoutParams#WRAP_CONTENT} and then provide a width and height measure spec though
   * this interface.
   *
   * <p>This is helpful for implementing {@link View.MeasureSpec#AT_MOST} support since Android
   * LayoutManagers don't support an AT_MOST concept as part of {@link ViewGroup.LayoutParams}'s
   * special values.
   */
  public interface LayoutManagerOverrideParams {

    int UNINITIALIZED = -1;

    int getWidthMeasureSpec();

    int getHeightMeasureSpec();

    // TODO T30527513 Remove after fixing 0 height issues.
    boolean hasValidAdapterPosition();
  }

  @Override
  public String toString() {
    // dump this view and include litho internal UI data
    return super.toString() + LithoViewTestHelper.viewToString(this, true);
  }

  static class MountStartupLoggingInfo {
    private final LithoStartupLogger startupLogger;
    private final String startupLoggerAttribution;
    private final boolean[] firstMountLogged;
    private final boolean[] lastMountLogged;
    private final boolean isLastAdapterItem;
    private final boolean isOrientationVertical;

    MountStartupLoggingInfo(
        LithoStartupLogger startupLogger,
        String startupLoggerAttribution,
        boolean[] firstMountLogged,
        boolean[] lastMountLogged,
        boolean isLastAdapterItem,
        boolean isOrientationVertical) {
      this.startupLogger = startupLogger;
      this.startupLoggerAttribution = startupLoggerAttribution;
      this.firstMountLogged = firstMountLogged;
      this.lastMountLogged = lastMountLogged;
      this.isLastAdapterItem = isLastAdapterItem;
      this.isOrientationVertical = isOrientationVertical;
    }

    static boolean maybeLogFirstMountStart(@Nullable MountStartupLoggingInfo loggingInfo) {
      if (loggingInfo != null
          && LithoStartupLogger.isEnabled(loggingInfo.startupLogger)
          && loggingInfo.firstMountLogged != null
          && !loggingInfo.firstMountLogged[0]) {
        loggingInfo.startupLogger.markPoint(
            LithoStartupLogger.FIRST_MOUNT,
            LithoStartupLogger.START,
            loggingInfo.startupLoggerAttribution);
        return true;
      }
      return false;
    }

    static boolean maybeLogLastMountStart(
        @Nullable MountStartupLoggingInfo loggingInfo, LithoView lithoView) {
      if (loggingInfo != null
          && LithoStartupLogger.isEnabled(loggingInfo.startupLogger)
          && loggingInfo.firstMountLogged != null
          && loggingInfo.firstMountLogged[0]
          && loggingInfo.lastMountLogged != null
          && !loggingInfo.lastMountLogged[0]) {

        final ViewGroup parent = (ViewGroup) lithoView.getParent();
        if (parent == null) {
          return false;
        }

        if (loggingInfo.isLastAdapterItem
            || (loggingInfo.isOrientationVertical
                ? lithoView.getBottom() >= parent.getHeight() - parent.getPaddingBottom()
                : lithoView.getRight() >= parent.getWidth() - parent.getPaddingRight())) {
          loggingInfo.startupLogger.markPoint(
              LithoStartupLogger.LAST_MOUNT,
              LithoStartupLogger.START,
              loggingInfo.startupLoggerAttribution);
          return true;
        }
      }
      return false;
    }

    static void logFirstMountEnd(MountStartupLoggingInfo loggingInfo) {
      loggingInfo.startupLogger.markPoint(
          LithoStartupLogger.FIRST_MOUNT,
          LithoStartupLogger.END,
          loggingInfo.startupLoggerAttribution);
      loggingInfo.firstMountLogged[0] = true;
    }

    static void logLastMountEnd(MountStartupLoggingInfo loggingInfo) {
      loggingInfo.startupLogger.markPoint(
          LithoStartupLogger.LAST_MOUNT,
          LithoStartupLogger.END,
          loggingInfo.startupLoggerAttribution);
      loggingInfo.lastMountLogged[0] = true;
    }
  }

  @Override
  public void setRenderState(RenderState renderState) {
    throw new UnsupportedOperationException("Not currently supported by Litho");
  }

  @Override
  protected Map<String, Object> getLayoutErrorMetadata(int width, int height) {
    final Map<String, Object> metadata = super.getLayoutErrorMetadata(width, height);

    final @Nullable ComponentTree tree = getComponentTree();
    if (tree == null) {
      metadata.put("lithoView", null);
      return metadata;
    }

    final Map<String, Object> lithoSpecific = new HashMap<>();
    metadata.put("lithoView", lithoSpecific);
    if (tree.getRoot() == null) {
      lithoSpecific.put("root", null);
      return metadata;
    }

    lithoSpecific.put("root", tree.getRoot().getSimpleName());
    lithoSpecific.put("tree", ComponentTreeDumpingHelper.dumpContextTree(tree.getContext()));

    return metadata;
  }
}
