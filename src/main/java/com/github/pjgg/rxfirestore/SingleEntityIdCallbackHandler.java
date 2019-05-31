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

import com.google.api.core.ApiFutureCallback;
import com.google.cloud.firestore.DocumentReference;

import io.reactivex.subjects.SingleSubject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SingleEntityIdCallbackHandler<K> implements ApiFutureCallback<DocumentReference> {

	private static Logger LOG = LoggerFactory.getLogger(SingleEntityIdCallbackHandler.class);
	private SingleSubject<K> entityId = SingleSubject.create();

	@Override
	public void onFailure(Throwable throwable) {
		LOG.error(throwable.getMessage());
		entityId.onError(throwable);
	}

	@Override
	public void onSuccess(DocumentReference documentReference) {
		LOG.trace("Blocking firestore SDK response success.");
		entityId.onSuccess((K) documentReference.getId());
	}

	public SingleSubject<K> getEntityId() {
		return entityId;
	}
}
