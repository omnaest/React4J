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
package org.omnaest.react4j.service.internal.service.internal.theme;

import lombok.Value;

/**
 * The two pieces of index.html markup the theme contributes
 *
 * @see ThemeHeadRenderer
 * @author omnaest
 */
@Value
public class ThemeHead
{
    /**
     * The markup replacing the head placeholder: stylesheet links and, for the AUTO colour mode, the colour mode script
     */
    String headMarkup;

    /**
     * The complete attribute text replacing the attribute placeholder of the {@code html} element, i.e. {@code data-bs-theme="light"}, or an
     * empty string if no attribute is written.
     */
    String htmlAttribute;
}
