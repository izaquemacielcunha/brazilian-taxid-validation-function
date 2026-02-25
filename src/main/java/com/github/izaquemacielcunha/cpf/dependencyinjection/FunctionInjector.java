package com.github.izaquemacielcunha.cpf.dependencyinjection;

import com.github.izaquemacielcunha.cpf.function.ValidateCpf;

import com.microsoft.azure.functions.spi.inject.FunctionInstanceInjector;

public class FunctionInjector implements FunctionInstanceInjector {
    private final FunctionComponent component;

    public FunctionInjector() {
        this.component = DaggerFunctionComponent.create();
    }

    @Override
    public <T> T getInstance(Class<T> functionClass) throws Exception {
        if (functionClass == ValidateCpf.class) {
            return (T) component.getValidateCpf();
        }
        throw new IllegalArgumentException("Function not mapped in Dagger: " + functionClass);
    }

}// end of class