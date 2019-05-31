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
import java.util.Map;
import java.util.Optional;

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.firestore.DocumentSnapshot;

import io.reactivex.subjects.SingleSubject;

public class SingleEntityCallbackHandler implements ApiFutureCallback<DocumentSnapshot> {

	private static Logger LOG = LoggerFactory.getLogger(SingleEntityCallbackHandler.class);
	private SingleSubject<Map<String, Object>> entity = SingleSubject.create();

	@Override
	public void onFailure(Throwable throwable) {
		LOG.error(throwable.getMessage());
		entity.onError(throwable);
	}

	@Override
	public void onSuccess(DocumentSnapshot document) {
		LOG.trace("Blocking firestore SDK response success.");
		if (document.exists()) {
			Map<String, Object> data = document.getData();
			data.put("_id", Optional.ofNullable(document.getId()).orElse("NONE"));
			entity.onSuccess(data);
		} else {
			entity.onError(new RuntimeException("Not Found"));
		}
	}

	public SingleSubject<Map<String, Object>> getEntity() {
		return entity;
	}

}
