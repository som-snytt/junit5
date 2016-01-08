/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit4;

import static java.util.Collections.singleton;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.junit.gen5.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.gen5.commons.util.FunctionUtils.where;
import static org.junit.gen5.engine.ClassFilters.classNameMatches;
import static org.junit.gen5.engine.TestPlanSpecification.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.gen5.api.Test;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestPlanSpecification;
import org.junit.gen5.engine.TestTag;
import org.junit.gen5.engine.junit4.samples.PlainOldJavaClassWithoutAnyTest;
import org.junit.gen5.engine.junit4.samples.junit3.JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails;
import org.junit.gen5.engine.junit4.samples.junit3.PlainJUnit3TestCaseWithSingleTestWhichFails;
import org.junit.gen5.engine.junit4.samples.junit4.Categories.Failing;
import org.junit.gen5.engine.junit4.samples.junit4.Categories.Plain;
import org.junit.gen5.engine.junit4.samples.junit4.Categories.Skipped;
import org.junit.gen5.engine.junit4.samples.junit4.Categories.SkippedWithReason;
import org.junit.gen5.engine.junit4.samples.junit4.IgnoredJUnit4TestCase;
import org.junit.gen5.engine.junit4.samples.junit4.JUnit4TestCaseWithOverloadedMethod;
import org.junit.gen5.engine.junit4.samples.junit4.PlainJUnit4TestCaseWithFiveTestMethods;
import org.junit.gen5.engine.junit4.samples.junit4.PlainJUnit4TestCaseWithSingleTestWhichFails;
import org.junit.gen5.engine.junit4.samples.junit4.SingleFailingTheoryTestCase;
import org.junit.gen5.engine.junit4.samples.junit4.TestCaseRunWithJUnit5;

class JUnit4TestEngineDiscoveryTests {

	JUnit4TestEngine engine = new JUnit4TestEngine();

