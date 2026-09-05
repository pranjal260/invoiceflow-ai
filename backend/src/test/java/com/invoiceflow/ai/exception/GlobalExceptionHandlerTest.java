package com.invoiceflow.ai.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void mapsUserNotFoundToNotFound() {
        assertStatus(exceptionHandler.handleNotFound(new UserNotFoundException(UUID.randomUUID()), request),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsCategoryNotFoundToNotFound() {
        assertStatus(exceptionHandler.handleNotFound(new CategoryNotFoundException(UUID.randomUUID()), request),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsInvoiceNotFoundToNotFound() {
        assertStatus(exceptionHandler.handleNotFound(new InvoiceNotFoundException(UUID.randomUUID()), request),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsInvoiceItemNotFoundToNotFound() {
        assertStatus(exceptionHandler.handleNotFound(new InvoiceItemNotFoundException(UUID.randomUUID()), request),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsEmailAlreadyExistsToConflict() {
        assertStatus(exceptionHandler.handleConflict(new EmailAlreadyExistsException("ada@example.com"), request),
                HttpStatus.CONFLICT);
    }

    @Test
    void mapsDuplicateCategoryNameToConflict() {
        assertStatus(exceptionHandler.handleConflict(new DuplicateCategoryNameException("Software"), request),
                HttpStatus.CONFLICT);
    }

    @Test
    void mapsDuplicateInvoiceNumberToConflict() {
        assertStatus(exceptionHandler.handleConflict(new DuplicateInvoiceNumberException("INV-001"), request),
                HttpStatus.CONFLICT);
    }

    @Test
    void mapsValidationFailureToBadRequest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidation(exception, request);

        assertStatus(response, HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("email: must be a well-formed email address");
    }

    @Test
    void mapsUnexpectedExceptionToInternalServerErrorWithoutLeakingDetails() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnexpected(
                new IllegalStateException("internal database details"), request);

        assertStatus(response, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
    }

    private static void assertStatus(ResponseEntity<ApiErrorResponse> response, HttpStatus expectedStatus) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().error()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
