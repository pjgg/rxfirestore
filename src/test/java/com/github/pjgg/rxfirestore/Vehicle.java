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

public class Vehicle implements Entity {

	public final static String CARS_COLLECTION_NAME = "cars";
	public final static String BRAND = "brand";
	public final static String MODEL = "model";
	public final static String ELECTRIC = "electric";


	private String id;
	private String eventType;
	private String brand;
	private String model;
	private Boolean electric;

	public Vehicle(){}

	public Vehicle(String brand, String model, Boolean electric){
		this.brand = brand;
		this.model = model;
		this.electric = electric;

	}

	@Override
	public HashMap<String, Object> toMap() {
		return new HashMap<String, Object>(){{put(BRAND,brand);put(MODEL,model);put(ELECTRIC,electric); }};
	}

	@Override
	public String getCollectionName() {
		return CARS_COLLECTION_NAME;
	}

	@Override
	public Entity fromJsonAsMap(Map<String, Object> json) {

		this.brand = (String)json.get(BRAND);
		this.model = (String)json.get(MODEL);
		this.electric = (Boolean)json.get(ELECTRIC);
		this.id = (String)json.get("_id");
		this.eventType = (String)json.get("_eventType");

		return this;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Boolean getElectric() {
		return electric;
	}

	public void setElectric(Boolean electric) {
		this.electric = electric;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
}