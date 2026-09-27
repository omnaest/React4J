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
package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.handler.domain.Target;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire DTO for {@link org.omnaest.react4j.domain.DropTarget} (plan-235 S1). {@code dragIdFieldKey} /
 * {@code relationFieldKey} are the submitted-{@code Data} field keys the client must write the drop's dragId
 * and relation into BEFORE dispatching {@code dropTarget} (Cliff 1' - generated server-side from this node's
 * own {@code Location}, never a shared/magic string) - both {@code null} when no {@code onDrop} handler was
 * registered, exactly mirroring {@code dropTarget} being emitted as an empty {@link Target} in that case.
 */
public class DropTargetNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "DROPTARGET";

    @JsonProperty
    private Node   content;

    @JsonProperty
    private String dragIdFieldKey;

    @JsonProperty
    private String relationFieldKey;

    @JsonProperty
    private Target dropTarget;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public DropTargetNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public String getDragIdFieldKey()
    {
        return this.dragIdFieldKey;
    }

    public DropTargetNode setDragIdFieldKey(String dragIdFieldKey)
    {
        this.dragIdFieldKey = dragIdFieldKey;
        return this;
    }

    public String getRelationFieldKey()
    {
        return this.relationFieldKey;
    }

    public DropTargetNode setRelationFieldKey(String relationFieldKey)
    {
        this.relationFieldKey = relationFieldKey;
        return this;
    }

    public Target getDropTarget()
    {
        return this.dropTarget;
    }

    public DropTargetNode setDropTarget(Target dropTarget)
    {
        this.dropTarget = dropTarget;
        return this;
    }
}
