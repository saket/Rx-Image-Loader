package me.saket.rxtest.data;

import android.graphics.Bitmap;

import static junit.framework.Assert.assertNotNull;

/**
 * Wraps in a {@link Bitmap} and its source.
 */
public class Image {

    /**
     * The source from where an {@link Image} was fetched.
     */
    public enum Source {
        /**
         * Image was fetched from an in-memory cache.
         */
        MEMORY,

        /**
         * Image was fetched from the disk — where an existing copy
         * of it was present as a cached File.
         */
        DISK,

        /**
         * No cache was found for the image, so it was downloaded
         * from the internet.
         */
        NETWORK
    }

    public Bitmap bitmap;

    /**
     * One of {@link Source#MEMORY}, {@link Source#DISK} and {@link Source#NETWORK}.
     */
    public Source source;

    public Image(Bitmap bitmap, Source source) {
        assertNotNull(bitmap);
        this.bitmap = bitmap;
        this.source = source;
    }

}