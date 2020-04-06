# RxFirestore
[![Build Status](https://travis-ci.org/pjgg/rxfirestore.svg?branch=master)](https://travis-ci.org/pjgg/rxfirestore)
[![GitHub release](https://img.shields.io/maven-central/v/com.github.pjgg/rxfirestore.svg?color=dark%20green)](https://github.com/pjgg/rxfirestore/releases)
[![GitHub license](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/pjgg/rxfirestore/blob/master/LICENSE.txt)

<img align="right" src="https://github.com/pjgg/rxfirestore/blob/master/firestoreLogo.png">

RxFirestore is a [Firestore](https://cloud.google.com/firestore/) SDK written in a reactive way.
We have thought about it for server microservices, not to persist the state of your application, directly from the device.
You could have a look our [Motivation](#motivation) in order to understand WHY or  [design approach](#design-approach) to understand HOW.

## Index

- [Current state](#current-state)
- [Motivation](#motivation)
- [Design approach Version 1.0.5](#design-approach)
- [Minimum Requirements](#minimum-requirements)
- [Maven useful commands](#maven-useful-commands)
- [How to use it](#How-to-use-it)
- [API methods](#API-methods)
  - [Insert](#insert)
  - [Empty](#empty)
  - [Upsert](#upsert)
  - [Get](#get)
  - [Query Builder](#query-builder)
  - [Run Query](#run-query)
  - [Add Query Listener](#add-query-listener)
  - [Update](#update)
  - [Delete](#delete)

## Current State

RxFirestore is in its early stages, and we are actively looking for partner organizations and individuals to contribute to the project. Since the code is in active development, please do thorough testing and verification before implementing.

## Contact Info

If you would like to discuss any point do not hesitate to contact us through email pablo.gonzalez.granados@gmail.com.

## Motivation

Java Firestore SDK provided by Google is implemented in a traditional way, through OS kernel threads. This is a very expensive way to give us a concurrency abstraction.
This implementation, fails to meet today's requirement of concurrency, in particular threads cannot match the scale of the domain’s unit of concurrency. For example,
applications usually allow up to millions of transactions, users or sessions. However, the number of threads supported by the kernel is much less. Thus, a Thread for every user,
transaction, or session is often not feasible. To sum up, OS kernel threads is insufficient for meeting modern demands, and wasteful in computing resources that are particularly valuable in the cloud.

### Design approach

Our first solution to meet motivation requirements, is the use of asynchronous concurrent APIs. Common examples are CompletableFuture and RxJava.
Provided that such APIs don’t block the kernel thread, it gives an application a finer-grained concurrency construct on top of Java threads. However, at the end of the day we will have to use the blocking SDK defined by google,
so we have to think in a manner that allow us to mitigate use OS kernel threads in a per request/user way, and at the same time allow us to scale in order to meet today’s requirement of concurrency.

<img align="center" src="https://github.com/pjgg/rxfirestore/blob/master/RxFirestoreSDK.png">

We've decided to handler all the user request through a reactive facade that will dispatch all the event to a in memory event bus (a buffer) in order to handler backpreasure and decouples I/O computation from the thread that invoked the operation.
This event bus will be consumed by a Vertx Actor (Worker Verticle), executing all of this commands in a synchronous or asynchronous way, running within a ThreadPool.

## Minimum Requirements

-   Java 1.8+
-   Maven 3.5.3

## Maven useful commands

* build RxFirestore project: ```mvn clean install -DskipTests```

## How to use it

1. Add in your pom the following dependency:

```
<dependency>
  <groupId>com.github.pjgg</groupId>
  <artifactId>rxfirestore</artifactId>
  <version>1.0.5</version>
</dependency>
```

If your project is packaged as a fatjar consider to use `ServicesResourceTransformer`.
JAR files providing implementations of some interfaces often ship with a META-INF/services/ directory that maps interfaces to their implementation classes for lookup by the service locator.
To relocate the class names of these implementation classes, and to merge multiple implementations of the same interface into one service entry use the ServicesResourceTransformer.

example:
```
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.2.1</version>
        <executions>
          <execution>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
             	<finalName>your-app-with-dependencies</finalName>
             	<transformers>
             	<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
             	<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
             	    <mainClass>${main.class}</mainClass>
             	</transformer>
             	</transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  ...
</project>

```


2. Create your own repository and extends `RxFirestoreSdk`. You must provide your entity model as parameters.

for example:
Imagine that you have a garage, and you would like to manage your vehicles catalog.

```
public class VehicleRepository extends RxFirestoreSDK<Vehicle> {

	public VehicleRepository() {
		super(Vehicle::new);
	}

}
```

In case you are running this SDK under Vertx toolbox you could pass your vertx context as a parameter, and by this way will be reused.
```
public class VehicleRepository extends RxFirestoreSDK<Vehicle> {

	public VehicleRepository() {
		super(Vehicle::new, Vertx.currentContext().owner());
	}
}
```

3. Add `GOOGLE_APPLICATION_CREDENTIALS` environment variable to your project, pointing to your keyfile.json
4. *(Optional)* Add `DB_THREAD_POOL_SIZE` environment variable to your project. Default value is set to the amount of cores * 2.
5. Create your entity model

All entities must extend `Entity` interface and implements `getCollectionName` and `fromJsonAsMap`

* getCollectionName: Must return you firestore collection name
* fromJsonAsMap: will receive a JSON as a Map format and must return a Java entity

Example:

```
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

import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.Json;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;

public class Vehicle implements Entity {

	public final static String CARS_COLLECTION_NAME = "cars";
	public final static String BRAND = "brand";
	public final static String MODEL = "model";
	public final static String ELECTRIC = "electric";
	public final static String DISPLACEMENT = "displacement";

	private String id;
	private String eventType;

	@JsonProperty(value="brandy")
	private String brand;
	private String model;
	private Boolean electric;
	private Number displacement;

	@JsonFormat(shape = Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	private Date createdDate;

	public Vehicle() {
	}

	public Vehicle(String brand, String model, Boolean electric) {
		this.brand = brand;
		this.model = model;
		this.electric = electric;
		this.displacement = 0;
		this.createdDate = new Date();
	}

	@Override
	public String getCollectionName() {
		return CARS_COLLECTION_NAME;
	}

	@Override
	public Entity fromJsonAsMap(Map<String, Object> json) {

		this.brand = (String) json.getOrDefault(BRAND, "NONE");
		this.model = (String) json.getOrDefault(MODEL, "NONE");
		this.electric = (Boolean) json.getOrDefault(ELECTRIC, false);

		this.displacement = (Number) json.getOrDefault("displacement", 0);
		this.id = (String) json.getOrDefault("_id", "NONE");
		this.eventType = (String) json.getOrDefault("_eventType", "NONE");

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

	public Number getDisplacement() {
		return displacement;
	}

	public void setDisplacement(Number displacement) {
		this.displacement = displacement;
	}
}

```


*Note:* As you have notice we support Jackson annotations in order to define data types and field names.

## API methods

### Insert

Insert create a Document with an auto-generate ID. Firestore auto-generated IDs do not provide any automatic
ordering. If you want to be able to order your documents by creation date, you should store a timestamp as a
field in the documents.

```
Single<String> insert(final E entity)
```

### Empty

Empty create a document for a given collection, and return an an auto-generate ID.
In some cases, it can be useful to create a document reference with an auto-generated ID,
then use the reference later through a upsert method.

```
Single<String> empty(final String collectionName)
```

### Upsert

If the document does not exist, it will be created. If the document does exist, its contents will be overwritten
with the newly provided data.

When you use upsert to create or update a document, you must specify an ID for the document. But sometimes there
isn't a meaningful ID for the document, and it's more convenient to let Cloud Firestore auto-generate an ID for
you. You can do this by calling empty.

```
Single<Boolean> upsert(final String id, final String collectionName, final E entity)
```

### Get

Get will retrieve a Document by ID for a given collection name.

```
Single<E> get(final String id, final String collectionName)
```

### Query Builder

Query builder allow you to develop your own query with where statement. Use in combination with get in order to
develop complex inferences.

```
Single<Query> queryBuilder(final String collectionName)
```

example:
```
var query = carsRepository.queryBuilder(CarModel.CARS_COLLECTION_NAME).whereEqualTo("brand","Toyota");
```


### Run Query

get will retrieve a List of Documents by a given query.

```
Single<List<E>> get(Query query)
```

### Add Query Listener

addQueryListener, You can listen to a document changes (create, update and delete).
```
EventListenerResponse<E> addQueryListener(final Query query, final Optional<EventListener<QuerySnapshot>> eventsHandler)
```

EventListenerResponse will allow you subscribe to a query `listener.getEventsFlow().subscribe(event -> System.out.println("Event Type:"+ event.getEventType() + " model: " + event.getModel()));` and also close your connection `listener.getRegistration().remove();`

*Note:* this method is a *BLOCKING* operation, so a new thread will be created per listener.

### Update

Update full document (overwrite).

```
 Single<Boolean> update(final String id, final String collectionName, final E entity)
```


### Delete

To delete a document, use the delete method. Deleting a document does not delete its subcollections!

```
Single<Boolean> delete(final String id, final String collectionName)
```

