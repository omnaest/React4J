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
package org.omnaest.react4j.service.internal.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

@Profile("mock")
@RestController
public class ProxyAllController
{

    //    @RequestMapping(method = RequestMethod.GET, path = "/**")
    //    public void getAll(HttpServletRequest request, HttpServletResponse response)
    //    {
    //        String pathInfo = request.getServletPath();
    //        String body = RestClient.newStringRestClient()
    //                                .requestGet("http://localhost:3000" + pathInfo, String.class);
    //
    //        String contentType = StringUtils.split(request.getHeader("Accept"), ",")[0];
    //        response.addHeader("Content-Type", contentType);
    //
    //        try
    //        {
    //            PrintWriter writer = response.getWriter();
    //            writer.append(body);
    //            writer.close();
    //        }
    //        catch (IOException e)
    //        {
    //            throw new IllegalStateException(e);
    //        }
    //    }
}
