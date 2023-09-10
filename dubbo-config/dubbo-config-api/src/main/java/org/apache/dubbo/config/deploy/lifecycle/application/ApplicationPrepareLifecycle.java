/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;

@Activate(order = -4000)
public class ApplicationPrepareLifecycle implements ApplicationLifecycle{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationPrepareLifecycle.class);

    @Override
    public boolean needInitialize() {
        return true;
    }

    @Override
    public void preModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleState) {
        if (!changedModule.isInternal() && moduleState == DeployState.STARTED) {
            prepareApplicationInstance(applicationContext);
        }
    }

    public void prepareApplicationInstance(ApplicationContext applicationContext) {
        if (applicationContext.getHasPreparedApplicationInstance().get()) {
            return;
        }
        if (isRegisterConsumerInstance(applicationContext.getModel().getApplicationConfigManager().getApplicationOrElseThrow())) {
            notifyStartingListener(applicationContext);
        }
    }

    private boolean isRegisterConsumerInstance(ApplicationConfig applicationConfig) {
        Boolean registerConsumer = applicationConfig.getRegisterConsumer();
        if (registerConsumer == null) {
            return true;
        }
        return Boolean.TRUE.equals(registerConsumer);
    }

    private void notifyStartingListener(ApplicationContext applicationContext) {
        if (!applicationContext.getCurrentState().equals(DeployState.STARTING)) {
            return;
        }
        for (DeployListener<ApplicationModel> listener : applicationContext.getDeployListeners()) {
            try {
                if (listener instanceof ApplicationDeployListener) {
                    ((ApplicationDeployListener) listener).onModuleStarted(applicationContext.getModel());
                }
            } catch (Throwable e) {
                logger.error(CONFIG_FAILED_START_MODEL, "", "", applicationContext.getModel().getDesc() + " an exception occurred when handle starting event", e);
            }
        }
    }


}