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

public interface Modal extends UIComponentWithContent<Modal>
{
    public Modal withTitle(String title);

    public Modal withFooter(UIComponent<?> footer);

    public Modal withVisible(boolean visible);

    public Modal withSize(Size size);

    public Modal withCentered(boolean centered);

    public Modal onClose(EventHandler eventHandler);

    public static enum Size
    {
        SMALL, LARGE, EXTRA_LARGE;

        public String toBootstrapToken()
        {
            switch (this)
            {
                case SMALL :
                    return "sm";
                case LARGE :
                    return "lg";
                case EXTRA_LARGE :
                    return "xl";
                default :
                    return null;
            }
        }

        public static Optional<Size> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Size::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Size::valueOf);
        }
    }
}
