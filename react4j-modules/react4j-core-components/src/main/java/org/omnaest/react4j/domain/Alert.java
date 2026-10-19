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

public interface Alert extends UIComponentWithContent<Alert>
{
    public Alert withStyle(Style style);

    /**
     * Adds a close button with which the user can dismiss the alert.
     * <p>
     * Dismissing is client-side script: the interactive renderer keeps the alert's visibility as state in the browser
     * and hides the alert when the button is pressed, without any round trip to the server. The static HTML render
     * ({@code NodeRenderType.HTML}) has no script to run and no channel back to the server, so it renders the same
     * close button (and the {@code alert-dismissible} class) but the button does nothing there. This is a structural
     * difference of the static render, not a missing field: do not rely on a dismissible alert being dismissable in
     * static output.
     *
     * @param dismissible
     *            {@code true} to add the close button
     * @return this
     */
    public Alert withDismissible(boolean dismissible);

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
}
