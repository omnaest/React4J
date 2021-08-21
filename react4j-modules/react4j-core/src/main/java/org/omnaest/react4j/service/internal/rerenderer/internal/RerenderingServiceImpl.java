package org.omnaest.react4j.service.internal.rerenderer.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingNodeProviderRegistry;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.springframework.stereotype.Service;

/**
 * @see RerenderingService
 * @author omnaest
 */
@Service
public class RerenderingServiceImpl implements RerenderingService, RerenderingNodeProviderRegistry
{
    private Map<Target, RerenderedNodeProvider> targetToRerenderedNodeProvider = new ConcurrentHashMap<>();

    @Override
    public void register(Target target, RerenderedNodeProvider rerenderedNodeProvider)
    {
        this.targetToRerenderedNodeProvider.put(target, rerenderedNodeProvider);
    }

    @Override
    public Optional<TargetNode> rerenderTargetNode(Target target, Optional<Data> data)
    {
        List<Target> currentAndAllParentalTargets = StreamUtils.recursiveFlattened(Optional.ofNullable(target)
                                                                                           .map(Stream::of)
                                                                                           .orElse(Stream.empty()),
                                                                                   childTarget -> Stream.of(Target.from(Location.of(childTarget)
                                                                                                                                .getParent()))
                                                                                                        .filter(parentTarget -> !parentTarget.isEmpty()))
                                                               .distinct()
                                                               .collect(Collectors.toList());
        Optional<TargetNode> rerenderedNode = currentAndAllParentalTargets.stream()
                                                                          .filter(this.targetToRerenderedNodeProvider::containsKey)
                                                                          .findFirst()
                                                                          .map(iTarget -> BiElement.of(iTarget,
                                                                                                       this.targetToRerenderedNodeProvider.get(iTarget)))
                                                                          .filter(BiElement::hasNoNullValue)
                                                                          .map(targetAndNodeProvider -> new TargetNode(targetAndNodeProvider.getFirst(),
                                                                                                                       targetAndNodeProvider.getSecond()
                                                                                                                                            .apply(data)));
        return rerenderedNode;
    }
}
