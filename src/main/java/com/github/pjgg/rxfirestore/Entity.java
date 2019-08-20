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

import io.vertx.core.json.Json;
import java.util.HashMap;
import java.util.Map;

public interface Entity {

	String getCollectionName();

	/**
	 * Note that your will receive two extra fields _id and _eventType
	 */
	Entity fromJsonAsMap(Map<String, Object> json);

	default String toJson() {
		return Json.encode(this);
	}

}
