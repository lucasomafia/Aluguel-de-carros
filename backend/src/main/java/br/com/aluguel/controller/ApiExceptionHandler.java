package br.com.aluguel.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Verifique os campos informados.");
        List<String> campos = excecao.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).toList();
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflito() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Cadastro em conflito com um registro existente."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> pedidoAlterado() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Este pedido foi alterado. Atualize a lista e tente novamente."));
    }
}
