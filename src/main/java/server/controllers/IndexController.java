package server.controllers;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;


public class IndexController {

    // Basic route controller to serve homepage
    public static void serveIndexPage() {
        get("/", (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("message", "CS 5431 YACCoin Project");
            return new ModelAndView(model, "index.ftl");
        }, new FreeMarkerEngine());
    }
}
