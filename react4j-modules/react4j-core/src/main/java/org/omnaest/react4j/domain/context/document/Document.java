package org.omnaest.react4j.domain.context.document;

import org.omnaest.react4j.domain.context.Context;

/**
 * @author omnaest
 */
public interface Document
{
    public Field getField(String fieldName);

    public static interface Field
    {
        public Context getContext();

        public String getFieldName();

        public Document getDocument();
    }

    public Context getContext();
}
