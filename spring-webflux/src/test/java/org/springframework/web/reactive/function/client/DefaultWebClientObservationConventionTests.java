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

import java.net.URI;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultWebClientObservationConvention}.
 *
 * @author Brian Clozel
 */
class DefaultWebClientObservationConventionTests {

	private DefaultWebClientObservationConvention observationConvention = new DefaultWebClientObservationConvention();

	@Test
	void shouldOnlySupportWebClientObservationContext() {
		assertThat(this.observationConvention.supportsContext(new WebClientObservationContext())).isTrue();
		assertThat(this.observationConvention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void shouldAddKeyValuesForNullExchange() {
		WebClientObservationContext context = new WebClientObservationContext();
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "none"), KeyValue.of("uri", "none"), KeyValue.of("status", "CLIENT_ERROR"),
						KeyValue.of("exception", "none"), KeyValue.of("outcome", "UNKNOWN"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("client.name", "none"), KeyValue.of("uri.expanded", "none"));
	}

	@Test
	void shouldAddKeyValuesForExchangeWithException() {
		WebClientObservationContext context = new WebClientObservationContext();
		context.setError(new IllegalStateException("Could not create client request"));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context)).hasSize(5)
				.contains(KeyValue.of("method", "none"), KeyValue.of("uri", "none"), KeyValue.of("status", "CLIENT_ERROR"),
						KeyValue.of("exception", "IllegalStateException"), KeyValue.of("outcome", "UNKNOWN"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("client.name", "none"), KeyValue.of("uri.expanded", "none"));
	}

	@Test
	void shouldAddKeyValuesForRequestWithUriTemplate() {
		ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("/resource/42"))
				.attribute(WebClient.class.getName() + ".uriTemplate", "/resource/{id}").build();
		WebClientObservationContext context = createContext(request);
		context.setUriTemplate("/resource/{id}");
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("exception", "none"), KeyValue.of("method", "GET"), KeyValue.of("uri", "/resource/{id}"),
						KeyValue.of("status", "200"), KeyValue.of("outcome", "SUCCESS"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(2)
				.contains(KeyValue.of("client.name", "none"), KeyValue.of("uri.expanded", "/resource/42"));
	}

	@Test
	void shouldAddKeyValuesForRequestWithoutUriTemplate() {
		WebClientObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("/resource/42")).build());
		assertThat(this.observationConvention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("method", "GET"), KeyValue.of("uri", "none"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).hasSize(2).contains(KeyValue.of("uri.expanded", "/resource/42"));
	}

	@Test
	void shouldAddClientNameKeyValueForRequestWithHost() {
		WebClientObservationContext context = createContext(ClientRequest.create(HttpMethod.GET, URI.create("https://localhost:8080/resource/42")).build());
		assertThat(this.observationConvention.getHighCardinalityKeyValues(context)).contains(KeyValue.of("client.name", "localhost"));
	}

	private WebClientObservationContext createContext(ClientRequest request) {
		WebClientObservationContext context = new WebClientObservationContext();
		context.setCarrier(request);
		context.setResponse(ClientResponse.create(HttpStatus.OK).build());
		return context;
	}

}
