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
package org.omnaest.react4j;

import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.support.UIContentHolder.Layout;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Standalone, tree-table-only showcase view: the SOLE served page when the {@code treeTableFullWindow} Spring
 * profile is active - a single {@code TreeTable}, backed by the generated ~10,050-node
 * {@link LargeTreeTableDataProvider} tree, stretched to fill the ENTIRE browser viewport (full width AND full
 * height, with the row body scrolling inside the window) via the framework's own
 * {@link Layout#FULL_VIEWPORT_SIZE} layout constant - a {@code ScrollbarContainer} preset to
 * {@code VerticalBoxMode.FULL_VIEWPORT_HEIGHT} / {@code HorizontalBoxMode.FULL_VIEWPORT_WIDTH} (same constant
 * {@code IMBSApplication.LibraryComponent} already uses for a sibling {@code Layout} value, so this is an
 * established, not novel, framework idiom). No {@code Card}, no other showcase section, no {@code NavigationBar} -
 * {@code HomePage.tsx} renders the plain {@code body-full} shell with zero chrome (no navbar row) whenever no
 * {@code NavigationBar} is configured, which this provider deliberately never does.
 * <p>
 * <b>Single active provider.</b> {@code @Profile("treeTableFullWindow")} here, paired with
 * {@code @Profile("!treeTableFullWindow")} added to both {@link ComponentShowcaseUI} and {@link MockUI}, makes the
 * three page providers for this app's ONE default context path ({@code ReactUIService#DEFAULT_CONTEXT_PATH})
 * mutually exclusive Spring beans - Spring creates exactly one of the three at context-refresh time depending on
 * the active profile, so exactly one {@code @PostConstruct}/{@code ReactUIProvider.accept(...)} ever calls
 * {@code ReactUIService}'s {@code computeIfAbsent}-keyed root registration for that path. This is deterministic,
 * not a bean-initialization-order race - contrast with the PRE-EXISTING {@code ComponentShowcaseUI}/{@code MockUI}
 * collision (both unconditionally instantiated, same default context path, first {@code @PostConstruct} wins by
 * accidental ordering - see {@code ComponentShowcaseUI}'s class javadoc) that this profile split also incidentally
 * makes deterministic for the default (non-{@code treeTableFullWindow}) case.
 * <p>
 * Unlike {@code ComponentShowcaseUI}/{@code MockUI} (which call {@code ReactUIService.getOrCreateDefaultRoot(...)}
 * directly from their own {@code @PostConstruct}), this class implements the framework's {@link ReactUIProvider}
 * composition-root interface (see memory {@code react4j-reactuiprovider-composition-root}), the properly-supported
 * mechanism driven by the framework's {@code ReactUIProviderInitializer} - the same pattern
 * {@code DeployerApplication}'s {@code DeployerUI} uses.
 *
 * @author omnaest
 */
@Service
@Profile("treeTableFullWindow")
public class TreeTableFullWindowUI implements ReactUIProvider
{
    /**
     * ~25-40 rows per sibling-group window, per the brief: large enough to visibly fill a tall browser window,
     * small enough that load-more/expand/sort/filter stay all exercisable and the DOM never renders more than one
     * window's worth of rows per group at a time - the point of the lazy, server-driven row model even against a
     * 10,050-node backing tree.
     */
    private static final int                 WINDOW_SIZE           = 30;

    private final LargeTreeTableDataProvider treeTableDataProvider = new LargeTreeTableDataProvider();

    @Override
    public void accept(ReactUI reactUI)
    {
        reactUI.addNewComponent(factory -> Layout.FULL_VIEWPORT_SIZE.apply(factory)
                                                                    .withContent(factory.newTreeTable()
                                                                                        .withColumns(TreeTableColumn.of("name", "Name")
                                                                                                                    .withInitialSortDirection(SortDirection.DESCENDING),
                                                                                                     TreeTableColumn.of("owner", "Owner"),
                                                                                                     TreeTableColumn.of("modified", "Modified"),
                                                                                                     TreeTableColumn.of("kind", "Kind")
                                                                                                                    .withFilterable(false)
                                                                                                                    .withSortable(false))
                                                                                        .withDataProvider(this.treeTableDataProvider)
                                                                                        .withWindowSize(WINDOW_SIZE)
                                                                                        .withFlatModeToggleEnabled(true)
                                                                                        .withMultiColumnSortEnabled(true)));
    }
}
