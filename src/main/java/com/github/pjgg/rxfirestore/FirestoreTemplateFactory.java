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

import io.reactivex.subjects.SingleSubject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.netty.channel.DefaultChannelId;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;

public enum FirestoreTemplateFactory {

	INSTANCE;

	private static final long MAX_EXECUTION_TIME_SEC = 30;

	private EventBus eventBus;
	private SingleSubject<Vertx> vertxSubject = SingleSubject.create();

	public void init(Vertx... vertxArg) {

		EventBusOptions eventBusOpt = new EventBusOptions();

		// Deployment options to set the verticle as a worker
		String poolsize = Optional.ofNullable(System.getenv("DB_THREAD_POOL_SIZE")).orElse("");
		int dbThreadPoolSize =
				poolsize.isEmpty() ? Runtime.getRuntime().availableProcessors() * 2 : Integer.parseInt(poolsize);

		if (poolsize.isEmpty()) {
			System.out.println("DB_THREAD_POOL_SIZE environment variable not found. Default value " + dbThreadPoolSize);
		}

		DeploymentOptions firestoreWorkerDeploymentOptions = new DeploymentOptions().setWorker(true)
				.setInstances(dbThreadPoolSize) // matches the worker pool size below
				.setWorkerPoolName("rxfirestore-worker-pool")
				.setWorkerPoolSize(dbThreadPoolSize)
				.setMaxWorkerExecuteTime(MAX_EXECUTION_TIME_SEC)
				.setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS);

		if (vertxArg.length == 0) {
			// Hack in order to avoid a noisy blocked thread exception at initialization time. Only happens once.
			DefaultChannelId.newInstance();

			List<Class<? extends AbstractVerticle>> verticleList = Arrays.asList(FirestoreTemplate.class);
			List<DeploymentOptions> deploymentOptionsList = Arrays.asList(firestoreWorkerDeploymentOptions);

			Vertx vertxInstance = Runner
					.run(verticleList, new VertxOptions().setEventBusOptions(eventBusOpt), deploymentOptionsList);
			eventBus = vertxInstance.eventBus();
			vertxSubject.onSuccess(vertxInstance);
		} else {
			vertxArg[0].deployVerticle(FirestoreTemplate.class.getName(), firestoreWorkerDeploymentOptions);
			eventBus = vertxArg[0].eventBus();
			vertxSubject.onSuccess(vertxArg[0]);
		}
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public SingleSubject<Vertx> getVertx() {
		return vertxSubject;
	}
}
