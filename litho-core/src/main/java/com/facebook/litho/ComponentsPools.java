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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.MountItemsPool.MountContentCreator;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.concurrent.GuardedBy;

/**
 * Pools of recycled resources.
 *
 * <p>FUTURE: Consider customizing the pool implementation such that we can match buffer sizes.
 * Without this we will tend to expand all buffers to the largest size needed.
 */
public class ComponentsPools {

  private ComponentsPools() {}

  private static final Object sMountContentLock = new Object();

  @GuardedBy("sMountContentLock")
  private static final Map<Context, SparseArray<MountContentPool>> sMountContentPoolsByContext =
      new HashMap<>(4);

  // This Map is used as a set and the values are ignored.
  @GuardedBy("sMountContentLock")
  private static final WeakHashMap<Context, Boolean> sDestroyedRootContexts = new WeakHashMap<>();

  @GuardedBy("sMountContentLock")
  private static PoolsActivityCallback sActivityCallbacks;

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API doesn't exist), we
   * allow apps to explicitly invoke activity callbacks. If this is enabled we'll throw if we are
   * passed a context for which we have no record.
   */
  static boolean sIsManualCallbacks;

  public static Object acquireMountContent(
      Context context, Component component, @ComponentTree.RecyclingMode int recyclingMode) {
    final MountContentPool pool = getMountContentPool(context, component, recyclingMode);
    if (pool == null) {
      return component.createMountContent(context);
    }

    Object content = pool.acquire(context, component);
    if (recyclingMode == ComponentTree.RecyclingMode.NO_VIEW_REUSE) {
      // Throw acquired content if N0_VIEW_REUSE recycling mode!
      return component.createMountContent(context);
    }

    return content;
  }

  public static void release(
      Context context, Component component, Object mountContent, int recyclingMode) {
    final MountContentPool pool = getMountContentPool(context, component, recyclingMode);
    if (pool != null) {
      if (mountContent instanceof LoggingMountContent) {
        ((LoggingMountContent) mountContent).onMountContentRecycled();
      }
      pool.release(mountContent);
    }
  }

  /**
   * Pre-allocates mount content for this component type within the pool for this context unless the
   * pre-allocation limit has been hit in which case we do nothing.
   */
  public static void maybePreallocateContent(
      Context context, Component component, int recyclingMode) {
    // When Rendercore MountState is enabled, delegate these calls to MountItemsPool.
    // Needed since this API is called from product layer.
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.maybePreallocateContent(context, wrapComponentInRenderUnit(component));
      return;
    }

