/**
 * Copyright 2018 The original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dekorate.servicecatalog.handler;

import io.dekorate.Handler;
import io.dekorate.Resources;
import io.dekorate.kubernetes.config.Configuration;
import io.dekorate.deps.servicecatalog.api.model.ServiceBindingBuilder;
import io.dekorate.deps.servicecatalog.api.model.ServiceInstanceBuilder;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.servicecatalog.config.Parameter;
import io.dekorate.servicecatalog.config.ServiceCatalogConfig;
import io.dekorate.servicecatalog.config.ServiceCatalogInstance;
import io.dekorate.utils.Strings;
import io.dekorate.servicecatalog.config.EditableServiceCatalogConfig;
import io.dekorate.doc.Description;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Description("Adds service instance and binding and inject binding info to container environment.")
public class ServiceCatalogHandler implements Handler<ServiceCatalogConfig> {

  private final Resources resources;

  // only used for testing
  ServiceCatalogHandler() {
    this(new Resources());
  }

  public ServiceCatalogHandler(Resources resources) {
    this.resources = resources;
  }

  @Override
  public int order() {
    return 410;
  }

  @Override
  public void handle(ServiceCatalogConfig config) {
    for (ServiceCatalogInstance instance : config.getInstances()) {
      resources.add(new ServiceInstanceBuilder()
                    .withNewMetadata()
                    .withName(instance.getName())
                    .endMetadata()
                    .withNewSpec()
                    .withClusterServiceClassExternalName(instance.getServiceClass())
                    .withClusterServicePlanExternalName(instance.getServicePlan())
                    .withParameters(toMap(instance.getParameters()))
                    .endSpec()
                    .build());

      if (Strings.isNotNullOrEmpty(instance.getBindingSecret())) {
        resources.add(new ServiceBindingBuilder()
                      .withNewMetadata()
                      .withName(instance.getName())
                      .endMetadata()
                      .withNewSpec()
                      .withNewInstanceRef(instance.getName())
                      .withSecretName(instance.getBindingSecret())
                      .endSpec()
                      .build());

        resources.decorate(new AddEnvVarDecorator(resources.getName(), resources.getName(), new EnvBuilder().withSecret(instance.getBindingSecret()).build()));
      }
    }
  }

  public boolean canHandle(Class<? extends Configuration> type) {
    return type.equals(ServiceCatalogConfig.class) ||
      type.equals(EditableServiceCatalogConfig.class);
  }


  /**
   * Converts an array of {@link Parameter} to a {@link Map}.
   * @param parameters    The parameters.
   * @return              A map.
   */
  protected static Map<String, Object> toMap(Parameter... parameters) {
    return Arrays.asList(parameters).stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
  }
}
