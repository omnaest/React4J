package org.omnaest.react4j.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public interface Icon extends UIComponent<Icon>
{
    public Icon from(StandardIcon value);

    public static enum StandardIcon implements Supplier<String>
    {
        ENVELOPE("envelope-open-text"), DOLLAR_SIGN("dollar-sign"), MICROSCOPE("microscope"), HEARTBEAT("heartbeat"), DNA("dna");

        private String key;

        private StandardIcon(String key)
        {
            this.key = key;
        }

        @Override
        public String get()
        {
            return key;
        }

        public static Optional<StandardIcon> of(String value)
        {
            return Arrays.asList(values())
                         .stream()
                         .filter(icon -> icon.name()
                                             .equals(value))
                         .findFirst();
        }
    }
}