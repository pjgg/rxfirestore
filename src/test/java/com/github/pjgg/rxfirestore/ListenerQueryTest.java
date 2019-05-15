/*
 * Copyright 2019 RxFirestore.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.pjgg.rxfirestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ListenerQueryTest {

	private final String brandName = "Toyota";
	private VertxTestContext testContext;

	@Before
	public void clean_scenario() {
		testContext = new VertxTestContext();
		Query query = TestSuite.getInstance().vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME);
		TestSuite.getInstance().vehicleRepository.get(query.whereEqualTo("brand", brandName)).blockingGet().forEach(vehicle -> {
			TestSuite.getInstance().vehicleRepository.delete(vehicle.getId(), Vehicle.CARS_COLLECTION_NAME).blockingGet();
		});
	}

	@Test
	public void should_subscribe_to_query() throws Throwable {
		final int ITERATIONS = 4;
		final String randomModel = UUID.randomUUID().toString();
		final CountDownLatch latch = new CountDownLatch(ITERATIONS);
		for (int i = 0; i < ITERATIONS; i++) {
			Vehicle vehicle = new Vehicle(brandName, randomModel, true);
			TestSuite.getInstance().vehicleRepository.insert(vehicle)
					.doOnError(error -> System.out.println(error.getMessage()))
					.subscribe(id -> {
						System.out.println(id);
						latch.countDown();
					});
		}

		latch.await();
		boolean ends = listenEventTask(TestSuite.getInstance().vehicleRepository, ITERATIONS,
			TestSuite.getInstance().vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME).whereEqualTo("model", randomModel));

		assertThat(testContext.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
		assertTrue(ends);
	}

	private boolean listenEventTask(VehicleRepository vehicleRepository, int iterations, Query query)
			throws InterruptedException, TimeoutException, ExecutionException {
		final CountDownLatch latch = new CountDownLatch(iterations);
		AtomicBoolean result = new AtomicBoolean(true);

		EventListenerResponse<Vehicle> listener = vehicleRepository.addQueryListener(query, Optional.empty());
		listener.getEventsFlow().subscribe(
				event -> {
					System.out.println("Event Type:" + event.getEventType() + " model: " + event.getModel());
					latch.countDown();
				}
				, error -> {
					System.err.println(error.getMessage());
					result.set(false);
				    testContext.failNow(error);
					latch.countDown();
				});

		latch.await();
		testContext.completeNow();
		listener.getRegistration().remove();
		return result.get();
	}

}
