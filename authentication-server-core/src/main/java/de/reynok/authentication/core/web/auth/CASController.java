package de.reynok.authentication.core.web.auth;

import de.reynok.authentication.core.Constants;
import de.reynok.authentication.core.api.exception.AccessDeniedException;
import de.reynok.authentication.core.api.exception.ServiceException;
import de.reynok.authentication.core.api.exception.UnknownServiceException;
import de.reynok.authentication.core.api.models.Identity;
import de.reynok.authentication.core.api.models.Service;
import de.reynok.authentication.core.api.service.LoginRequest;
import de.reynok.authentication.core.api.service.LoginResponse;
import de.reynok.authentication.core.conf.CASConfiguration;
import de.reynok.authentication.core.logic.cas.*;
import de.reynok.authentication.core.logic.database.repository.IdentityRepository;
import de.reynok.authentication.core.util.JwtProcessor;
import de.reynok.authentication.core.util.validation.OneTimePasswordValidator;
import de.reynok.authentication.core.web.RequestProcessedController;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
public class CASController extends RequestProcessedController {
    private final TicketHandler      ticketHandler;
    private final JwtProcessor       jwtProcessor;
    private final IdentityRepository identityRepository;
    private final ServiceValidation  serviceValidation;
    private final CASConfiguration   casConfiguration;

    @Autowired
    public CASController(TicketHandler ticketHandler, JwtProcessor jwtProcessor, IdentityRepository identityRepository, ServiceValidation serviceValidation, CASConfiguration configuration) {
        super(identityRepository);
        this.jwtProcessor = jwtProcessor;
        this.identityRepository = identityRepository;
        this.serviceValidation = serviceValidation;
        this.ticketHandler = ticketHandler;
        this.casConfiguration = configuration;
    }

    @GetMapping("/cas/logout")
    public void logout(HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie("CASTGC", "");
        cookie.setMaxAge(1);
        cookie.setPath(casConfiguration.getCookiePath());
        cookie.setComment(casConfiguration.getCookieComment());
        cookie.setDomain(casConfiguration.getCookieDomain());

        response.addCookie(cookie);
        response.sendRedirect(casConfiguration.getSystemDomain() + "/");
        response.setStatus(302);
    }

