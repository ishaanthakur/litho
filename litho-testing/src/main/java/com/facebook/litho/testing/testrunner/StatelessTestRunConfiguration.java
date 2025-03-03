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

package com.facebook.litho.testing.testrunner;

import com.facebook.litho.config.ComponentsConfiguration;
import org.junit.runners.model.FrameworkMethod;

/** Run configuration that enabled stateless components. */
public class StatelessTestRunConfiguration implements LithoTestRunConfiguration {

  private boolean defaultUseStatelessComponent;
  private boolean defaultReuseInternalNodes;
  private ComponentsConfiguration.Builder defaultConfigBuilder;

  @Override
  public void beforeTest(FrameworkMethod method) {
    defaultUseStatelessComponent = ComponentsConfiguration.useStatelessComponent;
    defaultReuseInternalNodes = ComponentsConfiguration.reuseInternalNodes;
    defaultConfigBuilder = ComponentsConfiguration.getDefaultComponentsConfigurationBuilder();

    ComponentsConfiguration.useStatelessComponent = true;
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create()
            .useStatelessComponents(true)
            .useInputOnlyInternalNodes(true)
            .reuseInternalNodes(true));
  }

  @Override
  public void afterTest(FrameworkMethod method) {
    ComponentsConfiguration.useStatelessComponent = defaultUseStatelessComponent;
    ComponentsConfiguration.reuseInternalNodes = defaultReuseInternalNodes;
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(defaultConfigBuilder);
  }
}
