package com.github.izaquemacielcunha.cpf.dependencyinjection;

import com.github.izaquemacielcunha.cpf.model.ExternalServiceConfig;

import dagger.Module;
import dagger.Provides;

import jakarta.inject.Singleton;

@Module
public class CredentialsConfigModule {

    @Provides
    @Singleton
    static ExternalServiceConfig provideServiceConfig() {
        String url = System.getenv("EXTERNAL_API_URL");
        String token = System.getenv("EXTERNAL_API_TOKEN");

        if (url == null || token == null) {
            return new ExternalServiceConfig("","","");
        }

        return new ExternalServiceConfig(url, token, url.replace("{token}", token));
    }

}// end of class