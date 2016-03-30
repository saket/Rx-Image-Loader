package me.saket.rxtest.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Helper class for loading photos.
 * Start at {@link #load(String)}.
 */
public class PhotoLoader {

    private static final String TAG = "Photos";
    private static final Object LOCK = new Object();
    private static volatile PhotoLoader sPhotoLoader;

    private final MemoryBitmapCache mMemoryCache;
    private final DiskBitmapCache mDiskCache;
    private NetworkClient mNetworkClient;

    public PhotoLoader(MemoryBitmapCache memoryCache, DiskBitmapCache diskCache,
                       NetworkClient networkClient) {
        mMemoryCache = memoryCache;
        mDiskCache = diskCache;
        mNetworkClient = networkClient;
    }

    public static PhotoLoader getInstance(Context context) {
        if (sPhotoLoader == null) {
            synchronized (LOCK) {
                // Another null check is required if a 2nd thread manages to get
                // queued for this synchronized block while the 1st thread was
                // already executing inside this block, instantiating the object.
                if (sPhotoLoader == null) {
                    sPhotoLoader = new PhotoLoader(
                            MemoryBitmapCache.getInstance(),
                            DiskBitmapCache.getInstance(context),
                            NetworkClient.getInstance()
                    );
                }
            }
        }
        return sPhotoLoader;
    }

    /**
     * Loads a photo from one of these 3 sources:
     * 1. Memory cache — if it was downloaded in the current session
     * 2. Disk cache   — if it was ever downloaded
     * 3. Network      — if it was never downloaded
     */
    public Observable<Bitmap> load(String imageUrl) {
        return Observable.just(imageUrl).flatMap(findImageSource());
    }

    private Func1<String, Observable<Bitmap>> findImageSource() {
        return imageUrl -> {
            Log.d(TAG, "Loading image Url: " + imageUrl);

            // Plan A: Check in memory
            final Observable<Bitmap> memoryCacheLoadObs = loadFromCache(imageUrl, mMemoryCache);

            // Plan B: Look into files
            final Observable<Bitmap> diskImageObservable = loadFromCache(imageUrl, mDiskCache)
                    .doOnNext(saveToCache(imageUrl, mMemoryCache));

            // Plan C: Hit the network
            final Observable<Bitmap> networkLoadObs = mNetworkClient
                    .loadImage(imageUrl)
                    // Save into both memory and disk cache for future calls
                    .doOnNext(saveToCache(imageUrl, mMemoryCache))
                    .doOnNext(saveToCache(imageUrl, mDiskCache))
                    .doOnNext(bitmap -> {
                        Log.i(TAG, "Loading from: Internet");
                    });

            return Observable.amb(memoryCacheLoadObs, diskImageObservable, networkLoadObs);
        };
    }

    private Action1<Bitmap> saveToCache(String imageUrl, BitmapCache bitmapCache) {
        return bitmap -> {
            if (bitmap == null) {
                return;
            }

            Log.i(TAG, "Saving to: " + bitmapCache.getName());
            bitmapCache.save(imageUrl, bitmap);
        };
    }

    private Observable<Bitmap> loadFromCache(String imageUrl, BitmapCache whichBitmapCache) {
        if (!whichBitmapCache.containsKey(imageUrl)) {
            return Observable.never();
        }

        Log.i(TAG, "Loading from: " + whichBitmapCache.getName());
        final Bitmap imageBitmap = whichBitmapCache.get(imageUrl);
        return Observable.just(imageBitmap);
    }

    public void clearCache() {
        mDiskCache.clear();
        mMemoryCache.clear();
    }

}