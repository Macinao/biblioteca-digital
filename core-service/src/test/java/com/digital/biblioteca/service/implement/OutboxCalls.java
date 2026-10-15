package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import org.mockito.Mockito;

import java.util.List;

/** Le, na ordem, tudo que um service pediu pra gravar no outbox (mock de OutboxService). */
final class OutboxCalls {

    record Evento(String aggregateType, String aggregateId, String eventType,
                  Object payload, Broker broker, String destination) {
    }

    private OutboxCalls() {
    }

    static List<Evento> de(OutboxService outbox) {
        return Mockito.mockingDetails(outbox).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("record"))
                .map(i -> {
                    Object[] a = i.getArguments();
                    return new Evento((String) a[0], (String) a[1], (String) a[2], a[3], (Broker) a[4], (String) a[5]);
                })
                .toList();
    }

    static List<String> tipos(OutboxService outbox) {
        return de(outbox).stream().map(Evento::eventType).toList();
    }
}
