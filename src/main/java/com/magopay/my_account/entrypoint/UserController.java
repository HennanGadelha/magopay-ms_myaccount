package com.magopay.my_account.entrypoint;

import com.magopay.my_account.entrypoint.dto.FindUserByEmailResponse;
import com.magopay.my_account.entrypoint.dto.RegisterUserRequest;
import com.magopay.my_account.entrypoint.dto.RegisterUserResponse;
import com.magopay.my_account.entrypoint.mapper.UserWebMapper;
import com.magopay.my_account.core.application.ports.in.FindUserByEmailUseCase;
import com.magopay.my_account.core.application.ports.in.RegisterUserUseCase;
import com.magopay.my_account.core.application.ports.in.command.RegisterUserCommand;
import com.magopay.my_account.core.application.ports.in.query.FindUserByEmailQuery;
import com.magopay.my_account.core.application.ports.in.result.FindUserByEmailResult;
import com.magopay.my_account.core.application.ports.in.result.RegisterUserResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final RegisterUserUseCase registerUserUseCase;
    private final FindUserByEmailUseCase findUserByEmailUseCase;
    private final UserWebMapper userWebMapper;

    public UserController(RegisterUserUseCase registerUserUseCase,
                          FindUserByEmailUseCase findUserByEmailUseCase,
                          UserWebMapper userWebMapper) {
        this.registerUserUseCase = registerUserUseCase;
        this.findUserByEmailUseCase = findUserByEmailUseCase;
        this.userWebMapper = userWebMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(
            @RequestBody RegisterUserRequest request,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationIdHeader
    ) {
        String correlationId = resolveCorrelationId(correlationIdHeader);
        MDC.put(CORRELATION_MDC_KEY, correlationId);

        String maskedEmail = maskEmail(request.email());
        String maskedDocument = maskDocument(request.document());

        LOGGER.info(
                "event=user.register.controller.started correlationId={} emailMasked={} documentMasked={}",
                correlationId,
                maskedEmail,
                maskedDocument
        );

        try {
            RegisterUserCommand command = userWebMapper.toCommand(request);
            RegisterUserResult result = registerUserUseCase.execute(command);
            RegisterUserResponse response = userWebMapper.toResponse(result);

            Link selfLink = linkTo(methodOn(UserController.class).register(request, correlationId))
                    .withSelfRel();
            response.add(selfLink);

            LOGGER.info(
                    "event=user.register.controller.completed correlationId={} userId={} statusCode={}",
                    correlationId,
                    result.id(),
                    HttpStatus.CREATED.value()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(CORRELATION_HEADER, correlationId)
                    .body(response);
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "event=user.register.controller.failed correlationId={} emailMasked={} documentMasked={} reason={}",
                    correlationId,
                    maskedEmail,
                    maskedDocument,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        } finally {
            MDC.remove(CORRELATION_MDC_KEY);
        }
    }

    private String resolveCorrelationId(String correlationIdHeader) {
        if (correlationIdHeader == null || correlationIdHeader.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationIdHeader.trim();
    }

    @GetMapping("/by-email")
    public ResponseEntity<FindUserByEmailResponse> findByEmail(
            @RequestParam("email") String email,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlationIdHeader
    ) {
        String correlationId = resolveCorrelationId(correlationIdHeader);
        MDC.put(CORRELATION_MDC_KEY, correlationId);

        String maskedEmail = maskEmail(email);

        LOGGER.info(
                "event=user.findByEmail.controller.started correlationId={} emailMasked={}",
                correlationId,
                maskedEmail
        );

        try {
            FindUserByEmailQuery query = new FindUserByEmailQuery(email);
            FindUserByEmailResult result = findUserByEmailUseCase.execute(query);
            FindUserByEmailResponse response = userWebMapper.toFindByEmailResponse(result);

            Link selfLink = linkTo(methodOn(UserController.class).findByEmail(email, correlationId))
                    .withSelfRel();
            response.add(selfLink);

            LOGGER.info(
                    "event=user.findByEmail.controller.completed correlationId={} userId={} statusCode={}",
                    correlationId,
                    result.id(),
                    HttpStatus.OK.value()
            );

            return ResponseEntity
                    .ok()
                    .header(CORRELATION_HEADER, correlationId)
                    .body(response);
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "event=user.findByEmail.controller.failed correlationId={} emailMasked={} reason={}",
                    correlationId,
                    maskedEmail,
                    ex.getMessage(),
                    ex
            );
            throw ex;
        } finally {
            MDC.remove(CORRELATION_MDC_KEY);
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }

        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 1 || atIndex == normalized.length() - 1) {
            return "***";
        }

        return normalized.substring(0, 2) + "***" + normalized.substring(atIndex);
    }

    private String maskDocument(String document) {
        if (document == null || document.isBlank()) {
            return "<empty>";
        }

        String normalized = document.trim();
        int visible = Math.min(4, normalized.length());
        return "***" + normalized.substring(normalized.length() - visible);
    }
}
