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

import com.github.pjgg.rxfirestore.exceptions.RxFirestoreExceptions;
import com.google.cloud.firestore.CollectionReference;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.eventbus.EventBus;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.SerializationUtils;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.common.collect.ImmutableList;

import io.vertx.core.json.Json;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

public class FirestoreTemplate extends AbstractVerticle {

	private static final Logger LOG = LoggerFactory.getLogger(FirestoreTemplate.class);

	public static final List<String> SCOPES = ImmutableList.of("https://www.googleapis.com/auth/datastore");
	public static final String TOPIC_INSERT = "FIRESTORE_INSERT";
	public static final String TOPIC_EMPTY = "FIRESTORE_EMPTY";
	public static final String TOPIC_UPSERT = "FIRESTORE_UPSERT";
	public static final String TOPIC_GET = "FIRESTORE_GET";
	public static final String TOPIC_UPDATE = "FIRESTORE_UPDATE";
	public static final String TOPIC_DELETE = "FIRESTORE_DELETE";
	public static final String TOPIC_QUERY = "FIRESTORE_QUERY";
	public static final String TOPIC_CLOSE = "FIRESTORE_CLOSE";
	public static final String TOPIC_QUERY_BUILDER = "FIRESTORE_QUERY_BUILDER";

	private final Firestore firestore;

