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
package org.omnaest.react4j.service.internal.handler.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Target implements Supplier<List<String>>
{
    private List<String> target;

    @JsonCreator
    protected Target(List<String> target)
    {
        super();
        this.target = target;
    }

    @JsonValue
    @Override
    public List<String> get()
    {
        return this.target;
    }

    public static Target from(Supplier<List<String>> supplier)
    {
        return new Target(supplier.get());
    }

    @Override
    public String toString()
    {
        return "Target [target=" + this.target + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.target == null) ? 0 : this.target.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        Target other = (Target) obj;
        if (this.target == null)
        {
            if (other.target != null)
            {
                return false;
            }
        }
        else if (!this.target.equals(other.target))
        {
            return false;
        }
        return true;
    }

    public boolean isEmpty()
    {
        return this.get()
                   .isEmpty();
    }

    public static Target empty()
    {
        return Target.from(() -> new ArrayList<>());
    }

}
