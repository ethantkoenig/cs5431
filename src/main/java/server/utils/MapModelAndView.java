package server.utils;

import spark.ModelAndView;

import java.util.HashMap;
import java.util.Map;

public final class MapModelAndView {
    private final String viewName;
    private final Map<String, Object> map = new HashMap<>();

    public MapModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public MapModelAndView add(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public final ModelAndView get() {
        return new ModelAndView(map, viewName);
    }
}
