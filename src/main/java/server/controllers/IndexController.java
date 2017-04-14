package server.controllers;

import com.google.inject.Inject;
import server.utils.RouteUtils;
import spark.template.freemarker.FreeMarkerEngine;

import static spark.Spark.get;


public class IndexController {

    private final RouteUtils routeUtils;

    @Inject
    private IndexController(RouteUtils routeUtils) {
        this.routeUtils = routeUtils;
    }

    // Basic route controller to serve homepage
    public void init() {
        get("/", (request, response) ->
                        routeUtils.modelAndView(request, "index.ftl")
                                .add("message", "CS 5431 YACCoin Project")
                                .get()
                , new FreeMarkerEngine());
    }
}
