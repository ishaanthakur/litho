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

package com.facebook.rendercore.testing;

import static com.facebook.rendercore.RenderUnit.Extension.extension;

import android.content.Context;
import android.view.View;
import com.facebook.rendercore.RenderUnit;

public class SimpleViewUnit extends RenderUnit<View> {

  private final View view;
  private final long id;

  public SimpleViewUnit(final View view, final long id) {
    super(RenderType.VIEW);
    this.view = view;
    this.id = id;
  }

  @Override
  public View createContent(Context c) {
    return view;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Object getRenderContentType() {
    return view.getClass();
  }

  public SimpleViewUnit addBindBinders(RenderUnit.Binder<SimpleViewUnit, View>... binders) {
    for (Binder<SimpleViewUnit, View> binder : binders) {
      super.addAttachDetachExtension(extension(this, binder));
    }

    return this;
  }

  public SimpleViewUnit addMounBinders(RenderUnit.Binder<SimpleViewUnit, View>... binders) {
    for (Binder<SimpleViewUnit, View> binder : binders) {
      super.addMountUnmountExtension(extension(this, binder));
    }

    return this;
  }
}