    @CrossOrigin
    @GetMapping("/cas/login")
    public void requestLogin(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "service") String serviceUrl) throws IOException {
        Service service = serviceValidation.isAllowed(serviceUrl);

        if (service == null) {
            response.sendRedirect(getRDUrl("error", request, CasStatusCode.MISSING_SERVICE));
            response.setStatus(302);
        }

        if (request.getAttribute(Constants.REQUEST_CLAIMS_FIELD) != null) {
            Claims claims = (Claims) request.getAttribute(Constants.REQUEST_CLAIMS_FIELD);

            Identity identity = identityRepository.findByUsername(claims.get("sub").toString()).orElseThrow(EntityNotFoundException::new);

            if (service.isIdentityAllowed(identity)) {
                String redirectUrl = getRedirectLogin(serviceUrl, identity);

                LoginResponse loginResponse = new LoginResponse();
                loginResponse.setLocation(redirectUrl);

                response.sendRedirect(redirectUrl);
                response.setStatus(302);
            } else {
                response.sendRedirect(getRDUrl("error", request, CasStatusCode.DENIED));
                response.setStatus(302);
            }
        } else {
            response.sendRedirect(getRDUrl("login", request));
            response.setStatus(302);
        }
    }

    @CrossOrigin
    @PostMapping("/cas/login")
    public ResponseEntity<LoginResponse> processAuthentication(HttpServletResponse response, @RequestBody LoginRequest body, @RequestParam("service") String serviceUrl) throws ServiceException {
        Service service = serviceValidation.isAllowed(serviceUrl);

        log.info("Authentication Request for {} (from={}) as User {} (CAS: {}).", service, serviceUrl, body.getUsername(), body.getCas());

        if (service == null) {
            throw new UnknownServiceException("No Service found for " + serviceUrl);
        }

        Optional<Identity> optionalIdentity = identityRepository.findByUsername(body.getUsername());

        if (optionalIdentity.isPresent()) {
            Identity identity = optionalIdentity.get();

            if (identity.checkPassword(body.getPassword())) {
                if (identity.getOtpSecret() != null && identity.getOtpSecret().length() > 0) {
                    if (body.getSecurityPassword() != null) {
                        OneTimePasswordValidator validator = new OneTimePasswordValidator(identity.getOtpSecret());

                        if (!validator.isValid(body.getSecurityPassword())) {
                            throw new AccessDeniedException("Username or Password does not match.").setCode(409);
                        }
                    } else {
                        throw new AccessDeniedException(null).setCode(409);
                    }
                }

                Cookie cookie = new Cookie(Constants.COOKIE_NAME, jwtProcessor.getJwtTokenFor(identity, service));
                cookie.setMaxAge(casConfiguration.getCookieMaxAge());
                cookie.setPath(casConfiguration.getCookiePath());
                cookie.setComment(casConfiguration.getCookieComment());

                if (casConfiguration.getCookieDomain() != null) {
                    cookie.setDomain(casConfiguration.getCookieDomain());
                }

                response.addCookie(cookie);

                LoginResponse loginResponse = new LoginResponse();

                if (body.getCas()) {
                    loginResponse.setLocation(getRedirectLogin(serviceUrl, identity)); // issues a new ticket
                } else {
                    loginResponse.setLocation(serviceUrl);
                }

                return ResponseEntity.ok(loginResponse);
            }
        }

        return ResponseEntity.status(403).build();
    }

    @GetMapping(value = {"/cas/validate", "/cas/p3/serviceValidate", "/cas/serviceValidate", "/cas/proxyValidate", "/cas/p3/proxyValidate"}, produces = "application/json")
    @ApiOperation(response = Map.class, value = "Validates a Service Ticket", notes = "**ATTENTION**: Proxy-Tickets cannot be validated!", produces = "application/json")
    public ResponseEntity validateAsJson(HttpServletResponse response, @RequestParam("ticket") String ticket, @RequestParam("service") String service) {
        CasJsonResponse jsonResponse = new CasJsonResponse();

        Identity identity = ticketHandler.getTicketData(ticket, service);

        if (identity != null) {
            jsonResponse.success(identity);
        } else {
            jsonResponse.error(CasStatusCode.INVALID_TICKET, "Ticket " + ticket + " not recognized.");
        }

        return jsonResponse.toResponse();
    }

    /**
     * ATTENTION: /proxyValidate is ONLY validating the normal service tickets, proxyTickets are NOT yet supported!
     */
    @GetMapping(value = {"/cas/validate", "/cas/p3/serviceValidate", "/cas/serviceValidate", "/cas/proxyValidate", "/cas/p3/proxyValidate"})
    @ApiOperation(response = Map.class, value = "Validates a Service Ticket", notes = "**ATTENTION**: Proxy-Tickets cannot be validated!", produces = "application/xml")
    public ResponseEntity validate(HttpServletResponse response, @RequestParam("ticket") String ticket, @RequestParam("service") String service) {
        CasXmlResponse xmlResponse = new CasXmlResponse();

        Identity identity = ticketHandler.getTicketData(ticket, service);

        if (identity != null) {
            xmlResponse.isSuccess(identity);
        } else {
            xmlResponse.isFailure(CasStatusCode.INVALID_TICKET, "Ticket " + ticket + " not recognized.");
        }

        response.setContentType("application/xml");
        return ResponseEntity.ok(xmlResponse.toString());
    }


    private String getRDUrl(String type, HttpServletRequest request, CasStatusCode statusCode) {
        return getRDUrl(type, request) + "&code=" + statusCode;
    }

    private String getRDUrl(String type, HttpServletRequest request) {
        return casConfiguration.getSystemDomain() + "/#/cas/" + type + "?service=" + request.getParameter("service");
    }

    private String getRedirectLogin(String service, Identity identity) {
        String serviceUrl = service;

        if (service.contains("?")) { service += "&"; } else { service += "?"; }
        service += "ticket=" + ticketHandler.generateTicketFor(TicketType.ST, serviceUrl, identity);

        return service;
    }
}