    final MountContentPool pool = getMountContentPool(context, component, recyclingMode);
    if (pool != null) {
      pool.maybePreallocateContent(context, component);
    }
  }

  /**
   * Can be called to fill up a mount content pool for the specified MountContent types. If a pool
   * doesn't exist for a Mount Content type, a default one will be created with the specified size.
   */
  public static void prefillMountContentPool(
      Context context,
      int poolSize,
      Class componentClass,
      MountContentCreator mountContentCreator) {
    if (poolSize == 0) {
      return;
    }

    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.prefillMountContentPool(
          context, poolSize, componentClass, mountContentCreator);
      return;
    }

    final MountContentPool pool =
        getDefaultMountContentPool(
            context, Component.getOrCreateId(componentClass), componentClass, poolSize);
    if (pool != null) {
      for (int i = 0; i < poolSize; i++) {
        pool.release(mountContentCreator.createMountContent(context));
      }
    }
  }

  private static @Nullable MountContentPool getMountContentPool(
      Context context, Component component, int recyclingMode) {
    if (component.poolSize() == 0 || !shouldCreateMountContentPool(recyclingMode)) {
      return null;
    }

    synchronized (sMountContentLock) {
      SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);
      if (poolsArray == null) {
        final Context rootContext = ContextUtils.getRootContext(context);
        if (sDestroyedRootContexts.containsKey(rootContext)) {
          return null;
        }

        ensureActivityCallbacks(context);
        poolsArray = new SparseArray<>();
        sMountContentPoolsByContext.put(context, poolsArray);
      }

      MountContentPool pool = poolsArray.get(component.getTypeId());
      if (pool == null) {
        pool = PoolBisectUtil.getPoolForComponent((Component) component);
        poolsArray.put(component.getTypeId(), pool);
      }

      return pool;
    }
  }

  private static @Nullable MountContentPool getDefaultMountContentPool(
      Context context, int typeId, Class componentClass, int poolSize) {

    synchronized (sMountContentLock) {
      SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);
      if (poolsArray == null) {
        final Context rootContext = ContextUtils.getRootContext(context);
        if (sDestroyedRootContexts.containsKey(rootContext)) {
          return null;
        }

        ensureActivityCallbacks(context);
        poolsArray = new SparseArray<>();
        sMountContentPoolsByContext.put(context, poolsArray);
      }

      MountContentPool pool = poolsArray.get(typeId);
      if (pool == null) {
        pool = new DefaultMountContentPool(componentClass.getSimpleName(), poolSize, true);
        poolsArray.put(typeId, pool);
      }

      return pool;
    }
  }

  private static boolean shouldCreateMountContentPool(int recyclingMode) {
    return recyclingMode == ComponentTree.RecyclingMode.DEFAULT
        || recyclingMode == ComponentTree.RecyclingMode.NO_VIEW_REUSE;
  }

  @GuardedBy("sMountContentLock")
  private static void ensureActivityCallbacks(Context context) {
    if (sActivityCallbacks == null && !sIsManualCallbacks) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        throw new RuntimeException(
            "Activity callbacks must be invoked manually below ICS (API level 14)");
      }
      sActivityCallbacks = new PoolsActivityCallback();
      ((Application) context.getApplicationContext())
          .registerActivityLifecycleCallbacks(sActivityCallbacks);
    }
  }

  /** Empty implementation of the {@link Application.ActivityLifecycleCallbacks} interface */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class PoolsActivityCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, @Nullable Bundle savedInstanceState) {
      ComponentsPools.onContextCreated(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityResumed(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityPaused(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityStopped(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      // Do nothing.
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      ComponentsPools.onContextDestroyed(activity);
    }
  }

  static void onContextCreated(Context context) {
    synchronized (sMountContentLock) {
      if (sMountContentPoolsByContext.containsKey(context)) {
        throw new IllegalStateException(
            "The MountContentPools has a reference to an activity that has just been created");
      }
    }
  }

  static void onContextDestroyed(Context context) {
    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.remove(context);

      // Clear any context wrappers holding a reference to this activity.
      final Iterator<Map.Entry<Context, SparseArray<MountContentPool>>> it =
          sMountContentPoolsByContext.entrySet().iterator();

      while (it.hasNext()) {
        final Context contextKey = it.next().getKey();
        if (isContextWrapper(contextKey, context)) {
          it.remove();
        }
      }

      sDestroyedRootContexts.put(ContextUtils.getRootContext(context), true);
    }
  }

  /** Call from tests to clear external references. */
  public static void clearMountContentPools() {
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.clear();
      return;
    }

    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.clear();
    }
  }

  /** Check whether contextWrapper is a wrapper of baseContext */
  private static boolean isContextWrapper(Context contextWrapper, Context baseContext) {
    Context currentContext = contextWrapper;
    while (currentContext instanceof ContextWrapper) {
      currentContext = ((ContextWrapper) currentContext).getBaseContext();

      if (currentContext == baseContext) {
        return true;
      }
    }

    return false;
  }

  static List<MountContentPool> getMountContentPools() {
    final ArrayList<MountContentPool> pools = new ArrayList<>();
    synchronized (sMountContentLock) {
      for (SparseArray<MountContentPool> contentPools :
          ComponentsPools.sMountContentPoolsByContext.values()) {
        for (int i = 0, count = contentPools.size(); i < count; i++) {
          pools.add(contentPools.valueAt(i));
        }
      }
    }
    return pools;
  }

  @VisibleForTesting
  @GuardedBy("sMountContentLock")
  static void clearActivityCallbacks() {
    sActivityCallbacks = null;
  }

  /**
   * A Mount Content which implements this interface can be notified of recycling events for
   * logging.
   */
  public interface LoggingMountContent {
    void onMountContentRecycled();
  }

  /**
   * RenderUnit wrapper for delegating maybePreallocateContent calls to RenderCore's Pooling system
   * when the configuration is enabled. Since this pooling mechanism only requires a component
   * within the RenderUnit, this is the only parameter passed here.
   */
  private static RenderUnit wrapComponentInRenderUnit(Component component) {
    return new LithoRenderUnit(
        0, new LayoutOutput(component, null, null, 0, 0, LayoutOutput.STATE_UNKNOWN), null);
  }
}
