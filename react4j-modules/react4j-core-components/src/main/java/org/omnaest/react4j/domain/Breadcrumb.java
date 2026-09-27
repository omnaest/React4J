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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.omnaest.react4j.service.internal.handler.domain.EventHandler;

public interface Breadcrumb extends UIComponent<Breadcrumb>
{
    public Breadcrumb addEntry(Consumer<BreadcrumbEntry> breadcrumbEntryConsumer);

    /**
     * Declares this {@link Breadcrumb} as an anchor target for CSS selection, by rendering the given
     * {@code locator} as the {@code id} attribute of the surrounding {@code <nav>} element.
     * <p>
     * This is mechanism (A) of the two {@code *Locator} mechanisms this component family uses - "be an anchor
     * target" - and is the same contract already published on {@link Card#withLinkLocator(String)} and
     * {@link GridContainer#withLinkLocator(String)}. It is the opposite of
     * {@link BreadcrumbEntry#withLinkedLocator(String)}, which makes an entry *point at* another component's
     * locator rather than *be* one.
     * <p>
     * Leave unset to render no {@code id} attribute at all - not an empty or {@code null}-valued one.
     *
     * @param locator
     *            the value to render as the {@code <nav>}'s {@code id} attribute, or {@code null} to render
     *            none
     * @return this
     */
    public Breadcrumb withLinkLocator(String locator);

    public static interface BreadcrumbEntry extends UIComponent<BreadcrumbEntry>
    {
        public BreadcrumbEntry withText(String text);

        public BreadcrumbEntry withLink(String link);

        public BreadcrumbEntry withLinkedLocator(String id);

        public BreadcrumbEntry withLinked(UIComponent component);

        public BreadcrumbEntry withActiveState(boolean active);

        /**
         * Registers a server-side click handler for this entry, invoked on a {@code POST /ui/event} round
         * trip when the entry's rendered anchor is clicked - mirroring
         * {@link Pagination.PaginationItem#onClick(EventHandler)} exactly.
         * <p>
         * <b>Deliberate renderer divergence.</b> This handler reaches the interactive React client renderer
         * only. A {@link org.omnaest.react4j.domain.rendering.node.NodeRenderType#HTML} static render has no
         * channel back to the server - there is no browser-side script to issue the {@code POST /ui/event}
         * request - so the HTML renderer ignores {@code onClick} entirely rather than emitting a handler
         * reference nothing can invoke. This is structural, not an oversight: a static page cannot round-trip
         * to the server by construction.
         * <p>
         * Per {@code Breadcrumb}'s entry-precedence rules, an {@code active} entry is never clickable
         * regardless of whether {@code onClick} is set, and where both {@code onClick} and {@code link}/
         * {@code withLinkedLocator} are set, {@code onClick} takes precedence on the interactive renderer.
         *
         * @param eventHandler
         *            the handler to invoke when this entry is clicked, or {@code null} to register none
         * @return this
         */
        public BreadcrumbEntry onClick(EventHandler eventHandler);
    }

    public static interface BreadcrumbProvider extends Supplier<Breadcrumb>
    {
    }

    public static interface BreadcrumbConsumer extends Consumer<Breadcrumb>
    {
    }
}
