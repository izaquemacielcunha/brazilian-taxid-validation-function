package com.github.izaquemacielcunha.cpf.dependencyinjection;

import com.github.izaquemacielcunha.cpf.service.ExternalService;
import com.github.izaquemacielcunha.cpf.service.ExternalServiceImpl;

import dagger.Binds;
import dagger.Module;

import jakarta.inject.Singleton;

@Module
public interface ServiceModule {

    @Binds
    @Singleton
    ExternalService bindExternalService(ExternalServiceImpl impl);
}// end of interface