package org.omnaest.react4j.service.internal.service.internal.translation.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;
import org.omnaest.utils.FileUtils;

public class TranslationFileServiceTest
{
    private TranslationFileService translationFileService = new TranslationFileService()
    {
        {
            this.translationFolder = FileUtils.createRandomTempDirectoryQuietly()
                                              .get()
                                              .getAbsolutePath();
        }
    };

    @Test
    public void testPersist() throws Exception
    {
        assertFalse(this.translationFileService.getTranslator(Locale.US, Locale.GERMAN)
                                               .isPresent());
        this.translationFileService.persist("key1", Locale.US, "I love you!", Locale.GERMAN, "Ich liebe Dich!");

        this.translationFileService.synchronize();

        assertTrue(this.translationFileService.getTranslator(Locale.US, Locale.GERMAN)
                                              .isPresent());

        assertEquals("Ich liebe Dich!", this.translationFileService.getTranslator(Locale.US, Locale.GERMAN)
                                                                   .flatMap(t -> t.apply("key1", "I love you!"))
                                                                   .orElse(null));
    }

}
