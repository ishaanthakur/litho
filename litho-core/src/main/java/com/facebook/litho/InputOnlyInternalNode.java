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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.facebook.litho.Component.isLayoutSpecWithSizeSpec;
import static com.facebook.litho.Layout.hostIsCompatible;
import static com.facebook.litho.Layout.isLayoutDirectionRTL;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;

import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.rendercore.Copyable;
import com.facebook.rendercore.RenderState;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default input only implementation of {@link InternalNode}. This does not contain a {@link
 * YogaNode}.
 */
@OkToExtend
@ThreadConfined(ThreadConfined.ANY)
public class InputOnlyInternalNode<Writer extends YogaLayoutProps>
    implements InternalNode, Cloneable {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  private static final String INVALID_LAYOUT_PROPS = "YogaLayoutProps:ContextSpecificStyleSet";

  // Flags used to indicate that a certain attribute was explicitly set on the node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L;
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  protected static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  protected static final long PFLAG_PADDING_IS_SET = 1L << 10;
  protected static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  protected static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  protected static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  protected static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  protected static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  protected static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  protected static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 24;
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 25;
  protected static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 27;
  protected static final long PFLAG_BORDER_IS_SET = 1L << 28;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_SET = 1L << 29;
  protected static final long PFLAG_STATE_LIST_ANIMATOR_RES_SET = 1L << 30;
  protected static final long PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET = 1L << 31;
  protected static final long PFLAG_TRANSITION_KEY_TYPE_IS_SET = 1L << 32;
  protected static final long PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET = 1L << 33;

  private List<InternalNode> mChildren = new ArrayList<>(4);

  protected Context mContext;

  @ThreadConfined(ThreadConfined.ANY)
  protected List<Component> mComponents = new ArrayList<>(2);

  @ThreadConfined(ThreadConfined.ANY)
  private @Nullable List<ScopedComponentInfo> mScopedComponentInfos;

  @ThreadConfined(ThreadConfined.ANY)
  private List<String> mComponentGlobalKeys = new ArrayList<>(2);

  protected final int[] mBorderEdgeWidths = new int[Border.EDGE_COUNT];
  protected final int[] mBorderColors = new int[Border.EDGE_COUNT];
  protected final float[] mBorderRadius = new float[Border.RADIUS_COUNT];

  protected @Nullable NodeInfo mNodeInfo;
  protected @Nullable EventHandler<VisibleEvent> mVisibleHandler;
  protected @Nullable EventHandler<FocusedVisibleEvent> mFocusedHandler;
  protected @Nullable EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  protected @Nullable EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  protected @Nullable EventHandler<InvisibleEvent> mInvisibleHandler;
  protected @Nullable EventHandler<VisibilityChangedEvent> mVisibilityChangedHandler;
  protected @Nullable Drawable mBackground;
  protected @Nullable Drawable mForeground;
  protected @Nullable PathEffect mBorderPathEffect;
  protected @Nullable StateListAnimator mStateListAnimator;
  private @Nullable Edges mTouchExpansion;
  protected @Nullable String mTransitionKey;
  protected @Nullable String mTransitionOwnerKey;
  protected @Nullable Transition.TransitionKeyType mTransitionKeyType;
  private @Nullable ArrayList<Transition> mTransitions;
  private @Nullable Map<String, Component> mComponentsNeedingPreviousRenderData;
  private @Nullable Map<String, ScopedComponentInfo> mScopedComponentInfosNeedingPreviousRenderData;
  private @Nullable ArrayList<WorkingRangeContainer.Registration> mWorkingRangeRegistrations;
  private @Nullable ArrayList<Attachable> mAttachables;
  protected @Nullable String mTestKey;
  private @Nullable Set<DebugComponent> mDebugComponents;
  private @Nullable List<Component> mUnresolvedComponents;
  protected @Nullable Paint mLayerPaint;

  protected boolean mIsPaddingSet;
  protected boolean mDuplicateParentState;
  protected boolean mDuplicateChildrenStates;
  protected boolean mForceViewWrapping;

  protected int mLayerType = LayerType.LAYER_TYPE_NOT_SET;
  protected int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  protected @DrawableRes int mStateListAnimatorRes;

  protected float mVisibleHeightRatio;
  protected float mVisibleWidthRatio;

  protected @Nullable YogaDirection mLayoutDirection;
  protected @Nullable YogaFlexDirection mFlexDirection;
  protected @Nullable YogaJustify mJustifyContent;
  protected @Nullable YogaAlign mAlignContent;
  protected @Nullable YogaAlign mAlignItems;
  protected @Nullable YogaWrap mYogaWrap;

  private @Nullable InputOnlyNestedTreeHolder mNestedTreeHolder;
  private @Nullable Edges mNestedPaddingEdges;
  private @Nullable boolean[] mNestedIsPaddingPercent;

  protected @Nullable YogaMeasureFunction mYogaMeasureFunction;

  private @Nullable CommonProps.DefaultLayoutProps mDebugLayoutProps;

  private boolean mIsClone = false;
  private boolean mFrozen;

  protected long mPrivateFlags;

  protected InputOnlyInternalNode(ComponentContext componentContext) {
    mContext = componentContext.getAndroidContext();
    mDebugComponents = new HashSet<>();
    if (componentContext.useStatelessComponent()) {
      mScopedComponentInfos = new ArrayList<>(2);
    }
  }

  @Override
  public void addChildAt(InternalNode child, int index) {
    mChildren.add(index, child);
  }

  @Override
  public void addComponentNeedingPreviousRenderData(
      final String globalKey,
      final Component component,
      final @Nullable ScopedComponentInfo scopedComponentInfo) {
    if (mComponentsNeedingPreviousRenderData == null) {
      mComponentsNeedingPreviousRenderData = new HashMap<>(1);
    }
    mComponentsNeedingPreviousRenderData.put(globalKey, component);

    if (scopedComponentInfo != null && mScopedComponentInfosNeedingPreviousRenderData == null) {
      mScopedComponentInfosNeedingPreviousRenderData = new HashMap<>(1);
    }
    if (mScopedComponentInfosNeedingPreviousRenderData != null) {
      mScopedComponentInfosNeedingPreviousRenderData.put(globalKey, scopedComponentInfo);
    }
  }

  @Override
  public void addTransition(Transition transition) {
    if (mTransitions == null) {
      mTransitions = new ArrayList<>(1);
    }
    mTransitions.add(transition);
  }

  @Override
  public void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {
    if (mWorkingRangeRegistrations == null) {
      mWorkingRangeRegistrations = new ArrayList<>(registrations.size());
    }
    mWorkingRangeRegistrations.addAll(registrations);
  }

  @Override
  public void addAttachable(Attachable attachable) {
    if (mAttachables == null) {
      mAttachables = new ArrayList<>(4);
    }
    mAttachables.add(attachable);
  }

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    mAlignContent = alignContent;
    return this;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    mAlignItems = alignItems;
    return this;
  }

  @Override
  public void appendComponent(
      Component component, String key, @Nullable ScopedComponentInfo scopedComponentInfo) {
    mComponents.add(component);
    mComponentGlobalKeys.add(key);
    if (mScopedComponentInfos != null) {
      mScopedComponentInfos.add(scopedComponentInfo);
    }
  }

  @Override
  public void appendUnresolvedComponent(Component component) {
    if (mUnresolvedComponents == null) {
      mUnresolvedComponents = new ArrayList<>();
    }

    mUnresolvedComponents.add(component);
  }

  @Override
  public InternalNode background(@Nullable Drawable background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    return this;
  }

  @Override
  public InternalNode backgroundColor(@ColorInt int backgroundColor) {
    return background(ComparableColorDrawable.create(backgroundColor));
  }

  @Override
  public InternalNode backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background(null);
    }

    return background(ContextCompat.getDrawable(mContext, resId));
  }

  @Override
  public InternalNode border(Border border) {
    border(border.mEdgeWidths, border.mEdgeColors, border.mRadius, border.mPathEffect);
    return this;
  }

  @Override
  public void border(int[] widths, int[] colors, float[] radii, @Nullable PathEffect effect) {
    mPrivateFlags |= PFLAG_BORDER_IS_SET;
    System.arraycopy(widths, 0, mBorderEdgeWidths, 0, mBorderEdgeWidths.length);
    System.arraycopy(colors, 0, mBorderColors, 0, mBorderColors.length);
    System.arraycopy(radii, 0, mBorderRadius, 0, mBorderRadius.length);
    mBorderPathEffect = effect;
  }

  protected void applyDiffNode(
      final LayoutStateContext current,
      final LithoLayoutResult result,
      final @Nullable LithoLayoutResult parent) {

    final LayoutState state = current.getLayoutState();
    if (state == null) { // Cannot apply diff nodes without a LayoutState
      return;
    }

    final @Nullable DiffNode diff;

    if (parent == null) { // If root, then get diff node root from the current layout state
      if (isLayoutSpecWithSizeSpec(getHeadComponent()) && current.hasNestedTreeDiffNodeSet()) {
        diff = current.consumeNestedTreeDiffNode();
      } else {
        diff = current.getCurrentDiffTree();
      }
    } else if (parent.getDiffNode() != null) { // Otherwise get it from the parent
      final int index = parent.getInternalNode().getChildIndex(this);
      if (index != -1 && index < parent.getDiffNode().getChildCount()) {
        diff = parent.getDiffNode().getChildAt(index);
      } else {
        diff = null;
      }
    } else {
      diff = null;
    }

    if (diff == null) { // Return if no diff node to apply.
      return;
    }

    final Component component = getTailComponent();

    if (!hostIsCompatible(this, diff) && !(parent != null && isLayoutSpecWithSizeSpec(component))) {
      return;
    }

    result.setDiffNode(diff);

    if (!Layout.shouldComponentUpdate(this, diff)) {
      if (component != null) {
        final @Nullable ScopedComponentInfo scopedComponentInfo = getTailScopedComponentInfo();
        final @Nullable ScopedComponentInfo diffNodeScopedComponentInfo =
            diff.getScopedComponentInfo();

        component.copyInterStageImpl(
            scopedComponentInfo != null
                ? scopedComponentInfo.getInterStagePropsContainer()
                : component.getInterStagePropsContainer(),
            diffNodeScopedComponentInfo != null
                ? diffNodeScopedComponentInfo.getInterStagePropsContainer()
                : Preconditions.checkNotNull(diff.getComponent()).getInterStagePropsContainer());

        component.copyPrepareInterStageImpl(
            scopedComponentInfo != null
                ? scopedComponentInfo.getPrepareInterStagePropsContainer()
                : component.getPrepareInterStagePropsContainer(),
            diffNodeScopedComponentInfo != null
                ? diffNodeScopedComponentInfo.getPrepareInterStagePropsContainer()
                : Preconditions.checkNotNull(diff.getComponent())
                    .getPrepareInterStagePropsContainer());
      }

      result.setCachedMeasuresValid(true);
    }
  }

  protected void freeze(@Nullable InternalNode parent) {
    if (parent == null) {
      mFrozen = true;
      return;
    }
    // If parents important for A11Y is YES_HIDE_DESCENDANTS then
    // child's important for A11Y needs to be NO_HIDE_DESCENDANTS
    if (parent.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
      importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    final @NodeInfo.EnabledState int parentEnabledState;
    if (parent.getNodeInfo() != null) {
      parentEnabledState = parent.getNodeInfo().getEnabledState();
    } else {
      parentEnabledState = ENABLED_UNSET;
    }

    // If the parent of this node is disabled, this node has to be disabled too.
    if (parentEnabledState == ENABLED_SET_FALSE) {
      getOrCreateNodeInfo().setEnabled(false);
    }

    // Sets mFrozen as true to avoid anymore mutation.
    mFrozen = true;
  }

  private static void freezeRecursive(
      InputOnlyInternalNode node, @Nullable InputOnlyInternalNode parent) {
    if (node.mFrozen) {
      return;
    }

    node.freeze(parent);
    for (int i = 0; i < node.getChildCount(); i++) {
      freezeRecursive((InputOnlyInternalNode) node.getChildAt(i), node);
    }
  }

  /**
   * Builds the YogaNode tree from this tree of InternalNodes. At the same time, builds the
   * LayoutResult tree and sets it in the data of the corresponding YogaNodes.
   */
  @SuppressLint("LongLogTag")
  private static <Writer extends YogaLayoutProps> YogaNode buildYogaTree(
      LithoRenderContext renderContext,
      InputOnlyInternalNode<Writer> currentInternalNode,
      @Nullable LithoLayoutResult previousLayoutResult,
      @Nullable YogaNode parentNode) {
    final boolean isCloned =
        isCloned(renderContext.mLayoutStateContext, currentInternalNode, previousLayoutResult);
    final YogaNode node;
    if (isCloned) {
      node = previousLayoutResult.getYogaNode().cloneWithoutChildren();

      // TODO (T100055526): Investigate how the measure function can be set at this point.
      if (currentInternalNode.getChildCount() != 0 && node.isMeasureDefined()) {
        node.setMeasureFunction(null);
      }

    } else {
      node = NodeConfig.createYogaNode();

      // Create a LayoutProps object to write to.
      final Writer writer = (Writer) currentInternalNode.createYogaNodeWriter(node);

      // Transfer the layout props to YogaNode
      currentInternalNode.writeToYogaNode(writer, node);
    }

    final @Nullable LithoLayoutResult parentLayoutResult =
        parentNode != null ? (LithoLayoutResult) parentNode.getData() : null;
    final LithoLayoutResult layoutResult =
        currentInternalNode.createLayoutResult(
            renderContext.mLayoutStateContext, node, parentLayoutResult);
    currentInternalNode.applyDiffNode(
        renderContext.mLayoutStateContext, layoutResult, parentLayoutResult);
    node.setData(layoutResult);

    for (int i = 0; i < currentInternalNode.getChildCount(); i++) {
      final LithoLayoutResult previousChildLayoutResult =
          isCloned && i < previousLayoutResult.getChildCount()
              ? previousLayoutResult.getChildAt(i)
              : null;
      final YogaNode childNode =
          buildYogaTree(
              renderContext,
              (InputOnlyInternalNode) currentInternalNode.getChildAt(i),
              previousChildLayoutResult,
              node);
      node.addChildAt(childNode, i);
      layoutResult.addChild((LithoLayoutResult) childNode.getData());
    }

    return node;
  }

  @Override
  public LithoLayoutResult calculateLayout(
      final RenderState.LayoutContext<LithoRenderContext> c,
      final int widthSpec,
      final int heightSpec) {

    if (c.getRenderContext().mLayoutStateContext.getLayoutState() == null) {
      throw new IllegalStateException("Cannot calculate a layout without a layout state.");
    }

    final boolean isTracing = ComponentsSystrace.isTracing();

    applyOverridesRecursive(c.getRenderContext().mLayoutStateContext, this);

    if (isTracing) {
      ComponentsSystrace.beginSection("freeze:" + getHeadComponent().getSimpleName());
    }

    freezeRecursive(this, null);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    // Unlike DefaultInternalNode which creates the YogaNode tree at the same time as the
    // InternalNode tree, InputOnlyInternalNode creates the YogaNode tree during layout calculation.

    if (isTracing) {
      ComponentsSystrace.beginSection("buildYogaTree:" + getHeadComponent().getSimpleName());
    }

    final YogaNode root =
        buildYogaTree(c.getRenderContext(), this, c.getRenderContext().mCurrentLayoutRoot, null);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    if (isLayoutDirectionInherit() && isLayoutDirectionRTL(mContext)) {
      root.setDirection(YogaDirection.RTL);
    }
    if (YogaConstants.isUndefined(root.getWidth().value)) {
      Layout.setStyleWidthFromSpec(root, widthSpec);
    }
    if (YogaConstants.isUndefined(root.getHeight().value)) {
      Layout.setStyleHeightFromSpec(root, heightSpec);
    }

    final float width =
        SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(widthSpec);
    final float height =
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED
            ? YogaConstants.UNDEFINED
            : SizeSpec.getSize(heightSpec);

    if (isTracing) {
      ComponentsSystrace.beginSection("yogaCalculateLayout:" + getHeadComponent().getSimpleName());
    }

    root.calculateLayout(width, height);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return (LithoLayoutResult) root.getData();
  }

  @Override
  public InternalNode child(
      LayoutStateContext layoutContext, ComponentContext c, @Nullable Component child) {
    if (child != null) {
      return child(Layout.create(layoutContext, c, child));
    }

    return this;
  }

  @Override
  public InternalNode child(@Nullable InternalNode child) {
    if (child instanceof InputOnlyInternalNode) {
      addChildAt(child, mChildren.size());
    }

    return this;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
    return this;
  }

  @Override
  public InternalNode duplicateChildrenStates(boolean duplicateChildrenStates) {
    mPrivateFlags |= PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET;
    mDuplicateChildrenStates = duplicateChildrenStates;
    return this;
  }

  @Override
  public InternalNode flexDirection(YogaFlexDirection direction) {
    mFlexDirection = direction;
    return this;
  }

  @Override
  public InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = addVisibilityHandler(mFocusedHandler, focusedHandler);
    return this;
  }

  @Override
  public InternalNode foreground(@Nullable Drawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
    return this;
  }

  @Override
  public InternalNode foregroundColor(@ColorInt int foregroundColor) {
    return foreground(ComparableColorDrawable.create(foregroundColor));
  }

  @Override
  public InternalNode foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(ContextCompat.getDrawable(mContext, resId));
  }

  @Override
  public InternalNode layerType(final @LayerType int type, @Nullable final Paint paint) {
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      mLayerType = type;
      mLayerPaint = paint;
    }
    return this;
  }

  @Override
  public int getLayerType() {
    return mLayerType;
  }

  @Override
  public @Nullable Paint getLayerPaint() {
    return mLayerPaint;
  }

  @Override
  public InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = addVisibilityHandler(mFullImpressionHandler, fullImpressionHandler);
    return this;
  }

  @Override
  public int[] getBorderColors() {
    return mBorderColors;
  }

  @Override
  public @Nullable PathEffect getBorderPathEffect() {
    return mBorderPathEffect;
  }

  @Override
  public float[] getBorderRadius() {
    return mBorderRadius;
  }

  @Override
  public InternalNode getChildAt(int index) {
    return mChildren.get(index);
  }

  @Override
  public int getChildCount() {
    return mChildren.size();
  }

  @Override
  public int getChildIndex(InternalNode child) {
    for (int i = 0, count = mChildren.size(); i < count; i++) {
      if (mChildren.get(i) == child) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the list of components contributing to this InternalNode. We have no need for this in
   * production but it is useful information to have while debugging. Therefor this list will only
   * contain the root component if running in production mode.
   */
  @Override
  public List<Component> getComponents() {
    return mComponents;
  }

  /**
   * Return the list of keys of components contributing to this InternalNode. We have no need for
   * this in production but it is useful information to have while debugging. Therefor this list
   * will only contain the root component if running in production mode.
   */
  @Override
  public List<String> getComponentKeys() {
    return mComponentGlobalKeys;
  }

  @Override
  @Nullable
  public List<ScopedComponentInfo> getScopedComponentInfos() {
    return mScopedComponentInfos;
  }

  @Override
  public @Nullable List<Component> getUnresolvedComponents() {
    return mUnresolvedComponents;
  }

  @Override
  public @Nullable Map<String, Component> getComponentsNeedingPreviousRenderData() {
    return mComponentsNeedingPreviousRenderData;
  }

  @Override
  public @Nullable Map<String, ScopedComponentInfo>
      getScopedComponentInfosNeedingPreviousRenderData() {
    return mScopedComponentInfosNeedingPreviousRenderData;
  }

  @Override
  public Context getAndroidContext() {
    return mContext;
  }

  @Override
  public @Nullable EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return mFocusedHandler;
  }

  @Override
  public @Nullable Drawable getForeground() {
    return mForeground;
  }

  @Override
  public @Nullable EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return mFullImpressionHandler;
  }

  @Override
  public @Nullable Component getHeadComponent() {
    return mComponents.isEmpty() ? null : mComponents.get(mComponents.size() - 1);
  }

  @Override
  public @Nullable String getHeadComponentKey() {
    return mComponentGlobalKeys.get(mComponentGlobalKeys.size() - 1);
  }

  @Override
  public @Nullable ComponentContext getHeadComponentContext() {
    return mScopedComponentInfos != null
        ? mScopedComponentInfos.get(mScopedComponentInfos.size() - 1).getContext()
        : mComponents.get(mComponents.size() - 1).getScopedContext();
  }

  @Override
  public int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  @Override
  public @Nullable EventHandler<InvisibleEvent> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  @Override
  public @Nullable NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo) {
    mNodeInfo = nodeInfo;
  }

  @Override
  public NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = new NodeInfo();
    }

    return mNodeInfo;
  }

  @Override
  public @Nullable Component getTailComponent() {
    return mComponents.isEmpty() ? null : mComponents.get(0);
  }

  @Override
  public @Nullable String getTailComponentKey() {
    return mComponentGlobalKeys.get(0);
  }

  @Override
  public @Nullable ComponentContext getTailComponentContext() {
    return mScopedComponentInfos != null
        ? mScopedComponentInfos.get(0).getContext()
        : mComponents.get(0).getScopedContext();
  }

  @Override
  public @Nullable ScopedComponentInfo getTailScopedComponentInfo() {
    return mScopedComponentInfos != null ? mScopedComponentInfos.get(0) : null;
  }

  @Override
  public @Nullable ComponentContext getComponentContextAt(int index) {
    return mScopedComponentInfos != null
        ? mScopedComponentInfos.get(index).getContext()
        : mComponents.get(index).getScopedContext();
  }

  @Nullable
  @Override
  public List<Attachable> getAttachables() {
    return mAttachables;
  }

  @Override
  public @Nullable StateListAnimator getStateListAnimator() {
    return mStateListAnimator;
  }

  @Override
  public @DrawableRes int getStateListAnimatorRes() {
    return mStateListAnimatorRes;
  }

  @Override
  public void setNestedPadding(@Nullable Edges padding, @Nullable boolean[] isPercentage) {
    mNestedPaddingEdges = padding;
    mNestedIsPaddingPercent = isPercentage;
  }

  @Override
  public LayoutProps getDebugLayoutEditor() {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      mDebugLayoutProps = new CommonProps.DefaultLayoutProps();
      return mDebugLayoutProps;
    }
    return null;
  }

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  @Override
  public @Nullable String getTestKey() {
    return mTestKey;
  }

  @Override
  public @Nullable Edges getTouchExpansion() {
    return mTouchExpansion;
  }

  @Override
  public @Nullable String getTransitionKey() {
    return mTransitionKey;
  }

  @Override
  public @Nullable String getTransitionOwnerKey() {
    return mTransitionOwnerKey;
  }

  @Override
  public @Nullable Transition.TransitionKeyType getTransitionKeyType() {
    return mTransitionKeyType;
  }

  @Override
  public @Nullable ArrayList<Transition> getTransitions() {
    return mTransitions;
  }

  @Override
  public String getTransitionGlobalKey() {
    return getTailComponentKey();
  }

  @Override
  public @Nullable EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  @Override
  public @Nullable EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler() {
    return mVisibilityChangedHandler;
  }

  @Override
  public @Nullable EventHandler<VisibleEvent> getVisibleHandler() {
    return mVisibleHandler;
  }

  @Override
  public float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  @Override
  public float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  @Override
  public @Nullable ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations() {
    return mWorkingRangeRegistrations;
  }

  @Override
  public boolean hasBorderColor() {
    for (int color : mBorderColors) {
      if (color != Color.TRANSPARENT) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean hasStateListAnimatorResSet() {
    return (mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0;
  }

  @Override
  public boolean hasTouchExpansion() {
    return ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L);
  }

  @Override
  public boolean hasTransitionKey() {
    return !TextUtils.isEmpty(mTransitionKey);
  }

  @Override
  public boolean hasVisibilityHandlers() {
    return mVisibleHandler != null
        || mFocusedHandler != null
        || mUnfocusedHandler != null
        || mFullImpressionHandler != null
        || mInvisibleHandler != null
        || mVisibilityChangedHandler != null;
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  @Override
  public InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = addVisibilityHandler(mInvisibleHandler, invisibleHandler);
    return this;
  }

  @Override
  public boolean isDuplicateParentStateEnabled() {
    return mDuplicateParentState;
  }

  @Override
  public boolean isDuplicateChildrenStatesEnabled() {
    return mDuplicateChildrenStates;
  }

  @Override
  public boolean isForceViewWrapping() {
    return mForceViewWrapping;
  }

  @Override
  public boolean isImportantForAccessibilityIsSet() {
    return (mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) == 0L
        || mImportantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  }

  @Override
  public boolean isLayoutDirectionInherit() {
    return mLayoutDirection == null || mLayoutDirection == YogaDirection.INHERIT;
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    mJustifyContent = justifyContent;
    return this;
  }

  @Override
  public void layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mLayoutDirection = direction;
  }

  @Override
  public void registerDebugComponent(DebugComponent debugComponent) {
    if (mDebugComponents == null) {
      mDebugComponents = new HashSet<>();
    }
    mDebugComponents.add(debugComponent);
  }

  @Deprecated
  @Override
  public boolean implementsLayoutDiffing() {
    return false;
  }

  @Override
  public InternalNode removeChildAt(int index) {
    return mChildren.remove(index);
  }

  @Override
  public void setMeasureFunction(YogaMeasureFunction measureFunction) {
    mYogaMeasureFunction = measureFunction;
  }

  @Override
  public InternalNode stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_SET;
    mStateListAnimator = stateListAnimator;
    wrapInView();
    return this;
  }

  @Override
  public InternalNode stateListAnimatorRes(@DrawableRes int resId) {
    mPrivateFlags |= PFLAG_STATE_LIST_ANIMATOR_RES_SET;
    mStateListAnimatorRes = resId;
    wrapInView();
    return this;
  }

  @Override
  public InternalNode testKey(@Nullable String testKey) {
    mTestKey = testKey;
    return this;
  }

  @Override
  public InternalNode touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    if (mTouchExpansion == null) {
      mTouchExpansion = new Edges();
    }

    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    mTouchExpansion.set(edge, touchExpansion);

    return this;
  }

  @Override
  public InternalNode transitionKey(@Nullable String key, @Nullable String ownerKey) {
    if (SDK_INT >= ICE_CREAM_SANDWICH && !TextUtils.isEmpty(key)) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
      mTransitionOwnerKey = ownerKey;
    }

    return this;
  }

  @Override
  public InternalNode transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    mPrivateFlags |= PFLAG_TRANSITION_KEY_TYPE_IS_SET;
    mTransitionKeyType = type;
    return this;
  }

  @Override
  public InternalNode unfocusedHandler(
      @Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = addVisibilityHandler(mUnfocusedHandler, unfocusedHandler);
    return this;
  }

  @Override
  public InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET;
    mVisibilityChangedHandler =
        addVisibilityHandler(mVisibilityChangedHandler, visibilityChangedHandler);
    return this;
  }

  @Override
  public InternalNode visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = addVisibilityHandler(mVisibleHandler, visibleHandler);
    return this;
  }

  @Override
  public InternalNode visibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
    return this;
  }

  @Override
  public InternalNode visibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
    return this;
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    mYogaWrap = wrap;
    return this;
  }

  @Override
  public InternalNode wrapInView() {
    mForceViewWrapping = true;
    return this;
  }

  @Override
  public @Nullable Drawable getBackground() {
    return mBackground;
  }

  @Override
  public void applyAttributes(Context c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    final TypedArray a =
        c.obtainStyledAttributes(
            null, com.facebook.litho.R.styleable.ComponentLayout, defStyleAttr, defStyleRes);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_importantForAccessibility
          && SDK_INT >= JELLY_BEAN) {
        importantForAccessibility(a.getInt(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_duplicateParentState) {
        duplicateParentState(a.getBoolean(attr, false));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_background) {
        if (TypedArrayUtils.isColorAttribute(
            a, com.facebook.litho.R.styleable.ComponentLayout_android_background)) {
          backgroundColor(a.getColor(attr, 0));
        } else {
          backgroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_foreground) {
        if (TypedArrayUtils.isColorAttribute(
            a, com.facebook.litho.R.styleable.ComponentLayout_android_foreground)) {
          foregroundColor(a.getColor(attr, 0));
        } else {
          foregroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_contentDescription) {
        getOrCreateNodeInfo().setContentDescription(a.getString(attr));
      }
    }

    a.recycle();
  }

  protected static void applyLayoutStyleAttributes(YogaLayoutProps props, TypedArray a) {
    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_width) {
        int width = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (width >= 0) {
          props.widthPx(width);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_height) {
        int height = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (height >= 0) {
          props.heightPx(height);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_minHeight) {
        props.minHeightPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_minWidth) {
        props.minWidthPx(a.getDimensionPixelSize(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingLeft) {
        props.paddingPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingTop) {
        props.paddingPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingRight) {
        props.paddingPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingBottom) {
        props.paddingPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingStart
          && SUPPORTS_RTL) {
        props.paddingPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_paddingEnd
          && SUPPORTS_RTL) {
        props.paddingPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_padding) {
        props.paddingPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginLeft) {
        props.marginPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginTop) {
        props.marginPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginRight) {
        props.marginPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr
          == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginBottom) {
        props.marginPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginStart
          && SUPPORTS_RTL) {
        props.marginPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_marginEnd
          && SUPPORTS_RTL) {
        props.marginPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_android_layout_margin) {
        props.marginPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_direction) {
        props.flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_wrap) {
        props.wrap(YogaWrap.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_justifyContent) {
        props.justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_alignItems) {
        props.alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_alignSelf) {
        props.alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_positionType) {
        props.positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_layoutDirection) {
        final int layoutDirection = a.getInteger(attr, -1);
        props.layoutDirection(YogaDirection.fromInt(layoutDirection));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex) {
        final float flex = a.getFloat(attr, -1);
        if (flex >= 0f) {
          props.flex(flex);
        }
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_left) {
        props.positionPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_top) {
        props.positionPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_right) {
        props.positionPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == com.facebook.litho.R.styleable.ComponentLayout_flex_bottom) {
        props.positionPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      }
    }
  }

  @Override
  public String getSimpleName() {
    return mComponents.isEmpty() ? "<null>" : mComponents.get(0).getSimpleName();
  }

  @Override
  public boolean isClone() {
    return mIsClone;
  }

  protected InputOnlyInternalNode clone() {
    final InputOnlyInternalNode node;
    try {
      node = (InputOnlyInternalNode) super.clone();
      node.mIsClone = true;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }

    return node;
  }

  static @Nullable Rect getPaddingFromDrawable(Drawable drawable) {
    final Rect rect = new Rect();
    return getDrawablePadding(drawable, rect) ? rect : null;
  }

  /**
   * Release properties which are not longer required for the current layout pass or release
   * properties which should be reset during reconciliation.
   */
  protected void clean() {
    // 1. Release or clone props.
    mComponents = new ArrayList<>(8);
    mChildren = new ArrayList<>(4);
    mComponentGlobalKeys = new ArrayList<>(8);
    mDebugComponents = null;
    mFrozen = false;
  }

  void updateWith(
      final ComponentContext c,
      final List<Component> components,
      final List<String> componentKeys,
      final @Nullable DiffNode diffNode) {

    // 1. Set new ComponentContext, YogaNode, and components.
    mComponents = components;
    mComponentGlobalKeys = componentKeys;

    // 2. Update props.
    mComponentsNeedingPreviousRenderData = null;
    for (int i = 0, size = components.size(); i < size; i++) {
      final Component component = components.get(i);
      final String key = componentKeys.get(i);
      if (component.needsPreviousRenderData()) {
        // This method will not be called in stateless mode so it's safe to pass null
        // scopedComponentInfo
        addComponentNeedingPreviousRenderData(key, component, null);
      }
    }

    ArrayList<WorkingRangeContainer.Registration> ranges = mWorkingRangeRegistrations;
    mWorkingRangeRegistrations = null;
    if (ranges != null && !ranges.isEmpty()) {
      mWorkingRangeRegistrations = new ArrayList<>(ranges.size());
      for (WorkingRangeContainer.Registration old : ranges) {
        final String key = old.mKey;
        final int index = componentKeys.indexOf(key);
        if (index >= 0) {
          final Component component = components.get(index);
          // This method will not be called in stateless mode so it's safe to pass null
          // scopedComponentInfo
          mWorkingRangeRegistrations.add(
              new WorkingRangeContainer.Registration(
                  old.mName, old.mWorkingRange, component, key, null));
        }
      }
    }
  }

  /**
   * Convenience method to get an updated shallow copy of all the components of this InternalNode.
   * Optionally replace the head component with a new component. The head component is the root
   * component in the Component hierarchy representing this InternalNode.
   *
   * @param head The root component of this InternalNode's Component hierarchy.
   * @return List of updated shallow copied components of this InternalNode.
   */
  private Pair<List<Component>, List<String>> getUpdatedComponents(
      final LayoutStateContext layoutStateContext, Component head, @Nullable String headKey) {
    int size = mComponents.size();
    List<Component> updated = new ArrayList<>(size);
    List<String> updatedKeys = new ArrayList<String>(size);

    // 1. Add the updated head component to the list.
    updated.add(head);
    if (updatedKeys != null) {
      updatedKeys.add(headKey);
    }

    // 2. Set parent context for descendants.
    ComponentContext parentContext = head.getScopedContext();

    // 3. Shallow copy and update all components, except the head component.
    for (int i = size - 2; i >= 0; i--) {
      final String key = mComponentGlobalKeys.get(i);
      final Component component =
          mComponents.get(i).makeUpdatedShallowCopy(layoutStateContext, parentContext, key);
      updated.add(component);
      updatedKeys.add(key);

      parentContext = component.getScopedContext(); // set parent context for descendant
    }

    // 4. Reverse the list so that the root component is at index 0.
    Collections.reverse(updated);
    Collections.reverse(updatedKeys);

    return new Pair<>(updated, updatedKeys);
  }

  private @Nullable static <T> EventHandler<T> addVisibilityHandler(
      @Nullable EventHandler<T> currentHandler, @Nullable EventHandler<T> newHandler) {
    if (currentHandler == null) {
      return newHandler;
    }
    if (newHandler == null) {
      return currentHandler;
    }
    return new DelegatingEventHandler<>(currentHandler, newHandler);
  }

  /**
   * This is a wrapper on top of built in {@link Drawable#getPadding(Rect)} which overrides default
   * return value. The reason why we need this - is because on pre-L devices LayerDrawable always
   * returns "true" even if drawable doesn't have padding (see https://goo.gl/gExcMQ). Since we
   * heavily rely on correctness of this information, we need to check padding manually
   */
  private static boolean getDrawablePadding(Drawable drawable, Rect outRect) {
    drawable.getPadding(outRect);
    return outRect.bottom != 0 || outRect.top != 0 || outRect.left != 0 || outRect.right != 0;
  }

  @Override
  public InternalNode reconcile(
      final LayoutStateContext layoutStateContext,
      final ComponentContext c,
      final Component next,
      final @Nullable ScopedComponentInfo nextScopedComponentInfo,
      final @Nullable String nextKey) {
    final StateHandler stateHandler = layoutStateContext.getStateHandler();
    final Set<String> keys;
    if (stateHandler == null) {
      keys = Collections.emptySet();
    } else {
      keys = stateHandler.getKeysForPendingUpdates();
    }

    return reconcile(layoutStateContext, c, this, next, nextScopedComponentInfo, nextKey, keys);
  }

  public void setNestedTreeHolder(@Nullable InputOnlyNestedTreeHolder holder) {
    mNestedTreeHolder = holder;
  }

  protected YogaLayoutProps createYogaNodeWriter(YogaNode node) {
    return new YogaLayoutProps(node);
  }

  void writeToYogaNode(final Writer target, final YogaNode node) {

    // Apply the extra layout props
    if (mLayoutDirection != null) {
      node.setDirection(mLayoutDirection);
    }

    if (mFlexDirection != null) {
      node.setFlexDirection(mFlexDirection);
    }
    if (mJustifyContent != null) {
      node.setJustifyContent(mJustifyContent);
    }
    if (mAlignContent != null) {
      node.setAlignContent(mAlignContent);
    }
    if (mAlignItems != null) {
      node.setAlignItems(mAlignItems);
    }
    if (mYogaWrap != null) {
      node.setWrap(mYogaWrap);
    }
    if (mYogaMeasureFunction != null) {
      node.setMeasureFunction(mYogaMeasureFunction);
    }

    // Apply the layout props from the components to the YogaNode
    for (Component component : mComponents) {
      // If a NestedTreeHolder is set then transfer its resolved props into this InternalNode.
      if (mNestedTreeHolder != null && isLayoutSpecWithSizeSpec(component)) {
        mNestedTreeHolder.transferInto(this);
        if (mBackground != null) {
          setPaddingFromDrawable(target, mBackground);
        }
      } else {
        final CommonProps props = component.getCommonProps();
        if (props != null) {
          final int styleAttr = props.getDefStyleAttr();
          final int styleRes = props.getDefStyleRes();
          if (styleAttr != 0 || styleRes != 0) {
            final Context context =
                Preconditions.checkNotNull(getTailComponentContext()).getAndroidContext();
            final TypedArray a =
                context.obtainStyledAttributes(
                    null, com.facebook.litho.R.styleable.ComponentLayout, styleAttr, styleRes);
            applyLayoutStyleAttributes(target, a);
            a.recycle();
          }

          // Set the padding from the background
          final Drawable background = props.getBackground();
          if (background != null) {
            setPaddingFromDrawable(target, background);
          }

          // Copy the layout props into this InternalNode.
          props.copyLayoutProps(target);
        }
      }
    }

    // Apply the border widths
    if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
      for (int i = 0, length = mBorderEdgeWidths.length; i < length; ++i) {
        target.setBorderWidth(Border.edgeFromIndex(i), mBorderEdgeWidths[i]);
      }
    }

    // Maybe apply the padding if parent is a Nested Tree Holder
    if (mNestedPaddingEdges != null) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        float value = mNestedPaddingEdges.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          final YogaEdge edge = YogaEdge.fromInt(i);
          if (mNestedIsPaddingPercent != null && mNestedIsPaddingPercent[edge.intValue()]) {
            target.paddingPercent(edge, value);
          } else {
            target.paddingPx(edge, (int) value);
          }
        }
      }
    }

    if (mDebugLayoutProps != null) {
      mDebugLayoutProps.copyInto(target);
    }

    mIsPaddingSet = target.isPaddingSet;
  }

  DefaultLayoutResult createLayoutResult(
      final LayoutStateContext context,
      final YogaNode node,
      final @Nullable LithoLayoutResult parent) {
    return new DefaultLayoutResult(
        context, Preconditions.checkNotNull(getTailComponentContext()), this, node, parent);
  }

  protected static void setPaddingFromDrawable(YogaLayoutProps target, Drawable drawable) {
    final Rect rect = getPaddingFromDrawable(drawable);
    if (rect != null) {
      target.paddingPx(LEFT, rect.left);
      target.paddingPx(TOP, rect.top);
      target.paddingPx(RIGHT, rect.right);
      target.paddingPx(BOTTOM, rect.bottom);
    }
  }

  boolean isPaddingSet() {
    return mIsPaddingSet;
  }

  /**
   * Internal method to <b>try</b> and reconcile the {@param current} InternalNode with a new {@link
   * ComponentContext} and an updated head {@link Component}.
   *
   * @param layoutStateContext
   * @param parentContext The ComponentContext.
   * @param current The current InternalNode which should be updated.
   * @param next The updated component to be used to reconcile this InternalNode.
   * @param keys The keys of mutated components.
   * @return A new updated InternalNode.
   */
  private static @Nullable InternalNode reconcile(
      final LayoutStateContext layoutStateContext,
      final ComponentContext parentContext,
      final InputOnlyInternalNode current,
      final Component next,
      final @Nullable ScopedComponentInfo nextScopedComponentInfo,
      @Nullable final String nextKey,
      final Set<String> keys) {
    final boolean isInternalNodeReuseEnabled = parentContext.isInternalNodeReuseEnabled();
    final int mode =
        getReconciliationMode(
            Preconditions.checkNotNull(
                nextScopedComponentInfo != null
                    ? nextScopedComponentInfo.getContext()
                    : next.getScopedContext()),
            current,
            keys,
            isInternalNodeReuseEnabled);
    final InternalNode layout;

    switch (mode) {
      case ReconciliationMode.REUSE:
        if (isInternalNodeReuseEnabled || layoutStateContext.useStatelessComponent()) {
          commitToLayoutStateRecursively(layoutStateContext, current);
        }
        layout = current;
        break;
      case ReconciliationMode.COPY:
        layout =
            reconcile(layoutStateContext, current, next, nextKey, keys, ReconciliationMode.COPY);
        break;
      case ReconciliationMode.RECONCILE:
        layout =
            reconcile(
                layoutStateContext, current, next, nextKey, keys, ReconciliationMode.RECONCILE);
        break;
      case ReconciliationMode.RECREATE:
        layout = Layout.create(layoutStateContext, parentContext, next, false, true, nextKey);
        break;
      default:
        throw new IllegalArgumentException(mode + " is not a valid ReconciliationMode");
    }

    return layout;
  }

  /**
   * Internal method to reconcile the {@param current} InternalNode with a new {@link
   * ComponentContext} and an updated head {@link Component} and a {@link ReconciliationMode}.
   *
   * @param current The current InternalNode which should be updated.
   * @param next The updated component to be used to reconcile this InternalNode.
   * @param keys The keys of mutated components.
   * @param mode {@link ReconciliationMode#RECONCILE} or {@link ReconciliationMode#COPY}.
   * @return A new updated InternalNode.
   */
  private static InternalNode reconcile(
      final LayoutStateContext layoutStateContext,
      final InputOnlyInternalNode current,
      final Component next,
      final @Nullable String nextKey,
      final Set<String> keys,
      final @ReconciliationMode int mode) {

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          (mode == ReconciliationMode.COPY ? "copy:" : "reconcile:") + next.getSimpleName());
    }

    // 2. Shallow copy this layout.
    final InputOnlyInternalNode<?> layout;

    if (layoutStateContext.isInternalNodeReuseEnabled()
        || layoutStateContext.useStatelessComponent()) {
      layout = current.clone();
      layout.mChildren = new ArrayList<>(current.getChildCount());
      layout.mDebugComponents = null;
      commitToLayoutState(layoutStateContext, current);
    } else {
      layout = getCleanUpdatedShallowCopy(layoutStateContext, current, next, nextKey);
    }

    ComponentContext parentContext = layout.getTailComponentContext();

    // 3. Iterate over children.
    int count = current.getChildCount();
    for (int i = 0; i < count; i++) {
      final InputOnlyInternalNode child = (InputOnlyInternalNode) current.getChildAt(i);

      // 3.1 Get the head component of the child layout.
      final List<Component> components = child.getComponents();
      final List<String> componentKeys = child.getComponentKeys();
      int index = Math.max(0, components.size() - 1);
      final Component component = components.get(index);
      final String key = componentKeys == null ? null : componentKeys.get(index);
      final ScopedComponentInfo scopedComponentInfo =
          child.mScopedComponentInfos != null
              ? (ScopedComponentInfo) child.mScopedComponentInfos.get(index)
              : null;

      // 3.2 Update the head component of the child layout.
      final Component updated;

      if (layoutStateContext.isInternalNodeReuseEnabled()) {
        updated = component;
      } else {
        updated = component.makeUpdatedShallowCopy(layoutStateContext, parentContext, key);
      }

      // 3.3 Reconcile child layout.
      final InternalNode copy;
      if (mode == ReconciliationMode.COPY) {
        copy = reconcile(layoutStateContext, child, updated, key, keys, ReconciliationMode.COPY);
      } else {
        copy =
            reconcile(
                layoutStateContext, parentContext, child, updated, scopedComponentInfo, key, keys);
      }

      // 3.3 Add the child to the cloned yoga node
      layout.child(copy);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  static void commitToLayoutStateRecursively(LayoutStateContext c, InternalNode node) {
    final int count = node.getChildCount();
    commitToLayoutState(c, node);
    for (int i = 0; i < count; i++) {
      commitToLayoutStateRecursively(c, node.getChildAt(i));
    }
  }

  static void commitToLayoutState(LayoutStateContext c, InternalNode node) {
    final @Nullable List<ScopedComponentInfo> scopedComponentInfos = node.getScopedComponentInfos();

    if (scopedComponentInfos != null) {
      for (ScopedComponentInfo info : scopedComponentInfos) {
        info.commitToLayoutState(c.getStateHandler());
      }
    }
  }

  /**
   * Convenience method to create a shallow copy of the InternalNode, set a new YogaNode, update all
   * components and ComponentContext, release all the unnecessary properties from the new
   * InternalNode.
   */
  private static InputOnlyInternalNode getCleanUpdatedShallowCopy(
      final LayoutStateContext layoutStateContext,
      final InputOnlyInternalNode current,
      final Component head,
      final @Nullable String headKey) {

    final boolean isTracing = ComponentsSystrace.isTracing();

    if (isTracing) {
      ComponentsSystrace.beginSection("clone:" + head.getSimpleName());
    }

    // 1. Shallow copy this layout.
    final InputOnlyInternalNode layout = current.clone();

    if (isTracing) {
      ComponentsSystrace.endSection();
      ComponentsSystrace.beginSection("clean:" + head.getSimpleName());
    }

    // 2. Reset and release properties
    layout.clean();

    if (isTracing) {
      ComponentsSystrace.endSection();
      ComponentsSystrace.beginSection("update:" + head.getSimpleName());
    }

    // 3. Get updated components
    Pair<List<Component>, List<String>> updated =
        current.getUpdatedComponents(layoutStateContext, head, headKey);

    // 4. Update the layout with the updated context, components, and YogaNode.
    final Component tailComponent = updated.first.get(0);
    layout.updateWith(
        Preconditions.checkNotNull(tailComponent.getScopedContext()),
        updated.first,
        updated.second,
        null);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return layout;
  }

  /**
   * Returns the a {@link ReconciliationMode} mode which directs the reconciling process to branch
   * to either recreate the entire subtree, copy the entire subtree or continue to recursively
   * reconcile the subtree.
   */
  @VisibleForTesting
  static @ReconciliationMode int getReconciliationMode(
      final ComponentContext c,
      final InternalNode current,
      final Set<String> keys,
      final boolean isInternalNodeReuseEnabled) {
    final List<Component> components = current.getComponents();
    final List<String> componentKeys = current.getComponentKeys();
    final Component root = current.getHeadComponent();

    // 1.0 check early exit conditions
    if (c == null || root == null || current instanceof NestedTreeHolder) {
      return ReconciliationMode.RECREATE;
    }

    // 1.1 Check if any component has mutations
    for (int i = 0, size = components.size(); i < size; i++) {
      final String key = componentKeys.get(i);
      if (keys.contains(key)) {
        return ReconciliationMode.RECREATE;
      }
    }

    // 2.0 Check if any descendants have mutations
    final String rootKey = current.getHeadComponentKey();
    for (String key : keys) {
      if (key.startsWith(rootKey)) {
        return ReconciliationMode.RECONCILE;
      }
    }

    return isInternalNodeReuseEnabled ? ReconciliationMode.REUSE : ReconciliationMode.COPY;
  }

  private static void applyOverridesRecursive(LayoutStateContext c, InternalNode node) {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      DebugComponent.applyOverrides(
          Preconditions.checkNotNull(node.getTailComponentContext()), node);
      for (int i = 0, count = node.getChildCount(); i < count; i++) {
        applyOverridesRecursive(c, node.getChildAt(i));
      }
    }
  }

  @Override
  public Copyable makeCopy() {
    return null;
  }

  private static boolean isCloned(
      final LayoutStateContext context,
      final InternalNode node,
      final @Nullable LithoLayoutResult current) {
    final ComponentTree tree = context.getComponentTree();
    if (current != null && tree != null && tree.isLayoutCachingEnabled()) {
      return current.getInternalNode() == node || node.isClone();
    } else {
      return false;
    }
  }
}
