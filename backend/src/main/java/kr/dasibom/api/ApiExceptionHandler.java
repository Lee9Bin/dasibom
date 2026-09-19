package kr.dasibom.api;
import kr.dasibom.infrastructure.TourApiClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail status(ResponseStatusException e){return ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"요청을 처리할 수 없습니다.":e.getReason());}
    @ExceptionHandler({ConstraintViolationException.class,org.springframework.web.method.annotation.HandlerMethodValidationException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail validation(Exception e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"요청 형식을 확인해 주세요.");}
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail unavailable(IllegalStateException e){return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"데이터 연결을 준비 중입니다. 잠시 후 다시 시도해 주세요.");}
    @ExceptionHandler(TourApiClient.TourApiException.class)
    ProblemDetail upstream(TourApiClient.TourApiException e){return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"공공 API 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.");}
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e){return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"요청을 처리하지 못했습니다.");}
}
