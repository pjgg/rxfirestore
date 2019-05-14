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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.api.core.ApiFutureCallback;

import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;

public class PartialUpdateCallbackHandler implements ApiFutureCallback<HashMap<String, Single<Boolean>>> {

	private SingleSubject<Boolean> updated = SingleSubject.create();

	@Override
	public void onFailure(Throwable throwable) {
		updated.onError(throwable);
	}

	@Override
	public void onSuccess(HashMap<String, Single<Boolean>> result) {
		final AtomicReference<Boolean> allOperationSuccess = new AtomicReference<>();
		allOperationSuccess.set(true);
		for (Map.Entry<String, Single<Boolean>> entry : result.entrySet()) {
			if (allOperationSuccess.get()) {
				entry.getValue().subscribe(isUpdated -> allOperationSuccess.set(isUpdated));
			}
		}

		updated.onSuccess(allOperationSuccess.get());
	}

	public SingleSubject<Boolean> isUpdated() {
		return updated;
	}
}
