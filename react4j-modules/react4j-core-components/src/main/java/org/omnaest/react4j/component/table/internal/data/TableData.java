package org.omnaest.react4j.component.table.internal.data;

import java.util.Collections;
import java.util.List;

import org.omnaest.react4j.component.table.internal.TableImpl.RowImpl;
import org.omnaest.react4j.domain.i18n.I18nText;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
public class TableData
{
    @Default
    private List<I18nText> columnTitles = Collections.emptyList();

    @Singular("addRow")
    private List<RowImpl> rows;

    @Default
    private boolean responsive = true;

    @Default
    private String size = "";
}