	@Test
	void resolvesSimpleJUnit4TestClass() {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertTrue(runnerDescriptor.isContainer());
		assertFalse(runnerDescriptor.isTest());
		assertEquals(testClass.getName(), runnerDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName(), runnerDescriptor.getUniqueId());

		TestDescriptor childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTrue(childDescriptor.isTest());
		assertFalse(childDescriptor.isContainer());
		assertEquals("failingTest", childDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName() + "/failingTest(" + testClass.getName() + ")",
			childDescriptor.getUniqueId());
		assertThat(childDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesIgnoredJUnit4TestClass() {
		Class<?> testClass = IgnoredJUnit4TestCase.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertFalse(runnerDescriptor.isContainer());
		assertTrue(runnerDescriptor.isTest());
		assertEquals(testClass.getName(), runnerDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName(), runnerDescriptor.getUniqueId());
		assertThat(runnerDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesJUnit4TestClassWithCustomRunner() {
		Class<?> testClass = SingleFailingTheoryTestCase.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertTrue(runnerDescriptor.isContainer());
		assertFalse(runnerDescriptor.isTest());
		assertEquals(testClass.getName(), runnerDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName(), runnerDescriptor.getUniqueId());

		TestDescriptor childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTrue(childDescriptor.isTest());
		assertFalse(childDescriptor.isContainer());
		assertEquals("theory", childDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName() + "/theory(" + testClass.getName() + ")",
			childDescriptor.getUniqueId());
		assertThat(childDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesJUnit3TestCase() {
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertTrue(runnerDescriptor.isContainer());
		assertFalse(runnerDescriptor.isTest());
		assertEquals(testClass.getName(), runnerDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName(), runnerDescriptor.getUniqueId());

		TestDescriptor childDescriptor = getOnlyElement(runnerDescriptor.getChildren());
		assertTrue(childDescriptor.isTest());
		assertFalse(childDescriptor.isContainer());
		assertEquals("test", childDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName() + "/test(" + testClass.getName() + ")",
			childDescriptor.getUniqueId());
		assertThat(childDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesJUnit3Suites() {
		Class<?> suiteClass = JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails.class;
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		TestPlanSpecification specification = buildClassSpecification(suiteClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor suiteDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertTrue(suiteDescriptor.isContainer());
		assertFalse(suiteDescriptor.isTest());
		assertThat(suiteDescriptor.getDisplayName()).startsWith("TestSuite with 1 tests");
		assertEquals("junit4:" + suiteClass.getName(), suiteDescriptor.getUniqueId());

		TestDescriptor testClassDescriptor = getOnlyElement(suiteDescriptor.getChildren());
		assertTrue(testClassDescriptor.isContainer());
		assertFalse(testClassDescriptor.isTest());
		assertEquals(testClass.getName(), testClassDescriptor.getDisplayName());
		assertEquals("junit4:" + suiteClass.getName() + "/" + testClass.getName(), testClassDescriptor.getUniqueId());

		TestDescriptor testMethodDescriptor = getOnlyElement(testClassDescriptor.getChildren());
		assertTrue(testMethodDescriptor.isTest());
		assertFalse(testMethodDescriptor.isContainer());
		assertEquals("test", testMethodDescriptor.getDisplayName());
		assertEquals(
			"junit4:" + suiteClass.getName() + "/" + testClass.getName() + "/test" + "(" + testClass.getName() + ")",
			testMethodDescriptor.getUniqueId());
		assertThat(testMethodDescriptor.getChildren()).isEmpty();
	}

	@Test
	void resolvesJUnit4TestCaseWithOverloadedMethod() {
		Class<?> testClass = JUnit4TestCaseWithOverloadedMethod.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertTrue(runnerDescriptor.isContainer());
		assertFalse(runnerDescriptor.isTest());
		assertEquals(testClass.getName(), runnerDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName(), runnerDescriptor.getUniqueId());

		List<TestDescriptor> testMethodDescriptors = new ArrayList<>(runnerDescriptor.getChildren());
		assertThat(testMethodDescriptors).hasSize(2);

		TestDescriptor testMethodDescriptor = testMethodDescriptors.get(0);
		assertTrue(testMethodDescriptor.isTest());
		assertFalse(testMethodDescriptor.isContainer());
		assertEquals("theory", testMethodDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName() + "/theory" + "(" + testClass.getName() + ")[0]",
			testMethodDescriptor.getUniqueId());
		assertThat(testMethodDescriptor.getChildren()).isEmpty();

		testMethodDescriptor = testMethodDescriptors.get(1);
		assertTrue(testMethodDescriptor.isTest());
		assertFalse(testMethodDescriptor.isContainer());
		assertEquals("theory", testMethodDescriptor.getDisplayName());
		assertEquals("junit4:" + testClass.getName() + "/theory" + "(" + testClass.getName() + ")[1]",
			testMethodDescriptor.getUniqueId());
		assertThat(testMethodDescriptor.getChildren()).isEmpty();
	}

	@Test
	void doesNotResolvePlainOldJavaClassesWithoutAnyTest() {
		assertYieldsNoDescriptors(PlainOldJavaClassWithoutAnyTest.class);
	}

	@Test
	void doesNotResolveClassRunWithJUnit5() {
		assertYieldsNoDescriptors(TestCaseRunWithJUnit5.class);
	}

	@Test
	void resolvesAllTestsSpecification() throws Exception {
		File root = getClasspathRoot(PlainJUnit4TestCaseWithSingleTestWhichFails.class);
		TestPlanSpecification specification = build(allTests(singleton(root)));

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getName())
			.contains(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getName())
			.doesNotContain(PlainOldJavaClassWithoutAnyTest.class.getName());
		// @formatter:on
	}

	@Test
	void resolvesApplyingClassFilters() throws Exception {
		File root = getClasspathRoot(PlainJUnit4TestCaseWithSingleTestWhichFails.class);
		TestPlanSpecification specification = build(allTests(singleton(root)));
		specification.filterWith(classNameMatches(".*JUnit4.*"));
		specification.filterWith(classNameMatches(".*Plain.*"));

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getName())
			.doesNotContain(JUnit4TestCaseWithOverloadedMethod.class.getName())
			.doesNotContain(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getName());
		// @formatter:on
	}

	@Test
	void resolvesPackageSpecificationForJUnit4SamplesPackage() {
		Class<?> testClass = PlainJUnit4TestCaseWithSingleTestWhichFails.class;
		TestPlanSpecification specification = build(forPackage(testClass.getPackage().getName()));

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(testClass.getName())
			.doesNotContain(PlainJUnit3TestCaseWithSingleTestWhichFails.class.getName());
		// @formatter:on
	}

	@Test
	void resolvesPackageSpecificationForJUnit3SamplesPackage() {
		Class<?> testClass = PlainJUnit3TestCaseWithSingleTestWhichFails.class;
		TestPlanSpecification specification = build(forPackage(testClass.getPackage().getName()));

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		// @formatter:off
		assertThat(engineDescriptor.getChildren())
			.extracting(TestDescriptor::getDisplayName)
			.contains(testClass.getName())
			.doesNotContain(PlainJUnit4TestCaseWithSingleTestWhichFails.class.getName());
		// @formatter:on
	}

	@Test
	void resolvesCategoriesIntoTags() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		TestDescriptor runnerDescriptor = getOnlyElement(engineDescriptor.getChildren());
		assertThat(runnerDescriptor.getTags()).containsOnly(new TestTag(Plain.class.getName()));

		TestDescriptor failingTestDescriptor = findChildByName(runnerDescriptor, "failingTest");
		assertThat(failingTestDescriptor.getTags()).containsOnly(new TestTag(Plain.class.getName()),
			new TestTag(Failing.class.getName()));

		TestDescriptor ignoredWithoutReasonTestDescriptor = findChildByName(runnerDescriptor,
			"ignoredTest1_withoutReason");
		assertThat(ignoredWithoutReasonTestDescriptor.getTags()).containsOnly(new TestTag(Plain.class.getName()),
			new TestTag(Skipped.class.getName()));

		TestDescriptor ignoredWithReasonTestDescriptor = findChildByName(runnerDescriptor, "ignoredTest2_withReason");
		assertThat(ignoredWithReasonTestDescriptor.getTags()).containsOnly(new TestTag(Plain.class.getName()),
			new TestTag(Skipped.class.getName()), new TestTag(SkippedWithReason.class.getName()));
	}

	private TestDescriptor findChildByName(TestDescriptor runnerDescriptor, String name) {
		Predicate<TestDescriptor> predicate = where(TestDescriptor::getDisplayName, isEqual(name));
		return runnerDescriptor.getChildren().stream().filter(predicate).findAny().orElseThrow(() -> new AssertionError(
			"No child with display name \"" + name + "\" in " + runnerDescriptor.getChildren()));
	}

	private File getClasspathRoot(Class<?> testClass) throws Exception {
		URL location = testClass.getProtectionDomain().getCodeSource().getLocation();
		return new File(location.toURI());
	}

	private void assertYieldsNoDescriptors(Class<?> testClass) {
		TestPlanSpecification specification = buildClassSpecification(testClass);

		TestDescriptor engineDescriptor = engine.discoverTests(specification);

		assertThat(engineDescriptor.getChildren()).isEmpty();
	}

	private static TestPlanSpecification buildClassSpecification(Class<?> testClass) {
		return build(forClass(testClass));
	}
}
