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

package org.springframework.web.servlet.observation;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 
 * @author Brian Clozel
 * @since 6.0
 */
public class HttpRequestsObservationFilter extends OncePerRequestFilter {

	/**
	 *
	 */
	public static final String CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE = HttpRequestsObservationFilter.class.getName() + ".context";
	
	private static final Log logger = LogFactory.getLog(HttpRequestsObservationFilter.class);

	private static final HttpRequestsObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultHttpRequestsObservationConvention();

	private static final String CURRENT_OBSERVATION_ATTRIBUTE = HttpRequestsObservationFilter.class.getName() + ".observation";


	private final ObservationRegistry observationRegistry;

	private final HttpRequestsObservationConvention observationConvention;

	public HttpRequestsObservationFilter(ObservationRegistry observationRegistry) {
		this(observationRegistry, new DefaultHttpRequestsObservationConvention());
	}

	public HttpRequestsObservationFilter(ObservationRegistry observationRegistry, HttpRequestsObservationConvention observationConvention) {
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	@Override
	@SuppressWarnings("try") // for observation.openScope()
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		Observation observation = createOrFetchObservation(request, response);
		try (Observation.Scope scope = observation.openScope()){
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			observation.error(unwrapServletException(ex)).stop();
			throw ex;
		}
		finally {
			// Only stop Observation if async processing is done or has never been started.
			if (!request.isAsyncStarted()) {
				observation.error(fetchException(request));
				observation.stop();
			}
		}
	}

	private Observation createOrFetchObservation(HttpServletRequest request, HttpServletResponse response) {
		Observation observation = (Observation) request.getAttribute(CURRENT_OBSERVATION_ATTRIBUTE);
		if (observation == null) {
			HttpRequestsObservationContext context = new HttpRequestsObservationContext(request, response);
			observation = HttpRequestsObservation.HTTP_REQUESTS.observation(this.observationConvention,
					DEFAULT_OBSERVATION_CONVENTION, context, this.observationRegistry).start();
			request.setAttribute(CURRENT_OBSERVATION_ATTRIBUTE, observation);
			request.setAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation.getContext());
		}
		return observation;
	}

	private Throwable unwrapServletException(Throwable ex) {
		return (ex instanceof ServletException) ? ex.getCause() : ex;
	}

	private Throwable fetchException(HttpServletRequest request) {
		return (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
	}

}
