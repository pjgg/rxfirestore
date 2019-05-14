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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Runner is responsible of the verticles deployment and the configuration loading. The configuration is loaded from a
 * YAML file located in ${VERTX_CONFIG_PATH}${VERTX_CONFIG_SLASH}${VERTX_CONFIG_FILE}.
 */
public class Runner {

	public static Vertx run(List<Class<? extends AbstractVerticle>> classes, VertxOptions options,
			List<DeploymentOptions> deploymentOptions) {
		List<String> verticleNames = classes.stream().map(Class::getName).collect(Collectors.toList());
		Consumer<Vertx> runner = vertx -> {
			try {
				for (int i = 0; i < verticleNames.size(); i++) {
					if (deploymentOptions.get(i) != null) {
						vertx.deployVerticle(verticleNames.get(i), deploymentOptions.get(i));
					} else {
						vertx.deployVerticle(verticleNames.get(i));
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		};

		return deployVertx(options, runner);
	}

	private static Vertx deployVertx(VertxOptions options, Consumer<Vertx> runner) {
		Vertx vertx = Vertx.vertx(options);
		runner.accept(vertx);
		return vertx;
	}
}
