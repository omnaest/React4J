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

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.i18n.I18nText;

public interface Paragraph extends UIComponent<Paragraph>
{
    /**
     * @see Text
     * @param text
     * @return
     */
    public Paragraph addText(String text);

    public Paragraph addNonTranslatedText(String text);

    public Paragraph addText(I18nText i18nText);

    public Paragraph addText(StandardIcon icon, String text);

    public Paragraph addText(StandardIcon icon, I18nText text);

    /**
     * @see Heading
     * @param text
     * @param level
     * @return
     */
    public Paragraph addHeading(String text, int level);

    public Paragraph addLink(Consumer<Anker> ankerConsumer);

    public <E> Paragraph addLinks(Collection<E> sourceElements, BiConsumer<Anker, E> ankerAndSourceElementConsumer);

    public Paragraph addLinkButton(Consumer<AnkerButton> ankerButtonConsumer);

    /**
     * Reads the {@link StandardCharsets#UTF_8} encoded text file from the given absolute resource path
     * 
     * @param resourcePath
     * @return
     */
    public Paragraph addTextsByClasspathResource(String resourcePath);

    public Paragraph addLineBreak();

    public Paragraph withBoldStyle(boolean bold);

    public Paragraph withBoldStyle();

    public Paragraph addImage(String name, String imageName);

    public <E> Paragraph withElements(Collection<E> sourceElements, BiConsumer<Paragraph, E> paragraphAndSourceElementConsumer);

    public Paragraph addComponent(UIComponent<?> component);

    public Paragraph withLineBreakAfterEachText();

    public Paragraph withLineBreakAfterEachText(boolean enabled);

}
