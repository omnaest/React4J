package org.omnaest.react;

import org.omnaest.react.domain.ReactUI;

public interface ReactUIService
{
    public ReactUI createRoot(String path);

    public ReactUI createDefaultRoot();

    public ReactUI getOrCreateRoot(String contextPath);

    public ReactUI getOrCreateDefaultRoot();
}
