package com.liveqa.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * logout 用。discovery に end_session_endpoint が無いときだけブラウザ向け URL を載せる。
 * EndSessionClientRegistrationRepository
 * @author ichikura
 */
public final class EndSessionClientRegistrationRepository implements ClientRegistrationRepository {

    private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

    private final ClientRegistrationRepository delegate;
    private final String endSessionUri;

    public EndSessionClientRegistrationRepository(
            ClientRegistrationRepository delegate,
            String endSessionUri) {
        this.delegate = delegate;
        this.endSessionUri = endSessionUri;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        final ClientRegistration original = delegate.findByRegistrationId(registrationId);
        if (original == null) {
            return null;
        }
        if (original.getProviderDetails().getConfigurationMetadata().get(END_SESSION_ENDPOINT) != null) {
            return original;
        }
        final Map<String, Object> metadata = new HashMap<>(
                original.getProviderDetails().getConfigurationMetadata());
        metadata.put(END_SESSION_ENDPOINT, endSessionUri);
        return ClientRegistration.withClientRegistration(original)
                .providerConfigurationMetadata(metadata)
                .build();
    }
}
