package org.omnaest.react4j.component.form.upload.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class BoundedInputStreamTest
{

    @Test
    public void testReadsExactlyAtLimitOK() throws IOException
    {
        byte[] data = "12345".getBytes();
        try (InputStream stream = new BoundedInputStream(new ByteArrayInputStream(data), 5))
        {
            assertArrayEquals(data, stream.readAllBytes());
        }
    }

    @Test
    public void testOneByteOverThrows()
    {
        byte[] data = "123456".getBytes();
        assertThrows(BoundedInputStream.SizeLimitExceededException.class, () ->
        {
            try (InputStream stream = new BoundedInputStream(new ByteArrayInputStream(data), 5))
            {
                stream.readAllBytes();
            }
        });
    }

    @Test
    public void testCountsAcrossMultipleSmallReads() throws IOException
    {
        byte[] data = "1234567890".getBytes();
        try (InputStream stream = new BoundedInputStream(new ByteArrayInputStream(data), 10))
        {
            byte[] buffer = new byte[3];
            int totalRead = 0;
            int numberOfBytesRead;
            while ((numberOfBytesRead = stream.read(buffer)) != -1)
            {
                totalRead += numberOfBytesRead;
            }
            org.junit.jupiter.api.Assertions.assertEquals(10, totalRead);
        }
    }

    @Test
    public void testCountsAcrossMultipleSmallReadsThrowsWhenExceeded()
    {
        byte[] data = "1234567890X".getBytes();
        assertThrows(BoundedInputStream.SizeLimitExceededException.class, () ->
        {
            try (InputStream stream = new BoundedInputStream(new ByteArrayInputStream(data), 10))
            {
                byte[] buffer = new byte[3];
                while (stream.read(buffer) != -1)
                {
                    // drain
                }
            }
        });
    }

    @Test
    public void testSingleByteReadMode() throws IOException
    {
        byte[] data = "12345".getBytes();
        try (InputStream stream = new BoundedInputStream(new ByteArrayInputStream(data), 5))
        {
            int value;
            int count = 0;
            while ((value = stream.read()) != -1)
            {
                count++;
            }
            org.junit.jupiter.api.Assertions.assertEquals(5, count);
        }
    }

}
