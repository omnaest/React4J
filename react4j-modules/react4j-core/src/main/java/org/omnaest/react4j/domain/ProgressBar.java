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

public interface ProgressBar extends UIComponent<ProgressBar>
{
    public ProgressBar withText(String text);

    public ProgressBar withNonTranslatedText(String text);

    /**
     * Displays the progress ratio as percentage as text
     * 
     * @return
     */
    public ProgressBar withRatioText();

    /**
     * Direct alternative to {@link #withValue(double)} to define the progress. <br>
     * <br>
     * 0.0 = 0%<br>
     * 0.1 = 10%<br>
     * 0.9 = 90%<br>
     * 1.0 = 100%
     * 
     * @see #withValue(double)
     * @param value
     * @return
     */
    public ProgressBar withProgressRatio(double value);

    /**
     * Defines together with {@link #withMinimum(double)} and {@link #withMaximum(double)} the progress ratio. Consider using {@link #withProgressRatio(double)}
     * as a more direct alternative.
     * 
     * @see #withProgressRatio(double)
     * @see #withMinimum(double)
     * @see #withMaximum(double)
     * @param value
     * @return
     */
    public ProgressBar withValue(double value);

    /**
     * Defines the minimum value. Default is 0.
     * 
     * @param minimum
     * @return
     */
    public ProgressBar withMinimum(double minimum);

    /**
     * Defines the maximum value. Default is 100.
     * 
     * @param maximum
     * @return
     */
    public ProgressBar withMaximum(double maximum);
}
