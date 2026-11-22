package cl.duocuc.ep03.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Agrega un identificador de correlacion a cada peticion HTTP.
 *
 * <p>El ID queda en el MDC, por lo que TODAS las lineas de log emitidas
 * durante la peticion lo incluyen (trazabilidad detallada en CloudWatch
 * Logs cuando Fluent Bit recolecta la salida JSON). Tambien se devuelve
 * en la respuesta como cabecera {@code X-Request-Id}, de modo que el
 * frontend u otros clientes puedan referenciar el mismo ID al reportar
 * incidencias.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_PATH = "path";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_PATH, request.getRequestURI());
            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_PATH);
        }
    }
}
