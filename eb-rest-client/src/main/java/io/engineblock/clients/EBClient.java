package io.engineblock.clients;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class EBClient {

    private String baseUrl;


    public EBClient(String baseUrl) {
        Unirest.setObjectMapper(getObjectMapper());
        this.baseUrl = baseUrl;
    }

    public HttpResponse<JsonNode> getActivityTypes() throws UnirestException {
        return Unirest.get(pathOf(Path.activitytypes))
                .asJson();
    }

    private ObjectMapper getObjectMapper() {
        Gson gson = new Gson();

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

    public enum Path {
        activitytypes
    }
}
