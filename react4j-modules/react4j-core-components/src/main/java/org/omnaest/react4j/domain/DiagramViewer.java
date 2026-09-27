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

/**
 * Displays an SVG diagram and, by default, fits the whole diagram into its container - no clipping, no
 * scrolling required to see its extremities. When {@link #withInteractive(boolean)} is left at its default
 * ({@code true}), the client additionally supports zoom (in / out / reset) and pan by rewriting the rendered
 * {@code <svg>}'s {@code viewBox} around a base captured on mount; reset restores that base exactly.
 * <p>
 * Own node type ({@code DIAGRAMVIEWER}), own client renderer - unlike {@link SVGContainer}, which is a thin
 * delegation to {@link NativeHtml}'s raw-HTML escape hatch and therefore cannot host any client-side
 * interaction (a script inserted via {@code dangerouslySetInnerHTML} never executes).
 * <p>
 * Sizing is deliberately typed - {@link #withMaxHeight(String)} / {@link #withWidth(String)} - rather than a
 * {@code withCSS(CSSBuilder)} escape hatch: a typed field is read straight into an inline {@code style} on
 * this component's own root element, which travels with that element wherever React mounts it (including a
 * portalled modal, outside the document subtree any selector-based rule could reach), with no selector and
 * therefore no specificity contest to lose.
 *
 * @author omnaest
 */
public interface DiagramViewer extends UIComponent<DiagramViewer>
{
    /**
     * The SVG markup to display, carried verbatim.
     *
     * @param svg
     * @return this
     */
    public DiagramViewer withSvg(String svg);

    /**
     * The CSS max-height bound of this component's own root element - the box the diagram is fitted into.
     * Defaults to {@code 60vh}.
     *
     * @param cssValue
     *            a CSS length value, e.g. {@code "400px"}, {@code "60vh"}
     * @return this
     */
    public DiagramViewer withMaxHeight(String cssValue);

    /**
     * The CSS width of this component's own root element. Defaults to {@code 100%}.
     *
     * @param cssValue
     *            a CSS length value, e.g. {@code "100%"}, {@code "600px"}
     * @return this
     */
    public DiagramViewer withWidth(String cssValue);

    /**
     * The CSS height of this component's own root element - unlike {@link #withMaxHeight(String)}, a floor
     * rather than a ceiling: it tells the box to *be* this tall, not merely to grow no taller than this.
     * Unset by default, so a consumer that never calls this method gets a byte-for-byte unchanged rendered
     * node - the same reasoning that motivates {@link #withMaxHeight(String)} and {@link #withWidth(String)}
     * being typed CSS-value strings rather than a {@code withCSS(CSSBuilder)} escape hatch: a typed field is
     * read straight into an inline {@code style} on this component's own root element, which travels with
     * that element wherever React mounts it (including a portalled modal), with no selector and therefore no
     * specificity contest to lose. Note that a percentage value resolves only against an ancestor whose own
     * {@code height} is itself definite - making that ancestor definite is the embedding application's
     * layout concern, not this component's.
     *
     * @param cssValue
     *            a CSS length value, e.g. {@code "100%"}, {@code "600px"}
     * @return this
     */
    public DiagramViewer withHeight(String cssValue);

    /**
     * Whether the client renders zoom/pan controls and wires up the corresponding interaction. Default is
     * {@code true}. Set to {@code false} for a click-to-open thumbnail that should not itself react to
     * wheel/drag - e.g. an on-card preview whose click opens a separate, interactive rendering elsewhere.
     *
     * @param interactive
     * @return this
     */
    public DiagramViewer withInteractive(boolean interactive);
}
