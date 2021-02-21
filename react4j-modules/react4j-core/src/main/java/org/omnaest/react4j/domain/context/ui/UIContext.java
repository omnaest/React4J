package org.omnaest.react4j.domain.context.ui;

import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.document.DocumentList;

/**
 * A context within the UI that allows to read and manipulate data within the UI and decoupled from any server call or state.
 * 
 * @author omnaest
 */
public interface UIContext extends Context, DocumentList
{

}
