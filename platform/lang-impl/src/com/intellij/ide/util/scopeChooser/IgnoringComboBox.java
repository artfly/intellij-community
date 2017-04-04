/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.util.scopeChooser;

import com.intellij.openapi.ui.ComboBox;

/**
* User: anna
*/
public abstract class IgnoringComboBox extends ComboBox {

  @Override
  public void setSelectedItem(final Object item) {
    if (!(isIgnored(item))) {
      super.setSelectedItem(item);
    }
  }

  @Override
  public void setSelectedIndex(final int anIndex) {
    final Object item = getItemAt(anIndex);
    if (!isIgnored(item)) {
      super.setSelectedIndex(anIndex);
    }
  }

  protected abstract boolean isIgnored(Object item);
}
