package me.saket.rxtest.data;

/**
 * The source from where an {@link SourceIdentifiableDrawable} was fetched.
 */
public enum ImageSource {
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