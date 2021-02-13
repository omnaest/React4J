package org.omnaest.react4j.service;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.configuration.HomePageConfiguration;

public interface ReactUIService
{
    public static final String DEFAULT_CONTEXT_PATH = "/";

    public ReactUIService createRoot(String path, Consumer<ReactUI> reactUIConsumer);

    public ReactUIService createDefaultRoot(Consumer<ReactUI> reactUIConsumer);

    public ReactUIService getOrCreateRoot(String contextPath, Consumer<ReactUI> reactUIConsumer);

    public ReactUIService getOrCreateDefaultRoot(Consumer<ReactUI> reactUIConsumer);

    public ReactUIService disableCaching();

    public ReactUIService enableCaching(boolean active);

    public ReactUIService withCacheDurationInSeconds(int cacheDurationInSeconds);

    public ReactUIService configureHomePage(Consumer<HomePageConfiguration> configurationConsumer);
}
