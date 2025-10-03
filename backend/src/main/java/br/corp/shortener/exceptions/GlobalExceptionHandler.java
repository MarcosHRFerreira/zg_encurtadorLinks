package br.corp.shortener.exceptions;

import br.corp.shortener.dto.ErrorResponse;
import br.corp.shortener.dto.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCodeException(DuplicateCodeException ex) {
        ErrorResponse response = new ErrorResponse("Código já está em uso", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse("Erro de validação", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
        ErrorResponse response = new ErrorResponse("URL não encontrada", "O recurso solicitado não existe");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponse response = new ErrorResponse("URL não encontrada", "O recurso solicitado não existe");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String method = ex.getMethod();
        String supported = (ex.getSupportedMethods() != null && ex.getSupportedMethods().length > 0)
                ? String.join(", ", ex.getSupportedMethods())
                : "";
        String message = supported.isEmpty()
                ? "Método não permitido para este recurso"
                : "Método '" + method + "' não permitido. Suportado: " + supported;
        ErrorResponse response = new ErrorResponse("Método não permitido", message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Fallback: se for um NoResourceFound/NoHandlerFound não capturado pelas handlers específicas
        if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
            ErrorResponse response = new ErrorResponse("URL não encontrada", "O recurso solicitado não existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        ErrorResponse response = new ErrorResponse("Erro interno do servidor", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}