package io.engineblock.clients;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;

import java.util.List;

public class EBClient {


    public enum Path {
        activitytypes
    }

    public EBClient(String baseUrl) {
        this.endpoints = new Endpoints(baseUrl);
        Unirest.setObjectMapper(getObjectMapper());
    }

    public List<String> getActivityTypes() {
        return Unirest.get(pathOf(Path.activitytypes))
                .asObject(List.class);

    }

    private ObjectMapper getObjectMapper() {
        Gson gson = new GSON();

        return new ObjectMapper() {
            @Override
            public <T> T readValue(String s, Class<T> aClass) {
                return gson.fromJson(s, aClass);
            }

            @Override
            public String writeValue(Object o) {
                return gson.toJson(o);
            }
        };
    }

    private String pathOf(Path paths) {
        return baseUrl + "/" + paths.name();
    }
}
