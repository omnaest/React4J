package org.omnaest.react4j.component.table.internal.renderer;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.component.table.internal.TableImpl.CellImpl;
import org.omnaest.react4j.component.table.internal.TableImpl.RowImpl;
import org.omnaest.react4j.component.table.internal.data.TableData;
import org.omnaest.react4j.component.table.internal.renderer.node.TableNode;
import org.omnaest.react4j.component.table.internal.renderer.node.TableNode.CellNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.functional.Provider;
import org.omnaest.utils.template.TemplateUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TableRendererImpl implements UIComponentRenderer
{
    private final TableData                    data2;
    private final LocalizedTextResolverService textResolver;
    private final Provider<String>             idProvider;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return new TableNode().setColumnTitles(this.textResolver.apply(this.data2.getColumnTitles(), location))
                              .setRows(this.data2.getRows()
                                                 .stream()
                                                 .map(MapperUtils.withIntCounter())
                                                 .map(rowAndIndex -> new TableNode.RowNode().setCells(rowAndIndex.getFirst()
                                                                                                                 .getCells()
                                                                                                                 .stream()
                                                                                                                 .map(MapperUtils.withIntCounter())
                                                                                                                 .map(this.createCellRenderer(renderingProcessor,
                                                                                                                                              location,
                                                                                                                                              rowAndIndex))
                                                                                                                 .collect(Collectors.toList())))
                                                 .collect(Collectors.toList()));
    }

    private Function<BiElement<CellImpl, Integer>, CellNode> createCellRenderer(RenderingProcessor renderingProcessor, Location location,
                                                                                BiElement<RowImpl, Integer> rowAndIndex)
    {
        return cellAndIndex ->
        {
            Location cellLocation = this.createCellLocation(location, rowAndIndex, cellAndIndex);
            CellImpl cell = cellAndIndex.getFirst();
            return new TableNode.CellNode().setContent(renderingProcessor.process(cell.getContent(), cellLocation)

            );
        };
    }

    private Location createCellLocation(Location location, BiElement<RowImpl, Integer> rowAndIndex, BiElement<CellImpl, Integer> cellAndIndex)
    {
        return Optional.ofNullable(location)
                       .map(iLocation -> iLocation.and("row" + rowAndIndex.getSecond())
                                                  .and("cell" + cellAndIndex.getSecond()))
                       .orElse(null);
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
        registry.register(TableNode.class, NodeRenderType.HTML, new NodeRenderer<TableNode>()
        {
            @Override
            public String render(TableNode node, NodeRenderingProcessor nodeRenderingProcessor)
            {
                return TemplateUtils.builder()
                                    .useTemplateClassResource(this.getClass(), "/render/templates/html/table.html")
                                    .add("columns", node.getColumnTitles()
                                                        .stream()
                                                        .map(nodeRenderingProcessor::render)
                                                        .collect(Collectors.toList()))
                                    .add("rows", node.getRows()
                                                     .stream()
                                                     .map(rowNode -> rowNode.getCells()
                                                                            .stream()
                                                                            .map(CellNode::getContent)
                                                                            .map(nodeRenderingProcessor::render)
                                                                            .collect(Collectors.toList()))
                                                     .collect(Collectors.toList()))
                                    .build()
                                    .get();
            }

        });
    }

    @Override
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
    {
        return this.data2.getRows()
                         .stream()
                         .map(MapperUtils.withIntCounter())
                         .flatMap(rowAndIndex -> rowAndIndex.getFirst()
                                                            .getCells()
                                                            .stream()
                                                            .map(MapperUtils.withIntCounter())
                                                            .map(cellAndIndex -> ParentLocationAndComponent.of(this.createCellLocation(parentLocation,
                                                                                                                                       rowAndIndex,
                                                                                                                                       cellAndIndex),
                                                                                                               cellAndIndex.getFirst()
                                                                                                                           .getContent())));
    }
}