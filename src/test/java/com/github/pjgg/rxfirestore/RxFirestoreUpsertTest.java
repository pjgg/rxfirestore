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
public class RxFirestoreUpsertTest {

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
	public void testshould_insert_car() throws InterruptedException {

		TestObserver<Boolean> testObserver = new TestObserver();
		String expectedModel = "Auris_updated";
		Vehicle vehicle = new Vehicle(brandName, "Auris", true);
		Single<Boolean> isUpdated = TestSuite.getInstance().vehicleRepository.upsert("001", Vehicle.CARS_COLLECTION_NAME, vehicle);

		Observable<Boolean> result = Observable.fromFuture(isUpdated.toFuture());
		result.subscribe(testObserver);

		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertNever(r -> {
			testContext.completeNow();
			return r == false;
		});

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();

		// check that is properly stored
		TestObserver<Vehicle> vehicleObserver = new TestObserver();
		Observable<Vehicle> r = Observable.fromFuture(TestSuite.getInstance().vehicleRepository.get("001", Vehicle.CARS_COLLECTION_NAME).toFuture());
		r.subscribe(vehicleObserver);

		vehicleObserver.assertComplete();
		vehicleObserver.assertNoErrors();
		vehicleObserver.assertValue(re -> {
			testContext.completeNow();
			return re.getId().equalsIgnoreCase("001");
		});

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testshould_upsert_car() throws InterruptedException {

		TestObserver<Boolean> testObserver = new TestObserver();
		String expectedModel = "Auris_updated";
		Vehicle vehicle = new Vehicle(brandName, "Auris", true);
		Single<Boolean> isUpdated = TestSuite.getInstance().vehicleRepository.upsert("002", Vehicle.CARS_COLLECTION_NAME, vehicle).flatMap( isInsertedVehicle -> {
			if(isInsertedVehicle) {
				vehicle.setModel("Auris_updated");
				return TestSuite.getInstance().vehicleRepository.upsert("002", Vehicle.CARS_COLLECTION_NAME, vehicle);
			}

			return Single.just(false);
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

		// check that is properly stored
		TestObserver<Vehicle> vehicleObserver = new TestObserver();
		Observable<Vehicle> r = Observable.fromFuture(TestSuite.getInstance().vehicleRepository.get("002", Vehicle.CARS_COLLECTION_NAME).toFuture());
		r.subscribe(vehicleObserver);

		vehicleObserver.assertComplete();
		vehicleObserver.assertNoErrors();
		vehicleObserver.assertValue(re -> {
			testContext.completeNow();
			return re.getModel().equalsIgnoreCase("Auris_updated");
		});

		assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();
	}
}
