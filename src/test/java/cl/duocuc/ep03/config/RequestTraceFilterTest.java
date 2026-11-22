package cl.duocuc.ep03.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests del filtro de trazabilidad: cubre todas las ramas de la decision
 * de generacion de request-id (sin cabecera, con cabecera, cabecera vacia).
 *
 * <p>El filtro limpia el MDC al terminar (finally), por lo que el valor
 * del MDC se captura DENTRO de la cadena mediante un AtomicReference.
 */
class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void generaRequestIdCuandoNoVieneEnLaCabecera() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ep03");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcVisto = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> mdcVisto.set(MDC.get("requestId")));

        String generado = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertNotNull(generado);
        UUID.fromString(generado);
        assertEquals(generado, mdcVisto.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void respetaElRequestIdDeLaCabecera() throws Exception {
        String esperado = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ep03");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, esperado);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcVisto = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> mdcVisto.set(MDC.get("requestId")));

        assertEquals(esperado, response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER));
        assertEquals(esperado, mdcVisto.get());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void regeneraElRequestIdSiLaCabeceraVieneVacia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ep03");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcVisto = new AtomicReference<>();

        filter.doFilterInternal(request, response, (req, res) -> mdcVisto.set(MDC.get("requestId")));

        String generado = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertNotNull(generado);
        assertFalse(generado.isBlank());
        assertEquals(UUID.fromString(generado).toString(), generado);
        assertEquals(generado, mdcVisto.get());
    }

    @Test
    void dejaElMdcRegistradoDuranteLaPeticionYLoLimpiaAlFinal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/ep03/1");
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "id-fijo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals("id-fijo", MDC.get("requestId"));
            assertEquals("/ep03/1", MDC.get("path"));
        });

        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("path"));
    }
}
