package org.omnaest.react4j.service.internal.controller;

import java.util.Optional;

import org.omnaest.react4j.service.internal.handler.EventHandlerService;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReactUIController
{
    @Autowired
    private RootNodeResolverService resolverService;

    @Autowired
    private EventHandlerService eventHandlerService;

    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.GET, path = "/ui/{contextPath}", produces = "application/json")
    public NodeHierarchy getNodeHierarchy(@PathVariable("contextPath") String contextPath)
    {
        return this.resolverService.resolveNodeHierarchy(contextPath);
    }

    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.GET, path = "/ui", produces = MediaType.APPLICATION_JSON_VALUE)
    public NodeHierarchy getNodeHierarchy()
    {
        return this.resolverService.resolveDefaultNodeHierarchy();
    }

    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.POST, path = "/ui/event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<ResponseBody> acceptEvent(@RequestBody EventBody eventBody)
    {
        return this.eventHandlerService.handleEvent(eventBody);
    }

}
