package com.ganzithon.backend.auth.application.port.out;

import java.util.function.Supplier;

public interface AuthTransaction {
    <T> T required(Supplier<T> work);
}
