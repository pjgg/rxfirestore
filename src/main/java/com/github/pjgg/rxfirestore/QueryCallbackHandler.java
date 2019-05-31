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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import io.reactivex.subjects.SingleSubject;

public class QueryCallbackHandler implements ApiFutureCallback<QuerySnapshot> {

	private static Logger LOG = LoggerFactory.getLogger(UpdateCallbackHandler.class);
	private SingleSubject<List<Map<String, Object>>> entities = SingleSubject.create();

	@Override
	public void onFailure(Throwable throwable) {
		LOG.error(throwable.getMessage());
		entities.onError(throwable);
	}

	@Override
	public void onSuccess(QuerySnapshot futureDocuments) {
		LOG.trace("Blocking firestore SDK response success.");
		List<Map<String, Object>> result = new ArrayList<>();
		List<QueryDocumentSnapshot> documents = futureDocuments.getDocuments();
		for (DocumentSnapshot document : documents) {
			Map<String, Object> data = document.getData();
			data.put("_id", Optional.ofNullable(document.getId()).orElse("NONE"));
			result.add(data);
		}

		entities.onSuccess(result);
	}

	public SingleSubject<List<Map<String, Object>>> getEntities() {
		return entities;
	}
}
