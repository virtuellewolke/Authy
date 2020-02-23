package io.virtuellewolke.authentication.core.spring.security;

import io.virtuellewolke.authentication.core.database.entity.Identity;
import io.virtuellewolke.authentication.core.database.repository.IdentityRepository;
import io.virtuellewolke.authentication.core.spring.components.ServiceValidation;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class ApiTokenInterceptor extends ServiceAwareInterceptor implements SecureContextInterceptor {

    private final IdentityRepository identityRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_TOKEN_HEADER     = "X-Api-Token";

    public ApiTokenInterceptor(IdentityRepository identityRepository, ServiceValidation serviceValidation) {
        super(serviceValidation);
        this.identityRepository = identityRepository;
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!SecureContext.hasSecureContext(request)) {
            String token = getApiTokenFromHeaders(request);

            Optional<Identity> identity = identityRepository.findByApiToken(token);

            if (identity.isPresent()) {
                SecureContext context = SecureContext.builder()
                        .identity(identity.get())
                        .service(getService(request))
                        .source(SecureContext.Source.API_TOKEN)
                        .build();

                SecureContext.setSecureContext(context, request);
            }
        }

        return true;
    }

    private String getApiTokenFromHeaders(HttpServletRequest request) {
        String token = null;
        if (request.getHeader(AUTHORIZATION_HEADER) != null) {
            token = request.getHeader(AUTHORIZATION_HEADER);

            if (token.startsWith("Bearer ")) {
                token = token.substring("Bearer ".length());
            }
        } else if (request.getHeader(API_TOKEN_HEADER) != null) {
            token = request.getHeader(API_TOKEN_HEADER);
        }

        return token != null ? token.trim() : null;
    }
}