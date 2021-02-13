package org.omnaest.react4j.domain.data;

/**
 * {@link DataContext} that can be defined during declaration
 * 
 * @author omnaest
 */
public interface DefineableDataContext extends DataContext
{
    public DefineableDataContext withId(String id);

    public DefineableDataContext enableSingleton();

}
