/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.views.htmx.http;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.body.RawMessageBodyHandler;
import io.micronaut.http.codec.CodecException;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.ModelAndViewRenderer;
import io.micronaut.views.exceptions.ViewRenderingException;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * {@link RawMessageBodyHandler} implementation for {@link HtmxResponse}.
 * @author Sergio del Amo
 * @since 6.0.0
 * @param <T> The model type
 */
@Internal
@Singleton
final class HtmxResponseRawMessageBodyHandler<T> implements RawMessageBodyHandler<HtmxResponse<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(HtmxResponseRawMessageBodyHandler.class);

    private final ModelAndViewRenderer<T, HttpRequest<?>> modelAndViewRenderer;

    public HtmxResponseRawMessageBodyHandler(ModelAndViewRenderer<T, HttpRequest<?>> modelAndViewRenderer) {
        this.modelAndViewRenderer = modelAndViewRenderer;
    }

    @Override
    public @NonNull Collection<? extends Class<?>> getTypes() {
        return Collections.singletonList(HtmxResponse.class);
    }

    @Override
    public @NonNull Publisher<? extends HtmxResponse<T>> readChunked(@NonNull Argument<HtmxResponse<T>> type, @Nullable MediaType mediaType, @NonNull Headers httpHeaders, @NonNull Publisher<ByteBuffer<?>> input) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public @Nullable HtmxResponse<T> read(@NonNull Argument<HtmxResponse<T>> type, @Nullable MediaType mediaType, @NonNull Headers httpHeaders, @NonNull InputStream inputStream) throws CodecException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void writeTo(@NonNull Argument<HtmxResponse<T>> type, @NonNull MediaType mediaType, HtmxResponse<T> object, @NonNull MutableHeaders outgoingHeaders, @NonNull OutputStream outputStream) throws CodecException {
        HttpRequest<?> httpRequest = ServerRequestContext.currentRequest().orElse(null);
        for (ModelAndView<T> modelAndView : object.getModelAndViews()) {
            modelAndViewRenderer.render(modelAndView, httpRequest, mediaType.toString())
                    .ifPresent(writable -> {
                        try {
                            writable.writeTo(outputStream);
                        } catch (IOException e) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error("IOException writing ModelAndView to OutputStream", e);
                            }
                            throw new ViewRenderingException("IOException writing ModelAndView to OutputStream", e);
                        }
                    });
        }
        outgoingHeaders.set(HttpHeaders.CONTENT_TYPE, mediaType);
    }
}
