/*******************************************************************************
 * Copyright 2021 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.omnaest.react4j.component.treetable.TreeTableStateKeys;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.components.UIComponents;
import org.omnaest.react4j.domain.context.data.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link UIComponents} over the submitted {@link Data}: resolve the name to a {@link Location}, then write the
 * fields the component itself reads.
 * <p>
 * Every key comes from {@link TreeTableStateKeys}, never spelled out here. A write under a key the renderer does
 * not read has no effect and reports no error, so a second derivation of the same key would be a silent failure
 * waiting to happen.
 *
 * @author omnaest
 */
@Service
public class UIComponentsImpl implements UIComponents
{
    @Autowired
    private NamedComponentRegistry namedComponentRegistry;

    @Override
    public ComponentAccess in(Data data)
    {
        return new ComponentAccess()
        {
            @Override
            public TreeTableAccess treeTable(String name)
            {
                return new TreeTableAccessImpl(data, UIComponentsImpl.this.namedComponentRegistry.resolve(name));
            }
        };
    }

    /**
     * Resolves once, at construction, and then either writes or does nothing.
     * <p>
     * The absent case is a no-op rather than a throw. A page legitimately renders without a given table - a
     * different view mode, a permission the principal lacks - and a handler that reacts to a message should not
     * have to know which of those is true today.
     */
    private static class TreeTableAccessImpl implements TreeTableAccess
    {
        private final Data                         data;

        private final Optional<TreeTableStateKeys> keys;

        TreeTableAccessImpl(Data data, Optional<Location> gridLocation)
        {
            this.data = data;
            this.keys = gridLocation.map(TreeTableStateKeys::of);
        }

        @Override
        public boolean isPresent()
        {
            return this.keys.isPresent();
        }

        @Override
        public TreeTableAccess setFlatMode(boolean flat)
        {
            this.keys.ifPresent(k -> this.data.setFieldValue(k.flatMode(), flat));
            return this;
        }

        @Override
        public TreeTableAccess setFilter(String columnKey, String value)
        {
            // An empty string rather than a removal: the renderer treats an ABSENT filter field and a PRESENT
            // empty one identically (no filter), and an empty value is what the user's own cleared filter box
            // submits. Matching that keeps one code path rather than two.
            this.keys.ifPresent(k -> this.data.setFieldValue(k.filter(columnKey), value != null ? value : ""));
            return this;
        }

        @Override
        public TreeTableAccess clearFilters()
        {
            this.keys.ifPresent(k -> new ArrayList<>(k.filterKeysAmong(this.data.toMap()
                                                                                .keySet())).forEach(filterKey -> this.data.setFieldValue(filterKey, "")));
            return this;
        }

        @Override
        public TreeTableAccess setSort(String columnKey, boolean ascending)
        {
            // Replaces rather than appends. A caller asking for "sort by city" means that, not "add city to
            // whatever ordering happens to be in force" - and the multi-column spec is ordered, so appending
            // would make the result depend on history the caller cannot see.
            this.keys.ifPresent(k -> this.data.setFieldValue(k.sort(),
                                                             List.of(TreeTableStateKeys.encodeSort(columnKey, ascending ? "ASCENDING" : "DESCENDING"))));
            return this;
        }

        @Override
        public TreeTableAccess clearSort()
        {
            // An empty list, not an absent field. Absent means "never touched" and re-seeds the configured
            // initial sort; present-but-empty means "explicitly none", which is what clearing asks for.
            this.keys.ifPresent(k -> this.data.setFieldValue(k.sort(), Collections.emptyList()));
            return this;
        }

        @Override
        public TreeTableAccess setFiltersVisible(boolean visible)
        {
            this.keys.ifPresent(k -> this.data.setFieldValue(k.filtersVisible(), visible));
            return this;
        }
    }
}
