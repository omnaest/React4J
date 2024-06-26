/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.domain.context.ui;

import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.context.document.DocumentList;

/**
 * A context within the UI that allows to read and manipulate data within the UI and decoupled from any server call or state.
 * 
 * @author omnaest
 */
public interface UIContext extends Context, DocumentList
{

    public Field getField(String fieldName);

    public UIContext withId(String id);

}
