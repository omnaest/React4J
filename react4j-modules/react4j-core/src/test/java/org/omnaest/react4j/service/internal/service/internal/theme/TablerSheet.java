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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A read-only view of the SHIPPED {@code /public/css/theme/react4j-tabler.css}, loaded from the test classpath (the react4j-core-ui artifact), that
 * answers the two questions the Tabler token guards ask of it: where is a custom property declared and with which value, and is it READ through
 * {@code var(--name)} or {@code var(--name, fallback)}. Test support only: the sheet is compressed Sass output, so this is a deliberately small
 * scanner and not a CSS parser.
 */
final class TablerSheet
{
    static final String SHIPPED_PATH = "/public/css/theme/react4j-tabler.css";

    /**
     * One declaration {@code --name: value} with the selector text of the rule it sits in
     */
    static final class Declaration
    {
        private final String selector;
        private final String value;

        Declaration(String selector, String value)
        {
            this.selector = selector;
            this.value = value;
        }

        String getSelector()
        {
            return this.selector;
        }

        String getValue()
        {
            return this.value;
        }
    }

    private final String css;

    private TablerSheet(String css)
    {
        this.css = css;
    }

    static TablerSheet shipped()
    {
        try (InputStream stream = TablerSheet.class.getResourceAsStream(SHIPPED_PATH))
        {
            if (stream == null)
            {
                throw new IllegalStateException("The shipped " + SHIPPED_PATH + " is not on the test classpath: install react4j-core-ui first");
            }
            return new TablerSheet(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read the shipped " + SHIPPED_PATH, e);
        }
    }

    static TablerSheet of(String css)
    {
        return new TablerSheet(css);
    }

    String getCss()
    {
        return this.css;
    }

    /**
     * @return true if the sheet contains {@code var(--name)} or {@code var(--name,...)}: the property is READ, not merely declared
     */
    boolean isRead(String name)
    {
        return Pattern.compile("var\\(" + Pattern.quote(name) + "[,)]")
                      .matcher(this.css)
                      .find();
    }

    /**
     * @return every declaration of the property, in source order
     */
    List<Declaration> declarations(String name)
    {
        List<Declaration> declarations = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?<![\\w-])" + Pattern.quote(name) + ":\\s*([^;}]*)")
                                 .matcher(this.css);
        while (matcher.find())
        {
            declarations.add(new Declaration(selectorOf(matcher.start()), matcher.group(1)
                                                                                 .trim()));
        }
        return declarations;
    }

    /**
     * @return the value of the last declaration of the property, i.e. the one that wins at equal specificity
     */
    String lastValue(String name)
    {
        List<Declaration> declarations = this.declarations(name);
        if (declarations.isEmpty())
        {
            throw new IllegalStateException(name + " is not declared in the shipped sheet");
        }
        return declarations.get(declarations.size() - 1)
                           .getValue();
    }

    /**
     * The selector text in front of the rule that holds the declaration at the given index: from the end of the previous rule or comment up to the
     * opening brace, so a rule inside {@code @media} shows the at-rule in its selector, which no allow list accepts.
     */
    private String selectorOf(int declarationIndex)
    {
        int open = this.css.lastIndexOf('{', declarationIndex);
        int start = Math.max(this.css.lastIndexOf('}', open), this.css.lastIndexOf("*/", open) + 1);
        return this.css.substring(start + 1, open)
                       .trim();
    }
}
