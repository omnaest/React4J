package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.ButtonFormElement.ButtonEventHandler;
import org.omnaest.react4j.component.form.Form.FileUploadFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormFileUploadNode;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistry;

public class FileUploadFormElementImpl extends AbstractFormElementImpl<FileUploadFormElement> implements FileUploadFormElement
{
    private final UploadChannelRegistry uploadChannelRegistry;

    private UploadChannel               uploadChannel;
    private String                      accept;
    private DataEventHandler            eventHandler = (data, internalData) -> MappedData.builder()
                                                                                         .data(data)
                                                                                         .internalData(internalData)
                                                                                         .build();

    public FileUploadFormElementImpl(Function<Class<?>, String> identityProvider, LocalizedTextResolverService textResolver, Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry, Supplier<? extends DataContext> parentDataContext, UploadChannelRegistry uploadChannelRegistry)
    {
        super(identityProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
        this.uploadChannelRegistry = uploadChannelRegistry;
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        Context dataContext = this.getEffectiveContext();
        Location uploadLocation = location.and("fileUpload");
        String uploadId = this.uploadChannelRegistry.register(uploadLocation, this.uploadChannel);
        Target target = Target.from(uploadLocation);
        this.eventHandlerRegistry.registerDataEventHandler(target, this.eventHandler);

        return node.toBuilder()
                   .type("FILE_UPLOAD")
                   .fileUpload(FormFileUploadNode.builder()
                                                 .uploadUrl("ui/upload")
                                                 .uploadId(uploadId)
                                                 .accept(this.accept)
                                                 .maxSize(this.uploadChannel.maxSizeBytes())
                                                 .onComplete(new ServerHandler(target).setContextId(dataContext.getId(location)))
                                                 .build())
                   .build();
    }

    @Override
    public FileUploadFormElement withUploadChannel(UploadChannel uploadChannel)
    {
        this.uploadChannel = uploadChannel;
        return this;
    }

    @Override
    public FileUploadFormElement withAccept(String accept)
    {
        this.accept = accept;
        return this;
    }

    @Override
    public FileUploadFormElement onUpload(ButtonEventHandler eventHandler)
    {
        this.eventHandler = (previousData, previousInternalData) -> MappedData.builder()
                                                                              .data(eventHandler.apply(previousData, this.getEffectiveContext()))
                                                                              .internalData(previousInternalData)
                                                                              .build();
        return this;
    }

    @Override
    public FileUploadFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }

}
