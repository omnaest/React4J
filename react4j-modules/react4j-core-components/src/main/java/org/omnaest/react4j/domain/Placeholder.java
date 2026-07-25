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

public interface Placeholder extends UIComponent<Placeholder>
{
    public Placeholder withStyle(Style style);

    public Placeholder withSize(Size size);

    public Placeholder withColumns(int columns);

    public Placeholder withAnimation(Animation animation);

    public static enum Style
    {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK;

        public static Optional<Style> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Style::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Style::valueOf);
        }
    }

    public static enum Size
    {
        SM, LG;

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

    public static enum Animation
    {
        GLOW, WAVE;

        public static Optional<Animation> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Animation::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Animation::valueOf);
        }
    }
}
