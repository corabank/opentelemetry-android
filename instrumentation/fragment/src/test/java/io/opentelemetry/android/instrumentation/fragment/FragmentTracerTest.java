/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment;

import static io.opentelemetry.android.common.RumConstants.LAST_SCREEN_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import io.opentelemetry.android.common.ActiveSpan;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

class FragmentTracerTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private Tracer tracer;
    private ActiveSpan activeSpan;

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        VisibleScreenService visibleScreenService = Mockito.mock(VisibleScreenService.class);
        activeSpan = new ActiveSpan(visibleScreenService::getPreviouslyVisibleScreen);
    }

    @Test
    void create() {
        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();
        trackableTracer.startFragmentCreation();
        trackableTracer.endActiveSpan();
        SpanData span = getSingleSpan();
        assertEquals("Created", span.getName());
    }

    @Test
    void addPreviousScreen_noPrevious() {
        VisibleScreenService visibleScreenService = Mockito.mock(VisibleScreenService.class);

        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    void addPreviousScreen_currentSameAsPrevious() {
        VisibleScreenService visibleScreenService = Mockito.mock(VisibleScreenService.class);
        when(visibleScreenService.getPreviouslyVisibleScreen()).thenReturn("Fragment");

        FragmentTracer trackableTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        trackableTracer.startSpanIfNoneInProgress("starting");
        trackableTracer.addPreviousScreenAttribute();
        trackableTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertNull(span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    @Test
    void addPreviousScreen() {
        VisibleScreenService visibleScreenService = Mockito.mock(VisibleScreenService.class);
        when(visibleScreenService.getPreviouslyVisibleScreen()).thenReturn("previousScreen");
        activeSpan = new ActiveSpan(visibleScreenService::getPreviouslyVisibleScreen);

        FragmentTracer fragmentTracer =
                FragmentTracer.builder(mock(Fragment.class))
                        .setTracer(tracer)
                        .setActiveSpan(activeSpan)
                        .build();

        fragmentTracer.startSpanIfNoneInProgress("starting");
        fragmentTracer.addPreviousScreenAttribute();
        fragmentTracer.endActiveSpan();

        SpanData span = getSingleSpan();
        assertEquals("previousScreen", span.getAttributes().get(LAST_SCREEN_NAME_KEY));
    }

    private SpanData getSingleSpan() {
        List<SpanData> generatedSpans = otelTesting.getSpans();
        assertEquals(1, generatedSpans.size());
        return generatedSpans.get(0);
    }
}
