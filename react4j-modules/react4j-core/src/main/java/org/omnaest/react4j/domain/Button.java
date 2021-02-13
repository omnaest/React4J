package org.omnaest.react4j.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.service.internal.handler.domain.EventHandler;

public interface Button extends UIComponent<Button>
{
    public Button withName(String name);

    public Button withStyle(Style style);

    public Button onClick(EventHandler eventHandler);

    public static enum Style
    {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK, LINK;

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