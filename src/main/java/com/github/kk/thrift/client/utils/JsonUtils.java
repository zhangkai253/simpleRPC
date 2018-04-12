package com.github.kk.thrift.client.utils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * @author zhangkai
 *
 */
public class JsonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private static JsonParser JSONPARSER = new JsonParser();

    private static Gson GSON;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithModifiers(Modifier.STATIC);
        gsonBuilder.serializeNulls();
        gsonBuilder.disableHtmlEscaping();
        gsonBuilder.setPrettyPrinting();
        GSON = gsonBuilder.create();
    }

    private JsonUtils() {

    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static boolean isNull(JsonElement obj) {
        return obj != null && !obj.isJsonNull();
    }

    public static JsonElement parse(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONPARSER.parse(jsonStr);
        } catch (JsonSyntaxException e) {
            LOG.warn("Failed to parse json result,jsonStr=" + jsonStr, e);
        }
        return null;
    }

    public static JsonObject parseToJsonObject(String jsonStr) {
        JsonObject obj = null;
        try {
            JsonElement ele = parse(jsonStr);
            if (ele != null) {
                obj = ele.getAsJsonObject();
            }
        } catch (IllegalStateException e) {
            LOG.warn("Failed to parse json object,jsonStr=" + jsonStr, e);
        }
        return obj;
    }

    public static List<JsonObject> parseToJsonObjects(String jsonStr) {
        List<JsonObject> objs = new ArrayList<JsonObject>();
        if (jsonStr == null) {
            return objs;
        }
        JsonArray array = parseToJsonArray(jsonStr);
        for (int i = 0; i < array.size(); i++) {
            objs.add(array.get(i).getAsJsonObject());
        }
        return objs;
    }

    public static JsonArray parseToJsonArray(String jsonStr) {
        JsonArray array = null;
        try {
            JsonElement ele = parse(jsonStr);
            if (ele != null) {
                array = ele.getAsJsonArray();
            }
        } catch (IllegalStateException e) {
            LOG.warn("Failed to parse json array,jsonStr=" + jsonStr, e);
        }
        return array;
    }

    public static <T> T fromJson(String jsonStr, Class<T> clazz) {
        return GSON.fromJson(jsonStr, clazz);
    }

    public static <T> T fromJson(JsonObject obj, Class<T> clazz) {
        if (obj == null || obj.isJsonNull()) {
            return null;
        }
        return GSON.fromJson(obj, clazz);
    }

    public static <T> List<T> fromJsonArray(JsonArray array, Class<T> clazz) {
        List<T> datas = new ArrayList<T>();
        for (int i = 0, size = array.size(); i < size; i++) {
            T t = GSON.fromJson(array.get(i), clazz);
            if (t != null) {
                datas.add(t);
            }
        }
        return datas;
    }

    public static <T> List<T> fromJsonArray(String json, Class<T> clazz) {
        JsonArray array = parseToJsonArray(json);
        return fromJsonArray(array, clazz);
    }

    public static String getValueByPath(JsonElement obj, String path) {
        JsonElement e = getJsonElementByPath(obj, path);
        if (e == null || e.isJsonNull()) {
            return "";
        }
        return e.getAsString();
    }

    public static JsonElement getJsonElementByPath(JsonElement obj,
        String path) {
        if (obj == null || obj.isJsonNull()) {
            return obj;
        }
        String[] paths = path.split("\\.");
        if (paths.length == 0) {
            return obj;
        }
        JsonElement e = _getRecurice(paths, 0, obj.getAsJsonObject());
        return e;
    }

    public static JsonObject getJsonObjectByPath(JsonElement obj, String path) {
        JsonElement ele = getJsonElementByPath(obj, path);
        if (ele == null || ele.isJsonNull()) {
            return null;
        }
        return ele.getAsJsonObject();
    }

    private static JsonElement _getRecurice(String[] paths, int i,
        JsonObject obj) {
        if (obj == null || obj.isJsonNull() || paths.length <= i) {
            return null;
        }
        String p = paths[i];
        if (i == paths.length - 1) {
            return obj.get(p);
        }
        JsonElement ele = obj.get(p);
        if (ele == null || !ele.isJsonObject()) {
            return null;
        }
        return _getRecurice(paths, i + 1, ele.getAsJsonObject());
    }
}
