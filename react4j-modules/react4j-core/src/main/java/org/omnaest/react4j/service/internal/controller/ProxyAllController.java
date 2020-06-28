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
