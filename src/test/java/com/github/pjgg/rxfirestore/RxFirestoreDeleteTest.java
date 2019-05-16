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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class RxFirestoreDeleteTest {

	private final String brandName = "Toyota";

	@Before
	public void clean_scenario() {

		Query query = TestSuite.getInstance().vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME);
		TestSuite.getInstance().vehicleRepository.get(query.whereEqualTo("brand", brandName)).blockingGet().forEach(vehicle -> {
			TestSuite.getInstance().vehicleRepository.delete(vehicle.getId(), Vehicle.CARS_COLLECTION_NAME).blockingGet();
		});
	}

	@Test
	public void should_delete_car() throws Throwable {
		VertxTestContext testContext = new VertxTestContext();
		TestObserver<Boolean> testObserver = new TestObserver();
		Vehicle vehicle = new Vehicle(brandName, "Auris", true);
		Single<Boolean> isDeleted  = TestSuite.getInstance().vehicleRepository.insert(vehicle).flatMap(id -> {
			testContext.completeNow();
			return TestSuite.getInstance().vehicleRepository.delete(id, Vehicle.CARS_COLLECTION_NAME);
		});

		Observable<Boolean> result = Observable.fromFuture(isDeleted.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertNever(r -> r == false);

		assertThat(testContext.awaitCompletion(3, TimeUnit.SECONDS)).isTrue();


	}
}
