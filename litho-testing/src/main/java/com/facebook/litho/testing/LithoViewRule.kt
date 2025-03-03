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

package com.facebook.litho.testing

import android.app.Activity
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentsPools
import com.facebook.litho.LithoView
import com.facebook.litho.TreeProps
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.MountItemsPool
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * This test utility allows clients to create a [TestLithoView] instance that allows to test
 * assertion on the view hierarchy rendered by a Litho components.
 * ```
 *  @RunWith(AndroidJUnit4::class)
 *  class LithoSampleTest {
 *
 *  @Rule @JvmField val lithoViewRule = LithoViewRule()
 *  @Test
 *  fun test() {
 *   val testLithoView =  lithoViewRule.render { TestComponent() }
 *
 *    // or you can use setRoot/measure/layout for more fine-grained control
 *    val testLithoView = lithoViewRule.createTestLithoView().attachToWindow().setRoot(TestComponent()).measure().layout()
 *    // Test your assertions on the TestLithoView instance.
 *    }
 * }
 * ```
 */
class LithoViewRule
@JvmOverloads
constructor(
    val componentsConfiguration: ComponentsConfiguration? = null,
    val themeResId: Int? = null
) : TestRule {
  lateinit var context: ComponentContext
  private val threadLooperController: ThreadLooperController = ThreadLooperController()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        try {
          if (themeResId != null) {
            val activity = Robolectric.buildActivity(Activity::class.java).create().get()
            activity.setTheme(themeResId)
            context = ComponentContext(activity)
          } else {
            context = ComponentContext(getApplicationContext<Context>())
          }
          context.setLayoutStateContextForTesting()
          threadLooperController.init()
          base.evaluate()
        } finally {
          threadLooperController.clean()
          ComponentsPools.clearMountContentPools()
          MountItemsPool.clear()
        }
      }
    }
  }

  fun useContext(c: ComponentContext): LithoViewRule {
    context = c
    return this
  }

  /** Sets a new [TreeProp] for the next layout pass. */
  fun setTreeProp(klass: Class<*>, instance: Any?): LithoViewRule {
    val props = context.treeProps ?: TreeProps()
    props.put(klass, instance)
    context.treeProps = props
    return this
  }

  /**
   * Creates new TestLithoView holder responsible for keeping an instance of LithoView, allowing to
   * find Views/Components or perform assertions on it. For simple Component rendering without
   * fine-grained control, use [render]
   */
  @JvmOverloads
  fun createTestLithoView(
      componentFunction: (ComponentScope.() -> Component)? = null
  ): TestLithoView {
    val testLithoView = TestLithoView(context, componentsConfiguration)
    componentFunction?.let {
      testLithoView.setRoot(with(ComponentScope(context)) { componentFunction() })
    }
    return testLithoView
  }

  /**
   * Creates new TestLithoView holder responsible for keeping an instance of LithoView, allowing to
   * find Views/Components or perform assertions on it. You can pass additional parameters, if null,
   * the default values will be provided. For simple Component rendering without fine-grained
   * control, use [render]
   */
  @JvmOverloads
  fun createTestLithoViewWith(
      lithoView: LithoView? = null,
      componentTree: ComponentTree? = null,
      widthPx: Int? = null,
      heightPx: Int? = null,
      componentFunction: (ComponentScope.() -> Component)? = null
  ): TestLithoView {
    val testLithoView = TestLithoView(context, componentsConfiguration)
    componentTree?.let { testLithoView.useComponentTree(componentTree) }
    lithoView?.let { testLithoView.useLithoView(lithoView) }
    widthPx?.let { heightPx?.let { testLithoView.setSizePx(widthPx, heightPx) } }
    componentFunction?.let {
      testLithoView.setRoot(with(ComponentScope(context)) { componentFunction() })
    }
    return testLithoView
  }

  /** Sets the new root to render. */
  @JvmOverloads
  fun render(
      lithoView: LithoView? = null,
      componentTree: ComponentTree? = null,
      widthPx: Int? = null,
      heightPx: Int? = null,
      componentFunction: ComponentScope.() -> Component
  ): TestLithoView {
    val testLithoView =
        createTestLithoViewWith(
            lithoView = lithoView,
            componentTree = componentTree,
            widthPx = widthPx,
            heightPx = heightPx)
    return testLithoView
        .attachToWindow()
        .setRoot(with(ComponentScope(context)) { componentFunction() })
        .measure()
        .layout()
  }

  /**
   * Perform any interactions defined in the [InteractionScope] or on the [LithoViewRule].
   *
   * During tests we need to make sure that everything is in sync in the Main Thread and in the
   * Background Thread, just like in real life use case. This functions takes off the responsibility
   * from you to use the Loopers and manage the thread synchronisation. You only need to pass here
   * one of the defined interactions from [LithoViewRule] or [InteractionScope], and we will take
   * care of all of the rest
   */
  fun act(
      testLithoView: TestLithoView,
      action: TestLithoView.InteractionsScope.() -> Unit
  ): LithoViewRule {
    testLithoView.InteractionsScope().action()
    idle()
    return this
  }

  /**
   * Runs through all tasks on the background thread and main lopper, blocking until it completes.
   * Use if there are any async events triggered by layout ( ie visibility events) to manually drain
   * the queue
   */
  fun idle() {
    threadLooperController.runToEndOfTasksSync()
    shadowOf(Looper.getMainLooper()).idle()
  }
}
