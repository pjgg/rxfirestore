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
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class RxFirestoreUpdateTest {

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
	public void testshould_update_car() throws InterruptedException {

		TestObserver<Boolean> testObserver = new TestObserver();
		String expectedModel = "Auris_updated";
		Vehicle vehicle = new Vehicle(brandName, "Auris", true);
		Single<Boolean> isUpdated = TestSuite.getInstance().vehicleRepository.insert(vehicle).flatMap(id -> {
			vehicle.setModel(expectedModel);
			return TestSuite.getInstance().vehicleRepository.update(id, vehicle.getCollectionName(), vehicle);
		});

		Observable<Boolean> result = Observable.fromFuture(isUpdated.toFuture());
		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertNever(r -> {
			testContext.completeNow();
			return r == false;
		});

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();
	}


/*
	@Ignore
	@Test
	public void should_update_partial_car(){

		String expectedModel = "Auris_updated";
		HashMap<String, Object> u = new HashMap();
		u.put("model", expectedModel);

		var vehicle = new Vehicle("Toyota", "Auris", true);
		var ID = vehicleRepository.insert(vehicle).blockingGet();
		vehicle.setModel(expectedModel);
		var isUpdated = vehicleRepository.update(ID, Vehicle.CARS_COLLECTION_NAME, u).blockingGet();

		if (isUpdated) {
			var retrievedCar = vehicleRepository.get(ID, Vehicle.CARS_COLLECTION_NAME).blockingGet();
			assertEquals(retrievedCar.getModel(), expectedModel);
		}else{
			assertTrue(false, "Should update vehicle model, but return isUpdated false.");
		}
	}


*/
}
