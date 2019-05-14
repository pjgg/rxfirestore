
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

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.ListenerRegistration;
import io.reactivex.subjects.SingleSubject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;

import io.vertx.reactivex.core.Vertx;

public class BlockingFirestoreTemplate<E extends Entity> {

	private final Supplier<? extends Entity> supplier;
	private final Firestore firestore;
	private final SingleSubject<Vertx> vertx;

	public BlockingFirestoreTemplate(Supplier<? extends Entity> entityConstructor, SingleSubject<Vertx> vertxSubject) {
		supplier = Objects.requireNonNull(entityConstructor);
		this.vertx = vertxSubject;

		try {

			String keyPath = Optional.ofNullable(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")).orElseThrow(
					() -> new IllegalArgumentException("GOOGLE_APPLICATION_CREDENTIALS is not set in the environment"));

			firestore = FirestoreOptions.newBuilder().setCredentials(
					GoogleCredentials.fromStream(new FileInputStream(new File(keyPath))).createScoped(
						FirestoreTemplate.SCOPES)).build()
					.getService();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * addQueryListener, You can listen to a document changes (create, update and delete).
	 *
	 * @param query to subscribe. Build your query with queryBuilder method.
	 * @param eventsHandler will handler document changes. By default we provide an eventHandler that will give you a
	 * Flowable with all the document changes.
	 * @return EventListenerResponse, contains two object. "registration" will allow you to close the event flow
	 * @throws TimeoutException default timeout after 10 seconds
	 * @throws ExecutionException if something weird happens
	 * @throws InterruptedException if something weird happens as somebody break connection.
	 * <p>
	 * example:
	 * <p>
	 * {@code listener.getRegistration().remove();}
	 * <p>
	 * "eventsFlow" represent a flow of changes. Firstly you will get all the events that match with your query,and then
	 * all the changes until you close your listener.
	 * <p>
	 * example:
	 * <p>
	 * {@code
	 * listener.getEventsFlow().subscribe(event ->
	 * System.out.println("Event Type:"+ event.getEventType() + " model: " +
	 * event.getModel()));}
	 */

	public EventListenerResponse<E> addQueryListener(final Query query,
			final Optional<EventListener<QuerySnapshot>> eventsHandler)
			throws InterruptedException, ExecutionException, TimeoutException {

		CompletableFuture<EventListenerResponse<E>> fut = new CompletableFuture<>();
		vertx.subscribe(vertx -> {
			vertx.executeBlocking(future -> {
				final DefaultEventListener<E> defaultHandler = new DefaultEventListener<E>(supplier.get());
				CollectionReference q = firestore.collection(query.getCollectionName());
				com.google.cloud.firestore.Query queryBuilder;

				queryBuilder = q.offset(0);

				HashMap<String, Object> equalTo = query.getEqualTo();
				Iterator equalToIt = equalTo.entrySet().iterator();
				while (equalToIt.hasNext()) {
					Map.Entry pair = (Map.Entry) equalToIt.next();
					queryBuilder = queryBuilder.whereEqualTo((String) pair.getKey(), pair.getValue());
				}

				HashMap<String, Object> arrayContains = query.getArrayContains();
				Iterator arrayContainsIt = arrayContains.entrySet().iterator();
				while (arrayContainsIt.hasNext()) {
					Map.Entry pair = (Map.Entry) arrayContainsIt.next();
					queryBuilder = queryBuilder.whereArrayContains((String) pair.getKey(), pair.getValue());
				}

				HashMap<String, Object> greaterThan = query.getGreaterThan();
				Iterator greaterThanIt = greaterThan.entrySet().iterator();
				while (greaterThanIt.hasNext()) {
					Map.Entry pair = (Map.Entry) greaterThanIt.next();
					queryBuilder = queryBuilder.whereGreaterThan((String) pair.getKey(), pair.getValue());
				}

				HashMap<String, Object> lessThan = query.getLessThan();
				Iterator lessThanIt = lessThan.entrySet().iterator();
				while (lessThanIt.hasNext()) {
					Map.Entry pair = (Map.Entry) lessThanIt.next();
					queryBuilder = queryBuilder.whereLessThan((String) pair.getKey(), pair.getValue());
				}

				ListenerRegistration listener = queryBuilder.addSnapshotListener(eventsHandler.orElse(defaultHandler));
				fut.complete(new EventListenerResponse<E>(defaultHandler.getSource(), listener));

			}, result -> {});
		});

		return fut.get(10, TimeUnit.SECONDS);
	}

	public Query queryBuilder(final String collectionName) {
		return new Query(collectionName);
	}

}
