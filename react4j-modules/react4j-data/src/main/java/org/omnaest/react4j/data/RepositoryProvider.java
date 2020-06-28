package org.omnaest.react4j.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.data.RepositoryProvider.Tenant;

public interface RepositoryProvider extends Function<Tenant.Id, Tenant>
{
    public static interface Tenant extends Function<Repository.Id, Repository>
    {

        /**
         * @see #of(String...)
         * @author omnaest
         */
        public static interface Id extends Supplier<List<String>>
        {
            public static Id of(String... tokens)
            {
                return () -> Arrays.asList(tokens);
            }

            public static Id empty()
            {
                return () -> Collections.emptyList();
            }
        }
    }
}