	public FirestoreTemplate() {

		try {

			String keyPath = Optional.ofNullable(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")).orElseThrow(
				() -> new IllegalArgumentException("GOOGLE_APPLICATION_CREDENTIALS is not set in the environment"));

			firestore = FirestoreOptions.newBuilder().setCredentials(
				GoogleCredentials.fromStream(new FileInputStream(new File(keyPath))).createScoped(SCOPES)).build()
				.getService();

			LOG.trace("GOOGLE_APPLICATION_CREDENTIALS located -> " + keyPath);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void start() {
		EventBus firestoreEventBus = vertx.eventBus();

		MessageConsumer<Object> insertConsumer = firestoreEventBus.localConsumer(TOPIC_INSERT);
		insertConsumer.handler(this::handlerInsert);

		MessageConsumer<Object> emptyConsumer = firestoreEventBus.localConsumer(TOPIC_EMPTY);
		emptyConsumer.handler(this::handlerEmpty);

		MessageConsumer<Object> upsertConsumer = firestoreEventBus.localConsumer(TOPIC_UPSERT);
		upsertConsumer.handler(this::handlerUpsert);

		MessageConsumer<Object> getConsumer = firestoreEventBus.localConsumer(TOPIC_GET);
		getConsumer.handler(this::handlerGet);

		MessageConsumer<Object> updateConsumer = firestoreEventBus.localConsumer(TOPIC_UPDATE);
		updateConsumer.handler(this::handlerUpdate);

		MessageConsumer<Object> deleteConsumer = firestoreEventBus.localConsumer(TOPIC_DELETE);
		deleteConsumer.handler(this::handlerDelete);

		MessageConsumer<byte[]> queryBuilderConsumer = firestoreEventBus.localConsumer(TOPIC_QUERY_BUILDER);
		queryBuilderConsumer.handler(this::handlerQueryBuilder);

		MessageConsumer<byte[]> queryConsumer = firestoreEventBus.localConsumer(TOPIC_QUERY);
		queryConsumer.handler(this::handlerQuery);

		MessageConsumer<Void> closeClient = firestoreEventBus.localConsumer(TOPIC_CLOSE);
		closeClient.handler(this::handlerClose);

	}

	public String insert(final HashMap<String, Object> entity, final String collectionName) {
		LOG.trace("Insert blocking Firestore SDK call. Collection " + collectionName);

		SingleEntityIdCallbackHandler singleEntityId = new SingleEntityIdCallbackHandler<String>();
		ApiFuture<DocumentReference> response = firestore.collection(collectionName).add(entity);

		ApiFutures.addCallback(response, singleEntityId, Runnable::run);
		return (String) singleEntityId.getEntityId().blockingGet();
	}


	public String empty(final String collectionName) {
		LOG.trace("Empty blocking Firestore SDK call. Collection " + collectionName);

		DocumentReference response = firestore.collection(collectionName).document();
		return response.getId();
	}


	public Boolean upsert(final HashMap<String, Object> entity, final String id, final String collectionName) {
		LOG.trace("Upsert blocking Firestore SDK call. Collection " + collectionName);

		UpdateCallbackHandler updateCallbackHandler = new UpdateCallbackHandler();
		ApiFuture<WriteResult> response = firestore.collection(collectionName).document(id).set(entity);
		ApiFutures.addCallback(response, updateCallbackHandler, Runnable::run);
		return updateCallbackHandler.isUpdated().blockingGet();
	}


	public Map<String, Object> get(final String id, final String collectionName) throws RxFirestoreExceptions {
		LOG.trace("Get blocking Firestore SDK call. Collection " + collectionName);

		SingleEntityCallbackHandler entityCallbackHandler = new SingleEntityCallbackHandler();
		ApiFuture<DocumentSnapshot> response = firestore.collection(collectionName).document(id).get();
		ApiFutures.addCallback(response, entityCallbackHandler, Runnable::run);

		return entityCallbackHandler.getEntity().blockingGet();
	}

	public List<Map<String, Object>> get(final Query query) {
		LOG.trace("Query blocking Firestore SDK call. Collection " + query.getCollectionName());

		CollectionReference q = firestore.collection(query.getCollectionName());
		com.google.cloud.firestore.Query queryBuilder;

		if (query.isLimitSet()) {
			queryBuilder = q.limit(query.getLimit());
		} else {
			queryBuilder = q.limit(20);
		}

		if (query.isOffsetSet()) {
			queryBuilder.offset(query.getOffset());
		}

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

		QueryCallbackHandler queryCallbackHandler = new QueryCallbackHandler();
		ApiFuture<QuerySnapshot> response = queryBuilder.get();
		ApiFutures.addCallback(response, queryCallbackHandler, Runnable::run);

		return queryCallbackHandler.getEntities().blockingGet();
	}


	public Query queryBuilder(final String collectionName) {
		LOG.trace("QueryBuilder blocking Firestore SDK call. Collection " + collectionName);

		return new Query(collectionName);
	}


	public Boolean update(final String id, final String collectionName, final HashMap<String, Object> entity) {
		LOG.trace("Update blocking Firestore SDK call. Collection " + collectionName);

		UpdateCallbackHandler updateCallbackHandler = new UpdateCallbackHandler();
		ApiFuture<WriteResult> response = firestore.collection(collectionName).document(id).update(entity);
		ApiFutures.addCallback(response, updateCallbackHandler, Runnable::run);
		return updateCallbackHandler.isUpdated().blockingGet();
	}

	/*
	public Single<Boolean> update(final Precondition precondition, final K id, final E entity) {
		try (Firestore db = firestoreOpts.getService()) {
			ApiFuture<WriteResult> response = db.collection(entity.getCollectionName()).document(id)
					.update(entity.toMap(), precondition);
			ApiFutures.addCallback(response, updateCallbackHandler, rxJavaExecutor);

			return updateCallbackHandler.isUpdated();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}*/


	/*
	public Single<Boolean> update(final K id, final String collectionName, final HashMap<String, Object> fields) {
		try (Firestore db = firestoreOpts.getService()) {
			HashMap<String, Single<Boolean>> result = new HashMap();
			ApiFuture<HashMap<String, Single<Boolean>>> transactionResponse = db.runTransaction(transaction -> {
				for (Map.Entry<String, Object> entry : fields.entrySet()) {
					DocumentReference docRef = db.collection(collectionName).document(id);

					ApiFuture<WriteResult> response = docRef.update(entry.getKey(), entry.getValue());
					ApiFutures.addCallback(response, updateCallbackHandler, rxJavaExecutor);
					result.put(entry.getKey(), updateCallbackHandler.isUpdated());
				}
				return result;
			});

			ApiFutures.addCallback(transactionResponse, partialUpdateCallbackHandler, rxJavaExecutor);
			return partialUpdateCallbackHandler.isUpdated();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}*/


	public Boolean delete(final String id, final String collectionName) {
		LOG.trace("Delete blocking Firestore SDK call. Collection " + collectionName);

		DeleteCallbackHandler deleteCallbackHandler = new DeleteCallbackHandler();
		ApiFuture<WriteResult> response = firestore.collection(collectionName).document(id).delete();
		ApiFutures.addCallback(response, deleteCallbackHandler, Runnable::run);

		return deleteCallbackHandler.isDeleted().blockingGet();
	}

	private void handlerInsert(Message<Object> message) {
		LOG.trace("handler insert operation called.");

		String collectionName = message.headers().get("_collectionName");
		HashMap entity = Json.decodeValue((String) message.body(), HashMap.class);

		String id = insert(entity, collectionName);
		message
			.rxReply(id)
			.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));
	}

