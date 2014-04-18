package market.exception;

import java.util.Arrays;
import market.exception.dto.ValidationErrorDTO;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Обработчик исключений.
 */
@ControllerAdvice
public class SpringExceptionHandler {

    private final MessageSource messageSource;

    @Autowired
    public SpringExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    //-------------------------------------------------- Обработчики исключений
    
    /**
     * @param ex исключение
     */
    @ExceptionHandler(RestNotAuthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void handleNotAuthenticatedException(RestNotAuthenticatedException ex) {
        //
    }
    
    /**
     * Попытка сослаться на несуществующую страну.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(EmailExistsException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ValidationErrorDTO handleEmailExistsException(EmailExistsException ex) {
        List<FieldError> fieldErrors = Arrays.asList(ex.getFieldError());
        return processFieldErrors(fieldErrors);
    }
    
    /**
     * Попытка сослаться на несуществующую страну.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(EmptyCartException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ValidationErrorDTO handleEmptyCartException(EmptyCartException ex) {
        List<FieldError> fieldErrors = Arrays.asList(ex.getFieldError());
        return processFieldErrors(fieldErrors);
    }
    
    /**
     * Попытка сослаться на несуществующую страну.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(UnknownProductException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ValidationErrorDTO handleUnknownProductException(UnknownProductException ex) {
        List<FieldError> fieldErrors = Arrays.asList(ex.getFieldError());
        return processFieldErrors(fieldErrors);
    }
    
    /**
     * Попытка сослаться на несуществующую страну.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleProductNotFoundException(ProductNotFoundException ex) {
        return ex.getMessage();
    }
    
    /**
     * Попытка сослаться на несуществующую страну.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleOrderNotFoundException(OrderNotFoundException ex) {
        return ex.getMessage();
    }
    
    /**
     * Ошибки валидации полученного от клиента объекта.
     * @param ex исключение
     * @return перечень нарушенных ограничений
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ValidationErrorDTO processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();

        return processFieldErrors(fieldErrors);
    }
    
    //----------------------------------------- Компоновка сообщений об ошибках
    
    private ValidationErrorDTO processFieldErrors(List<FieldError> fieldErrors) {
        ValidationErrorDTO dto = new ValidationErrorDTO();
        for (FieldError fieldError: fieldErrors) {
            String localizedErrorMessage = resolveErrorMessage(fieldError);
            dto.addFieldError(fieldError.getField(), localizedErrorMessage);
        }
        return dto;
    }

    private String resolveErrorMessage(FieldError fieldError) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        String localizedErrorMessage = messageSource.getMessage(fieldError, currentLocale);

        // Если подходящего сообщения не найдено - попытаться найти ближайшее по коду ошибки
        if (localizedErrorMessage.equals(fieldError.getDefaultMessage())) {
            String[] fieldErrorCodes = fieldError.getCodes();
            localizedErrorMessage = fieldErrorCodes[0];
        }
        return localizedErrorMessage;
    }
}
