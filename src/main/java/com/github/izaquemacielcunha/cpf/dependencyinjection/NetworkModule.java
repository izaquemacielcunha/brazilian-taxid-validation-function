package com.github.izaquemacielcunha.cpf.dependencyinjection;

import java.net.http.HttpClient;
import java.time.Duration;

import dagger.Module;
import dagger.Provides;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.inject.Singleton;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    public HttpClient provideHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .build();
    }
}// end of class
