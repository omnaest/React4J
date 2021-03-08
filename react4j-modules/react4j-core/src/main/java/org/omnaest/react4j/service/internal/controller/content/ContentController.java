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
package org.omnaest.react4j.service.internal.controller.content;

import javax.annotation.PostConstruct;

import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ContentControllerBean
public class ContentController
{
    private static Logger LOG = LoggerFactory.getLogger(ContentController.class);

    @Autowired
    private ContentService contentService;

    @RequestMapping(method = RequestMethod.GET, path = "/images/content/{imageName}")
    public ResponseEntity<byte[]> getContentImages(@PathVariable("imageName") String imageName)
    {
        return this.contentService.findImage(imageName)
                                  .map(ContentImage::get)
                                  .map(ResponseEntity::ok)
                                  .orElseGet(() ->
                                  {
                                      LOG.warn("Unable to find context image: " + imageName);
                                      return ResponseEntity.notFound()
                                                           .build();
                                  });
    }

    @RequestMapping(method = RequestMethod.GET, path = "/content/{attachmentName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getContentAttachment(@PathVariable("attachmentName") String attachmentName)
    {
        return this.contentService.findAttachment(attachmentName)
                                  .map(attachment ->
                                  {
                                      byte[] data = attachment.get();
                                      ByteArrayResource resource = new ByteArrayResource(data);
                                      return ResponseEntity.ok()
                                                           .headers(headers -> headers.add(HttpHeaders.CONTENT_DISPOSITION,
                                                                                           "attachment; filename=" + attachment.getAttachmentName()))
                                                           .contentLength(data.length)
                                                           .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                           .body((Resource) resource);
                                  })
                                  .orElseGet(() ->
                                  {
                                      LOG.warn("Unable to find context attachment: " + attachmentName);
                                      return ResponseEntity.notFound()
                                                           .build();
                                  });
    }

    @PostConstruct
    public void postInit()
    {
        LOG.info(this.getClass()
                     .getSimpleName()
                + " enabled.");
    }
}
