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
package org.omnaest.react4j.service.internal.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The six Bootstrap theme colours that can be configured. {@code light} and {@code dark} are not configurable: they are the neutral ends of the
 * scale that the colour mode switches between.
 *
 * @author omnaest
 */
@AllArgsConstructor
public enum ThemeColorRole
{
    PRIMARY("primary"), SECONDARY("secondary"), SUCCESS("success"), INFO("info"), WARNING("warning"), DANGER("danger");

    /**
     * The name Bootstrap uses in its class names and variables, e.g. {@code primary} in {@code .btn-primary} and {@code --bs-primary}
     */
    @Getter
    private final String cssName;
}
