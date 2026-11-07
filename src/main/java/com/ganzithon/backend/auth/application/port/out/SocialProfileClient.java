package com.ganzithon.backend.auth.application.port.out;

import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.SocialProfile;

public interface SocialProfileClient {
    SocialProfile exchange(AuthProvider provider, String authorizationCode, String redirectUri);
}
