package org.omnaest.react4j.domain.context.document;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.omnaest.utils.stream.Streamable;

public interface DocumentList extends Streamable<Document>
{
    /**
     * Returns the {@link Document} at the given index position. Index = 0,1,2, ...
     * 
     * @param index
     * @return
     */
    public Document get(int index);

    /**
     * Returns the first {@link Document}
     * 
     * @return
     */
    public default Document getFirstDocument()
    {
        return this.get(0);
    }

    /**
     * Returns the number of elements
     * 
     * @return
     */
    public int size();

    @Override
    default Stream<Document> stream()
    {
        return IntStream.range(0, this.size())
                        .mapToObj(this::get);
    }

}