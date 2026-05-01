package br.com.bssantos.rag.handler;

import br.com.bssantos.rag.exception.DocumentoInvalidoException;
import br.com.bssantos.rag.exception.DocumentoNaoEncontradoException;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.exception.IdInvalidoException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DocumentoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handlerDocumentoNaoEncontrado(DocumentoNaoEncontradoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(404),
                ex.getMessage()
        );
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<ProblemDetail> handlerDocumentoInvalido(DocumentoInvalidoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(400),
                ex.getMessage()
        );
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(IdInvalidoException.class)
    public ResponseEntity<ProblemDetail> handlerIdInvalido(IdInvalidoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(400),
                ex.getMessage()
        );
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(FalhaNoProcessamentoException.class)
    public ResponseEntity<ProblemDetail> handlerFalhaNoProcessamento(FalhaNoProcessamentoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(500),
                ex.getMessage()
        );
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }
}
