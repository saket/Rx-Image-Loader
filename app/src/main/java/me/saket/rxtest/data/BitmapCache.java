package me.saket.rxtest.data;

import android.graphics.Bitmap;

/**
 * Interface for a fixed-size local storage.
 * Implemented by {@link MemoryBitmapCache} and {@link DiskBitmapCache}.
 */
public interface BitmapCache {

    /**
     * For debugging
     */
    String getName();

    /**
     * Returns the source of this cache.
     */
    Image.Source getSource();

    /**
     * Whether any object with <var>key</var> exists
     */
    boolean containsKey(String key);

    /**
     * Gets the object mapped against <var>key</var>.
     */
    Bitmap get(String key);

    /**
     * Saves <var>bitmapToSave</var> against <var>key</var>.
     */
    void save(String key, Bitmap bitmapToSave);

    /**
     * Deletes everything in this cache.
     */
    void clear();

}