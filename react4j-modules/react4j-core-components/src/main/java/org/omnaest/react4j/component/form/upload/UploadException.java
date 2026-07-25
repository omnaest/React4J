package org.omnaest.react4j.component.form.upload;

/**
 * Thrown by {@link UploadChannel#consume(UploadContent)} when the upload violates the channel's size limit, its accepted content types, or fails due to an
 * I/O error.
 *
 * @author omnaest
 */
public class UploadException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public static enum Reason
    {
        SIZE_EXCEEDED, CONTENT_TYPE_REJECTED, IO_ERROR
    }

    private final Reason reason;

    public UploadException(Reason reason, String message)
    {
        super(message);
        this.reason = reason;
    }

    public UploadException(Reason reason, String message, Throwable cause)
    {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason()
    {
        return this.reason;
    }

}
