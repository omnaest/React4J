package org.omnaest.react4j.component.form.internal.renderer.node.element;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormRangeNode
{
    @JsonProperty
    private String min;

    @JsonProperty
    private String max;

    @JsonProperty
    private String step;

    public String getMin()
    {
        return this.min;
    }

    public FormRangeNode setMin(String min)
    {
        this.min = min;
        return this;
    }

    public String getMax()
    {
        return this.max;
    }

    public FormRangeNode setMax(String max)
    {
        this.max = max;
        return this;
    }

    public String getStep()
    {
        return this.step;
    }

    public FormRangeNode setStep(String step)
    {
        this.step = step;
        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("FormRangeNode [min=")
               .append(this.min)
               .append(", max=")
               .append(this.max)
               .append(", step=")
               .append(this.step)
               .append("]");
        return builder.toString();
    }

}