	private void handlerEmpty(Message<Object> message) {
		LOG.trace("handler empty operation called.");

		String collectionName = message.headers().get("_collectionName");
		String id = empty(collectionName);

		message.rxReply(id)
			.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));

	}

	private void handlerUpsert(Message<Object> message) {
		LOG.trace("handler upsert operation called.");

		String collectionName = message.headers().get("_collectionName");
		String id = message.headers().get("_id");
		HashMap entity = Json.decodeValue((String) message.body(), HashMap.class);
		Boolean idUpdated = upsert(entity, id, collectionName);

		message.rxReply(idUpdated)
			.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));

	}

	private void handlerGet(Message<Object> message) {
		LOG.trace("handler get operation called.");

		try {
			String collectionName = message.headers().get("_collectionName");
			String id = message.headers().get("_id");
			Map<String, Object> entity = get(id, collectionName);

			message.rxReply(Json.encode(entity))
				.doOnError(err -> message.fail(001, err.getMessage()))
				.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));

		} catch (RxFirestoreExceptions err) {
			message.fail(err.getErrorCode(), err.getMessage());
		}

	}

	private void handlerUpdate(Message<Object> message) {
		LOG.trace("handler update operation called.");

		String collectionName = message.headers().get("_collectionName");
		String id = message.headers().get("_id");
		HashMap entity = Json.decodeValue((String) message.body(), HashMap.class);
		Boolean updated = update(id, collectionName, entity);

		message.rxReply(Json.encode(updated))
			.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));

	}

	private void handlerDelete(Message<Object> message) {
		LOG.trace("handler delete operation called.");

		String collectionName = message.headers().get("_collectionName");
		String id = message.headers().get("_id");
		Boolean deleted = delete(id, collectionName);

		message.rxReply(Json.encode(deleted))
			.subscribe(res -> { /**Do nothing */}, err -> handlerObjectMsgError(message, err));

	}

	private void handlerQueryBuilder(Message<byte[]> message) {
		LOG.trace("handler query operation called.");

		String collectionName = message.headers().get("_collectionName");
		Query query = queryBuilder(collectionName);
		message.rxReply(SerializationUtils.serialize(query))
			.subscribe(res -> { /**Do nothing */}, err -> handlerByteMsgError(message, err));

	}

	private void handlerQuery(Message<byte[]> message) {

		Query query = SerializationUtils.deserialize(message.body());
		List<Map<String, Object>> entityList = get(query);
		message.rxReply(Json.encode(entityList))
			.subscribe(res -> { /**Do nothing */}, err -> handlerByteMsgError(message, err));

	}

	private void handlerByteMsgError(Message<byte[]> message, Throwable err) {
		if (err instanceof ReplyException == false) {
			LOG.error(err.getMessage());
			message.fail(001, err.getMessage());
		}
	}

	private void handlerObjectMsgError(Message<Object> message, Throwable err) {
		if (err instanceof ReplyException == false) {
			LOG.error(err.getMessage());
			message.fail(001, err.getMessage());
		}
	}

	private void handlerClose(Message<Void> message) {
		try {
			firestore.close();
		} catch (Exception e) {
			message.fail(001, e.getMessage());
		}
	}

	/**
	 * To delete a document with some given preconditions.
	 *
	 * @param precondition
	 * @param id
	 * @param collectionName
	 * @return Single boolean
	 */
	/*
	public Single<Boolean> delete(final Precondition precondition, final K id, final String collectionName) {
		try (Firestore db = firestoreOpts.getService()) {
			ApiFuture<WriteResult> response = db.collection(collectionName).document(id).delete(precondition);
			ApiFutures.addCallback(response, deleteCallbackHandler, rxJavaExecutor);

			return deleteCallbackHandler.isDeleted();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}*/

}
