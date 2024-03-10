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
package org.omnaest.react4j.service.internal.component;

import java.util.UUID;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.SVGContainer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.utils.HexUtils;

import lombok.Getter;

public class SVGContainerImpl extends AbstractUIComponent<SVGContainer> implements SVGContainer
{
    private final NativeHtmlImpl nativeHtml;
    private final CSSBuilderImpl cssBuilder;

    public SVGContainerImpl(ComponentContext context)
    {
        super(context);
        this.nativeHtml = new NativeHtmlImpl(context);
        this.cssBuilder = new CSSBuilderImpl();
    }

    public SVGContainerImpl(ComponentContext context, NativeHtmlImpl nativeHtml, CSSBuilderImpl cssBuilder)
    {
        super(context);
        this.nativeHtml = nativeHtml;
        this.cssBuilder = cssBuilder;
    }

    @Override
    public SVGContainer withSvg(String svg)
    {
        this.nativeHtml.withSource(() -> this.cssBuilder.build() + "<div class=\"" + this.cssBuilder.getHtmlClassId() + "\"> " + svg + " </div>");
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return this.nativeHtml.asRenderer();
    }

    @Override
    public UIComponentProvider<SVGContainer> asTemplateProvider()
    {
        return () -> new SVGContainerImpl(this.context, this.nativeHtml, this.cssBuilder);
    }

    @Override
    public SVGContainer withCSS(Consumer<CSSBuilder> cssBuilderConsumer)
    {
        if (cssBuilderConsumer != null)
        {
            cssBuilderConsumer.accept(this.cssBuilder);
        }
        return this;
    }

    @Override
    public SVGContainer withAlignment(AlignmentProvider alignment)
    {
        return this.withCSS(css -> css.width(alignment.getWidth())
                                      .height(alignment.getHeight()));
    }

    private static class CSSBuilderImpl implements CSSBuilder
    {
        @Getter
        private final String htmlClassId = "_" + HexUtils.toHex(UUID.randomUUID()
                                                                    .toString()
                                                                    .hashCode());

        private String width;
        private String height;

        @Override
        public CSSBuilder width(String width)
        {
            this.width = width;
            return this;
        }

        @Override
        public CSSBuilder height(String height)
        {
            this.height = height;
            return this;
        }

        public String build()
        {
            String css = "<style>" + "div." + this.htmlClassId + ">svg {" + this.determineWidthCss() + " " + this.determineHeightCss() + " }" + "</style>";
            return css;
        }

        private String determineHeightCss()
        {
            return this.height != null ? "height: " + this.height + ";" : "";
        }

        private String determineWidthCss()
        {
            return this.width != null ? "width: " + this.width + ";" : "";
        }

    }
}
