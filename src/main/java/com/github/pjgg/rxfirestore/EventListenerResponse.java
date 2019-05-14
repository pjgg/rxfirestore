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

import com.google.cloud.firestore.ListenerRegistration;

import io.reactivex.Flowable;

public class EventListenerResponse<E extends Entity> {

	private Flowable<E> eventsFlow;

	private ListenerRegistration registration;

	public EventListenerResponse(Flowable<E> e, ListenerRegistration r) {
		eventsFlow = e;
		registration = r;
	}

	public Flowable<E> getEventsFlow() {
		return eventsFlow;
	}

	public void setEventsFlow(Flowable<E> eventsFlow) {
		this.eventsFlow = eventsFlow;
	}

	public ListenerRegistration getRegistration() {
		return registration;
	}

	public void setRegistration(ListenerRegistration registration) {
		this.registration = registration;
	}
}
