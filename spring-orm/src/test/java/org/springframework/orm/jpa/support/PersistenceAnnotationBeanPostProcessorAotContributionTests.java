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

package org.springframework.orm.jpa.support;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BiConsumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PersistenceAnnotationBeanPostProcessor} AOT contribution.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@CompileWithTargetClassAccess
class PersistenceAnnotationBeanPostProcessorAotContributionTests {

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private TestGenerationContext generationContext;

	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.generationContext = new TestGenerationContext();
	}

	@Test
	void processAheadOfTimeWhenPersistenceUnitOnPublicField() {
		RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitField.class);
		testCompile(registeredBean, (actual, compiled) -> {
			EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
			this.beanFactory.registerSingleton("entityManagerFactory",
					entityManagerFactory);
			DefaultPersistenceUnitField instance = new DefaultPersistenceUnitField();
			actual.accept(registeredBean, instance);
			assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
			assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
					.isEmpty();
		});
	}

	@Test
	void processAheadOfTimeWhenPersistenceUnitOnPublicSetter() {
		RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitMethod.class);
		testCompile(registeredBean, (actual, compiled) -> {
			EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
			this.beanFactory.registerSingleton("entityManagerFactory",
					entityManagerFactory);
			DefaultPersistenceUnitMethod instance = new DefaultPersistenceUnitMethod();
			actual.accept(registeredBean, instance);
			assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
			assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
					.isEmpty();
		});
	}

	@Test
	void processAheadOfTimeWhenCustomPersistenceUnitOnPublicSetter() {
		RegisteredBean registeredBean = registerBean(
				CustomUnitNamePublicPersistenceUnitMethod.class);
		testCompile(registeredBean, (actual, compiled) -> {
			EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
			this.beanFactory.registerSingleton("custom", entityManagerFactory);
			CustomUnitNamePublicPersistenceUnitMethod instance = new CustomUnitNamePublicPersistenceUnitMethod();
			actual.accept(registeredBean, instance);
			assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
			assertThat(compiled.getSourceFile()).contains(
					"findEntityManagerFactory((ListableBeanFactory) registeredBean.getBeanFactory(), \"custom\")");
			assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
					.isEmpty();
		});
	}

	@Test
	void processAheadOfTimeWhenPersistenceContextOnPrivateField() {
		RegisteredBean registeredBean = registerBean(
				DefaultPersistenceContextField.class);
		testCompile(registeredBean, (actual, compiled) -> {
			EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
			this.beanFactory.registerSingleton("entityManagerFactory",
					entityManagerFactory);
			DefaultPersistenceContextField instance = new DefaultPersistenceContextField();
			actual.accept(registeredBean, instance);
			assertThat(instance).extracting("entityManager").isNotNull();
			assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
					.singleElement().satisfies(typeHint -> {
						assertThat(typeHint.getType()).isEqualTo(
								TypeReference.of(DefaultPersistenceContextField.class));
						assertThat(typeHint.fields()).singleElement()
								.satisfies(fieldHint -> {
									assertThat(fieldHint.getName())
											.isEqualTo("entityManager");
									assertThat(fieldHint.isAllowWrite()).isTrue();
									assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
								});
					});
		});
	}

	@Test
	void processAheadOfTimeWhenPersistenceContextWithCustomPropertiesOnMethod() {
		RegisteredBean registeredBean = registerBean(
				CustomPropertiesPersistenceContextMethod.class);
		testCompile(registeredBean, (actual, compiled) -> {
			EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
			this.beanFactory.registerSingleton("entityManagerFactory",
					entityManagerFactory);
			CustomPropertiesPersistenceContextMethod instance = new CustomPropertiesPersistenceContextMethod();
			actual.accept(registeredBean, instance);
			Field field = ReflectionUtils.findField(
					CustomPropertiesPersistenceContextMethod.class, "entityManager");
			ReflectionUtils.makeAccessible(field);
			EntityManager sharedEntityManager = (EntityManager) ReflectionUtils
					.getField(field, instance);
			InvocationHandler invocationHandler = Proxy
					.getInvocationHandler(sharedEntityManager);
			assertThat(invocationHandler).extracting("properties")
					.asInstanceOf(InstanceOfAssertFactories.MAP)
					.containsEntry("jpa.test", "value")
					.containsEntry("jpa.test2", "value2");
			assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
					.isEmpty();
		});
	}

	private RegisteredBean registerBean(Class<?> beanClass) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName,
				new RootBeanDefinition(beanClass));
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private void testCompile(RegisteredBean registeredBean,
			BiConsumer<BiConsumer<RegisteredBean, Object>, Compiled> result) {
		PersistenceAnnotationBeanPostProcessor postProcessor = new PersistenceAnnotationBeanPostProcessor();
		BeanRegistrationAotContribution contribution = postProcessor
				.processAheadOfTime(registeredBean);
		BeanRegistrationCode beanRegistrationCode = mock(BeanRegistrationCode.class);
		contribution.applyTo(generationContext, beanRegistrationCode);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles())
				.compile(compiled -> result.accept(new Invoker(compiled), compiled));
	}

	static class Invoker implements BiConsumer<RegisteredBean, Object> {

		private Compiled compiled;

		Invoker(Compiled compiled) {
			this.compiled = compiled;
		}

		@Override
		public void accept(RegisteredBean registeredBean, Object instance) {
			List<Class<?>> compiledClasses = compiled.getAllCompiledClasses();
			assertThat(compiledClasses).hasSize(1);
			Class<?> compiledClass = compiledClasses.get(0);
			for (Method method : ReflectionUtils.getDeclaredMethods(compiledClass)) {
				if (method.getName().equals("apply")) {
					ReflectionUtils.invokeMethod(method, null, registeredBean, instance);
					return;
				}
			}
			throw new IllegalStateException("Did not find apply method");
		}

	}

	static class DefaultPersistenceUnitField {

		@PersistenceUnit
		public EntityManagerFactory emf;

	}

	static class DefaultPersistenceUnitMethod {

		@SuppressWarnings("unused")
		private EntityManagerFactory emf;

		@PersistenceUnit
		public void setEmf(EntityManagerFactory emf) {
			this.emf = emf;
		}

	}

	static class CustomUnitNamePublicPersistenceUnitMethod {

		@SuppressWarnings("unused")
		private EntityManagerFactory emf;

		@PersistenceUnit(unitName = "custom")
		public void setEmf(EntityManagerFactory emf) {
			this.emf = emf;
		}

	}

	static class DefaultPersistenceContextField {

		@SuppressWarnings("unused")
		@PersistenceContext
		private EntityManager entityManager;

	}

	static class CustomPropertiesPersistenceContextMethod {

		@SuppressWarnings("unused")
		private EntityManager entityManager;

		@PersistenceContext(
				properties = { @PersistenceProperty(name = "jpa.test", value = "value"),
						@PersistenceProperty(name = "jpa.test2", value = "value2") })
		public void setEntityManager(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

	}

}
