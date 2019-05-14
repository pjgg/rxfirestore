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

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

public class RxfirestoreGetTest {

	//TODO: You need to set your Gcloud creadentials as enviroment variable, example: GCLOUD_KEY_PATH=/Users/pablo/Desktop/keyfile.json
	private VehicleRepository vehicleRepository = new VehicleRepository();
	private final String brandName = "Toyota";

	@Before
	public void clean_scenario() {
		Query query = vehicleRepository.queryBuilderSync(Vehicle.CARS_COLLECTION_NAME);
		vehicleRepository.get(query.whereEqualTo("brand", brandName)).blockingGet().forEach(vehicle -> {
			vehicleRepository.delete(vehicle.getId(), Vehicle.CARS_COLLECTION_NAME).blockingGet();
		});
		System.out.println("Done!");
	}

	@Test
	public void should_get_car(){

		TestObserver<Vehicle> testObserver = new TestObserver();
		String expectedModel = "Auris";
		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<Vehicle> retrievedCar = vehicleRepository.insert(vehicle).flatMap(id -> vehicleRepository.get(id, Vehicle.CARS_COLLECTION_NAME));
		Observable<Vehicle> result = Observable.fromFuture(retrievedCar.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValue(v -> v.getBrand().equalsIgnoreCase(brandName));

	}


	@Test
	public void should_get_where(){
		TestObserver<List<Vehicle>> testObserver = new TestObserver();
		String expectedModel = "Auris";
		Vehicle vehicle = new Vehicle(brandName, expectedModel, true);
		Single<List<Vehicle>> vehicles = vehicleRepository.insert(vehicle).flatMap(id -> vehicleRepository.queryBuilder(Vehicle.CARS_COLLECTION_NAME).flatMap(query -> vehicleRepository.get(query)));
		Observable<List<Vehicle>> result = Observable.fromFuture(vehicles.toFuture());

		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();

		testObserver.values().stream().forEach(vehicleList -> vehicleList.forEach(v -> v.getBrand().equalsIgnoreCase(brandName)));

	}

}
