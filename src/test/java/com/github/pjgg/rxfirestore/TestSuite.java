package com.github.pjgg.rxfirestore;

import io.vertx.reactivex.core.Vertx;

public class TestSuite {

	private static TestSuite INSTANCE = null;
	private static final Byte OBJLOCK = new Byte((byte) 0);

	Vertx vertx;
	VehicleRepository vehicleRepository;

	private TestSuite(){
		vertx = Vertx.vertx();
		vehicleRepository = new VehicleRepository(vertx);
	}

	public static TestSuite getInstance() {
		if(INSTANCE == null) {
			synchronized(OBJLOCK) {
				if (INSTANCE == null) {
					INSTANCE = new TestSuite();
				}
			}
		}
		return INSTANCE;
	}

	public Vertx getVertx() {
		return vertx;
	}

	public VehicleRepository getVehicleRepository() {
		return vehicleRepository;
	}
}
