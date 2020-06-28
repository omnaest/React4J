package org.omnaest.react4j;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.stereotype.Service;

/**
 * Any {@link Service} can implement the {@link ReactUIProvider} interface to supply a {@link ReactUI} configuration.
 * 
 * @author omnaest
 */
public interface ReactUIProvider extends Consumer<ReactUI>
{
    public default String getContextPath()
    {
        return ReactUIService.DEFAULT_CONTEXT_PATH;
    }
}
