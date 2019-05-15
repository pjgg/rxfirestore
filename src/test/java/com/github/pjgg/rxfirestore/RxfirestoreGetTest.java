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

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class RxfirestoreGetTest {

	private final String brandName = "Toyota";
	private VertxTestContext testContext;

	@Before
	public void clean_scenario() {
		testContext = new VertxTestContext();
		Query query = TestSuite.getInstance().vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME);
		TestSuite.getInstance().vehicleRepository.get(query.whereEqualTo("brand", brandName)).blockingGet()
			.forEach(vehicle -> {
				TestSuite.getInstance().vehicleRepository.delete(vehicle.getId(), Vehicle.CARS_COLLECTION_NAME)
					.blockingGet();
			});
	}

	@Test
	public void should_get_car() throws Throwable {

		TestObserver<Vehicle> testObserver = new TestObserver();
		String expectedModel = "Auris";
		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<Vehicle> retrievedCar = TestSuite.getInstance().vehicleRepository.insert(vehicle)
			.flatMap(id -> TestSuite.getInstance().vehicleRepository.get(id, Vehicle.CARS_COLLECTION_NAME));
		Observable<Vehicle> result = Observable.fromFuture(retrievedCar.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValue(v -> {
			testContext.completeNow();
			return v.getBrand().equalsIgnoreCase(brandName);
		});

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();

	}


	@Test
	public void should_get_where() throws Throwable {
		TestObserver<List<Vehicle>> testObserver = new TestObserver();
		String expectedModel = "Auris";
		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<List<Vehicle>> vehicles = TestSuite.getInstance().vehicleRepository.insert(vehicle).flatMap(id ->
			TestSuite.getInstance().vehicleRepository.queryBuilder(Vehicle.CARS_COLLECTION_NAME)
				.flatMap(query -> TestSuite.getInstance().vehicleRepository.get(query)));
		Observable<List<Vehicle>> result = Observable.fromFuture(vehicles.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();

		testObserver.values().stream()
			.forEach(vehicleList -> vehicleList.forEach(v -> {
				v.getBrand().equalsIgnoreCase(brandName);
				testContext.completeNow();
			}));

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();

	}

	@Test
	public void should_get_where_equalTo() throws Throwable {
		TestObserver<List<Vehicle>> testObserver = new TestObserver();
		String expectedModel = "Auris";
		VehicleRepository vehicleRepository = TestSuite.getInstance().vehicleRepository;
		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<List<Vehicle>> vehicles = TestSuite.getInstance().vehicleRepository.insert(vehicle).flatMap(id -> {
			Query query = vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME)
				.whereEqualTo("model", "Auris");
			return vehicleRepository.get(query);
		});

		Observable<List<Vehicle>> result = Observable.fromFuture(vehicles.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();

		testObserver.values().stream().forEach(vehicleList -> vehicleList.forEach(v -> {
			v.getBrand().equalsIgnoreCase(brandName);
			testContext.completeNow();
		}));

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();

	}

	@Test
	public void should_get_where_equal_not_exists() throws Throwable {
		TestObserver<List<Vehicle>> testObserver = new TestObserver();
		String expectedModel = "Auris";
		VehicleRepository vehicleRepository = TestSuite.getInstance().vehicleRepository;
		AtomicInteger vehiclesRetrievedCounter = new AtomicInteger(0);

		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<List<Vehicle>> vehicles = TestSuite.getInstance().vehicleRepository.insert(vehicle).flatMap(id -> {
			Query query = vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME)
				.whereEqualTo("model", "doesn't exist");
			return vehicleRepository.get(query);
		});

		Observable<List<Vehicle>> result = Observable.fromFuture(vehicles.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();

		testObserver.values().stream().forEach(vehicleList -> vehicleList.forEach(v -> {
			v.getBrand().equalsIgnoreCase(brandName);
			vehiclesRetrievedCounter.getAndIncrement();
			testContext.completeNow();
		}));

		assertThat(testContext.awaitCompletion(3, TimeUnit.SECONDS)).isFalse();

	}

}
