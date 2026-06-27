package com.example.feihualinggame.utils;

import com.google.gson.Gson;

public class GsonUtil {
    private static final Gson gson = new Gson();

    public static Gson getGson() {
        return gson;
    }
}