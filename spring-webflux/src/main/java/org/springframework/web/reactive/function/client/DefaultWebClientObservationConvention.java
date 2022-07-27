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

package org.springframework.web.reactive.function.client;

import java.io.IOException;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Default implementation for a {@code WebClient} {@link Observation.ObservationConvention},
 * extracting information from the {@link WebClientObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultWebClientObservationConvention implements WebClientObservationConvention {

	private static final KeyValue URI_NONE = KeyValue.of(WebClientObservation.LowCardinalityKeyNames.URI, "none");

	private static final KeyValue METHOD_NONE = KeyValue.of(WebClientObservation.LowCardinalityKeyNames.METHOD, "none");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(WebClientObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(WebClientObservation.LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	@Override
	public String getName() {
		return "http.client.requests";
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(WebClientObservationContext context) {
		return KeyValues.of(uri(context), method(context), status(context), exception(context), outcome(context));
	}

	protected KeyValue uri(WebClientObservationContext context) {
		if (context.getUriTemplate() != null) {
			return KeyValue.of(WebClientObservation.LowCardinalityKeyNames.URI, context.getUriTemplate());
		}
		return URI_NONE;
	}

	protected KeyValue method(WebClientObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(WebClientObservation.LowCardinalityKeyNames.METHOD, context.getCarrier().method().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(WebClientObservationContext context) {
		return KeyValue.of(WebClientObservation.LowCardinalityKeyNames.STATUS, getStatusMessage(context));
	}

	private String getStatusMessage(WebClientObservationContext context) {
		if (context.getResponse() != null) {
			return String.valueOf(context.getResponse().statusCode().value());
		}
		if (context.getError().isPresent()) {
			return (context.getError().get() instanceof IOException) ? "IO_ERROR" : "CLIENT_ERROR";
		}
		return "CLIENT_ERROR";
	}

	protected KeyValue exception(WebClientObservationContext context) {
		return context.getError().map(exception -> {
			String simpleName = exception.getClass().getSimpleName();
			return KeyValue.of(WebClientObservation.LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}).orElse(EXCEPTION_NONE);
	}

	protected static KeyValue outcome(WebClientObservationContext context) {
		if (context.getResponse() != null) {
			HttpStatus status = HttpStatus.resolve(context.getResponse().statusCode().value());
			if (status != null) {
				return KeyValue.of(WebClientObservation.LowCardinalityKeyNames.OUTCOME, status.series().name());
			}
		}
		return OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(WebClientObservationContext context) {
		return KeyValues.of(uriExpanded(context), clientName(context));
	}

	protected KeyValue uriExpanded(WebClientObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(WebClientObservation.HighCardinalityKeyNames.URI_EXPANDED, context.getCarrier().url().toASCIIString());
		}
		return KeyValue.of(WebClientObservation.HighCardinalityKeyNames.URI_EXPANDED, "none");
	}

	protected KeyValue clientName(WebClientObservationContext context) {
		String host = "none";
		if (context.getCarrier() != null && context.getCarrier().url().getHost() != null) {
			host = context.getCarrier().url().getHost();
		}
		return KeyValue.of(WebClientObservation.HighCardinalityKeyNames.CLIENT_NAME, host);
	}

}
