package org.restheart.handlers;

import com.google.common.annotations.VisibleForTesting;

import com.codahale.metrics.MetricRegistry;

import org.restheart.Bootstrapper;
import org.restheart.Configuration;
import org.restheart.db.DbsDAO;
import org.restheart.utils.SharedMetricRegistryProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HexConverter;

import static org.restheart.Configuration.METRICS_GATHERING_LEVEL.COLLECTION;
import static org.restheart.Configuration.METRICS_GATHERING_LEVEL.DATABASE;
import static org.restheart.Configuration.METRICS_GATHERING_LEVEL.ROOT;

/**
 * Handler to measure calls to restheart. Will only take actions if metrics
 * should be gathered (config option "metrics-gathering-level" > OFF)
 */
public class TracingInstrumentationHandler extends PipedHttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingInstrumentationHandler.class);
    public static final String X_B3_TRACEID = "x-b3-traceid";
    public static final String X_B3_SPANID = "x-b3-spanid";
    public static final String X_B3_PARENTSPANID = "x-b3-parentspanid";
    public static final String X_B3_SAMPLED = "x-b3-sampled";

    public TracingInstrumentationHandler(PipedHttpHandler next) {
        super(next);
    }

    @VisibleForTesting
    static String generateRandomTraceId() {
        byte[] data = new byte[8];
        new Random().nextBytes(data);
        return HexConverter.convertToHexString(data);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        //header definitions can be found at https://github.com/openzipkin/b3-propagation
        String traceID = Optional.ofNullable(exchange.getRequestHeaders().get(X_B3_TRACEID))
            .map(HeaderValues::peekFirst)
            .orElse(generateRandomTraceId());
        MDC.put(X_B3_TRACEID, traceID);
        String spanID = Optional.ofNullable(exchange.getRequestHeaders().get(X_B3_SPANID))
            .map(HeaderValues::peekFirst)
            .orElse(generateRandomTraceId());
        MDC.put(X_B3_SPANID, spanID);
        String parentSpanID = Optional.ofNullable(exchange.getRequestHeaders().get(X_B3_PARENTSPANID))
            .map(HeaderValues::peekFirst)
            .orElse(generateRandomTraceId());
        MDC.put(X_B3_PARENTSPANID, parentSpanID);
        String sampleID = Optional.ofNullable(exchange.getRequestHeaders().get(X_B3_SAMPLED))
            .map(HeaderValues::peekFirst)
            .orElse(generateRandomTraceId());
        MDC.put(X_B3_SAMPLED, sampleID);

        if (!exchange.isResponseComplete() && getNext() != null) {
            next(exchange, context);
        }
    }
}
