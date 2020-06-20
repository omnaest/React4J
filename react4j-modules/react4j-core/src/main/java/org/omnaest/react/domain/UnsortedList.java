package org.omnaest.react.domain;

import java.util.function.Supplier;

public interface UnsortedList extends UIComponent<UnsortedList>
{
    public UnsortedList addText(String text);

    public UnsortedList addText(Icon icon, String text);

    public static enum Icon implements Supplier<String>
    {
        ENVELOPE("envelope-open-text"), DOLLAR_SIGN("dollar-sign"), MICROSCOPE("microscope"), HEARTBEAT("heartbeat"), DNA("dna");

        private String key;

        private Icon(String key)
        {
            this.key = key;
        }

        @Override
        public String get()
        {
            return key;
        }
    }
}