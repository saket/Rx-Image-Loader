package me.saket.rxtest.data;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Holds objects temporarily — until the app gets killed.
 */
public class MemoryBitmapCache implements BitmapCache {

    /**
     * This is ~500KB on a Moto G3, good enough for storing 10k Strings.
     */
    public static final int CACHE_SIZE_BYTES
            = (int) (Runtime.getRuntime().maxMemory() / 1024 / 2000);

    private LruCache<String, Bitmap> mCache = new LruCache<>(CACHE_SIZE_BYTES);
    private static MemoryBitmapCache sMemoryCache;

    public static MemoryBitmapCache getInstance() {
        if (sMemoryCache == null) {
            sMemoryCache = new MemoryBitmapCache();
        }
        return sMemoryCache;
    }

    @Override
    public String getName() {
        return "Memory Cache";
    }

    @Override
    public boolean containsKey(String key) {
        return mCache.get(key) != null;
    }

    @Override
    public Bitmap get(String key) {
        return mCache.get(key);
    }

    @Override
    public void save(String key, Bitmap bitmapToSave) {
        mCache.put(key, bitmapToSave);
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }

}