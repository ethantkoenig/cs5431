package server.controllers;

import server.utils.RouteUtils;
import spark.template.freemarker.FreeMarkerEngine;

import static spark.Spark.get;


public class IndexController {

    // Basic route controller to serve homepage
    public static void serveIndexPage() {
        get("/", (request, response) ->
                        RouteUtils.modelAndView(request, "index.ftl")
                                .add("message", "CS 5431 YACCoin Proejct")
                                .get()
                , new FreeMarkerEngine());
    }
}
