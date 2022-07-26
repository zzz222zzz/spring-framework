/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client;

import java.io.IOException;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation for a {@code RestTemplate} {@link Observation.ObservationConvention},
 * extracting information from the {@link RestTemplateObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultRestTemplateObservationConvention implements RestTemplateObservationConvention {

	private static final KeyValue URI_NONE = KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.URI, "none");

	private static final KeyValue METHOD_NONE = KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.METHOD, "none");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.OUTCOME, "UNKNOWN");
	
	private static final KeyValue URI_EXPANDED_NONE = KeyValue.of(RestTemplateObservation.HighCardinalityKeyNames.URI_EXPANDED, "none");



	@Override
	public String getName() {
		return "http.client.requests";
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(RestTemplateObservationContext context) {
		return KeyValues.of(uri(context), method(context), status(context), exception(context), outcome(context));
	}

	protected KeyValue uri(RestTemplateObservationContext context) {
		if (context.getUriTemplate() != null) {
			return KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.URI, context.getUriTemplate());
		}
		return URI_NONE;
	}

	protected KeyValue method(RestTemplateObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(RestTemplateObservationContext context) {
		return KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.STATUS, getStatusMessage(context.getResponse()));
	}

	private String getStatusMessage(@Nullable ClientHttpResponse response) {
		try {
			if (response == null) {
				return "CLIENT_ERROR";
			}
			return String.valueOf(response.getStatusCode().value());
		}
		catch (IOException ex) {
			return "IO_ERROR";
		}
	}

	protected KeyValue exception(RestTemplateObservationContext context) {
		return context.getError().map(exception -> {
			String simpleName = exception.getClass().getSimpleName();
			return KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}).orElse(EXCEPTION_NONE);
	}

	protected static KeyValue outcome(RestTemplateObservationContext context) {
		try {
			if (context.getResponse() != null) {
				HttpStatus status = HttpStatus.resolve(context.getResponse().getStatusCode().value());
				if (status != null) {
					return KeyValue.of(RestTemplateObservation.LowCardinalityKeyNames.OUTCOME, status.series().name());
				}
			}
		}
		catch (IOException ex) {
			// Continue
		}
		return OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(RestTemplateObservationContext context) {
		return KeyValues.of(requestUri(context), clientName(context));
	}

	protected KeyValue requestUri(RestTemplateObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(RestTemplateObservation.HighCardinalityKeyNames.URI_EXPANDED, context.getCarrier().getURI().toASCIIString());
		}
		return URI_EXPANDED_NONE;
	}

	protected KeyValue clientName(RestTemplateObservationContext context) {
		String host = "none";
		if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
			host = context.getCarrier().getURI().getHost();
		}
		return KeyValue.of(RestTemplateObservation.HighCardinalityKeyNames.CLIENT_NAME, host);
	}



}
