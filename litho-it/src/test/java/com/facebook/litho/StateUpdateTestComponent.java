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
import java.util.concurrent.atomic.AtomicInteger;

class StateUpdateTestComponent extends SpecGeneratedComponent {

  private static final int STATE_UPDATE_TYPE_NOOP = 0;
  private static final int STATE_UPDATE_TYPE_INCREMENT = 1;
  private static final int STATE_UPDATE_TYPE_MULTIPLY = 2;

  static final int INITIAL_COUNT_STATE_VALUE = 4;

  static StateContainer.StateUpdate createNoopStateUpdate() {
    return new StateContainer.StateUpdate(STATE_UPDATE_TYPE_NOOP);
  }

  static StateContainer.StateUpdate createIncrementStateUpdate() {
    return new StateContainer.StateUpdate(STATE_UPDATE_TYPE_INCREMENT);
  }

  static StateContainer.StateUpdate createMultiplyStateUpdate() {
    return new StateContainer.StateUpdate(STATE_UPDATE_TYPE_MULTIPLY);
  }

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private final AtomicInteger createInitialStateCount = new AtomicInteger(0);
  private static final AtomicInteger finalCounterValue = new AtomicInteger(0);

  StateUpdateTestComponent() {
    super("StateUpdateTest");
    setStateContainer(new TestStateContainer());
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }

  @Override
  protected boolean hasState() {
    return true;
  }

  @Override
  protected void createInitialState(ComponentContext c) {
    getStateContainerImpl(c).mCount = INITIAL_COUNT_STATE_VALUE;
    createInitialStateCount.incrementAndGet();
    finalCounterValue.set(INITIAL_COUNT_STATE_VALUE);
  }

  @Override
  protected void transferState(
      StateContainer prevStateContainer, StateContainer nextStateContainer) {
    TestStateContainer prevStateContainerImpl = (TestStateContainer) prevStateContainer;
    TestStateContainer nextStateContainerImpl = (TestStateContainer) nextStateContainer;
    nextStateContainerImpl.mCount = prevStateContainerImpl.mCount;
  }

  int getCount(ComponentContext c) {
    return finalCounterValue.get();
  }

  StateUpdateTestComponent getComponentForStateUpdate() {
    return this;
  }

  @Nullable
  @Override
  protected StateContainer createStateContainer() {
    return new TestStateContainer();
  }

  @Nullable
  protected TestStateContainer getStateContainerImpl(ComponentContext c) {
    return (TestStateContainer) Component.getStateContainer(c, this);
  }

  static class TestStateContainer extends StateContainer {
    protected int mCount;

    @Override
    public void applyStateUpdate(StateUpdate stateUpdate) {
      switch (stateUpdate.type) {
        case STATE_UPDATE_TYPE_NOOP:
          break;

        case STATE_UPDATE_TYPE_INCREMENT:
          mCount += 1;
          break;

        case STATE_UPDATE_TYPE_MULTIPLY:
          mCount *= 2;
          break;
      }
      finalCounterValue.set(mCount);
    }
  }
}
