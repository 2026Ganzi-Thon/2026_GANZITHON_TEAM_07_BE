package com.ganzithon.backend.health.application.service;

import com.ganzithon.backend.health.application.port.in.CheckHealthUseCase;
import com.ganzithon.backend.health.application.port.in.HealthStatus;

public final class HealthService implements CheckHealthUseCase {

    @Override
    public HealthStatus check() {
        return HealthStatus.UP;
    }
}
