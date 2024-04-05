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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public interface Icon extends UIComponent<Icon>
{
    public Icon from(StandardIcon value);

    public static enum StandardIcon implements Supplier<String>
    {
        ENVELOPE("envelope-open-text"), DOLLAR_SIGN("dollar-sign"), MICROSCOPE("microscope"), HEARTBEAT("heartbeat"), DNA("dna");

        private String key;

        private StandardIcon(String key)
        {
            this.key = key;
        }

        @Override
        public String get()
        {
            return key;
        }

        public static Optional<StandardIcon> of(String value)
        {
            return Arrays.asList(values())
                         .stream()
                         .filter(icon -> icon.name()
                                             .equals(value))
                         .findFirst();
        }
    }
}
