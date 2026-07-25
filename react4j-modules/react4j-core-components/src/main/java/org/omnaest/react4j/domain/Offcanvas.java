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
package org.omnaest.react4j.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.support.UIComponentWithContent;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;

public interface Offcanvas extends UIComponentWithContent<Offcanvas>
{
    public Offcanvas withTitle(String title);

    public Offcanvas withVisible(boolean visible);

    public Offcanvas withPlacement(Placement placement);

    public Offcanvas onClose(EventHandler eventHandler);

    public static enum Placement
    {
        START, END, TOP, BOTTOM;

        public static Optional<Placement> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Placement::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Placement::valueOf);
        }
    }
}
