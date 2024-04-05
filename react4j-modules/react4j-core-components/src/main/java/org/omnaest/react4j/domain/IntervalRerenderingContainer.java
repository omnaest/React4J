/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.domain;

import java.util.concurrent.TimeUnit;

import org.omnaest.react4j.domain.support.UIComponentGenericFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;

/**
 * Creates a UI wrapper component that does refresh its node periodically by calling the subnode rerendering endpoint of the backend. This is useful e.g. for
 * progress bars etc.
 * 
 * @author omnaest
 */
public interface IntervalRerenderingContainer extends UIComponent<IntervalRerenderingContainer>
{
    /**
     * Defines the interval duration. Default is 1 second.
     * 
     * @param intervalDuration
     * @param timeUnit
     * @return
     */
    public IntervalRerenderingContainer withIntervalDuration(int intervalDuration, TimeUnit timeUnit);

    public IntervalRerenderingContainer withRefreshedContent(UIComponentProvider<?> uiComponentProvider);

    public IntervalRerenderingContainer withRefreshedContent(UIComponentGenericFactoryFunction<ActiveRefreshStateControl> uiComponentFactory);

    /**
     * Allows to control the state of the {@link IntervalRerenderingContainer}
     * 
     * @author omnaest
     */
    public static interface ActiveRefreshStateControl
    {
        /**
         * Sends a disable signal, so that the refresh cycle stops. This can not be reactivated.
         * 
         * @return
         */
        public ActiveRefreshStateControl disable();
    }

}
