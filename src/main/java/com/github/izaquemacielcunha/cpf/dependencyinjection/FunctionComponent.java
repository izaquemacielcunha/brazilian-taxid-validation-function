package com.github.izaquemacielcunha.cpf.dependencyinjection;

import com.github.izaquemacielcunha.cpf.function.ValidateCpf;

import dagger.Component;

import jakarta.inject.Singleton;

@Singleton
@Component(modules = {
        ServiceModule.class,
        NetworkModule.class,
        CredentialsConfigModule.class
})
public interface FunctionComponent {
    ValidateCpf getValidateCpf();
}// end of interface