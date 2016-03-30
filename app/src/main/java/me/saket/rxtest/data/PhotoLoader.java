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
            // (And save into memory cache)
            final Observable<Bitmap> diskImageObservable = loadFromCache(imageUrl, mDiskCache)
                    .doOnNext(saveToCache(imageUrl, mMemoryCache));

            // Plan C: Hit the network
            // (And save into both memory and disk cache for future calls)
            final Observable<Bitmap> networkLoadObs = Observable.defer(() -> {
                Log.i(TAG, "Downloading from the Internet");
                return mNetworkClient
                        .loadImage(imageUrl)
                        .doOnNext(saveToCache(imageUrl, mMemoryCache))
                        .doOnNext(saveToCache(imageUrl, mDiskCache));
            });

            return Observable
                    .concat(memoryCacheLoadObs, diskImageObservable, networkLoadObs)
                     // Calling first() will stop the stream as soon as one item is emitted.
                     // This way, the cheapest source (memory) gets to emit first and the
                     // most expensive source (network) is only reached when no other source
                     // could emit any cached Bitmap.
                    .first(cachedBitmap -> cachedBitmap != null);
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

    /**
     * Returns a stream of the cached bitmap in <var>whichBitmapCache</var>.
     * The emitted item can be null if this cache source does not have anything to offer.
     */
    private Observable<Bitmap> loadFromCache(String imageUrl, BitmapCache whichBitmapCache) {
        final Bitmap imageBitmap = whichBitmapCache.get(imageUrl);
        return Observable
                .just(imageBitmap)
                .compose(logCacheSource(whichBitmapCache));
    }

    /**
     * Simple logging to let us know what each source is returning
     */
    private Observable.Transformer<Bitmap, Bitmap> logCacheSource(BitmapCache whichBitmapCache) {
        return dataObservable -> dataObservable.doOnNext(cachedBitmap -> {
            final String cacheName = whichBitmapCache.getName();
            Log.i(TAG, "Checking: " + cacheName);
            if (cachedBitmap == null) {
                Log.i(TAG, "Does not have this Url");
            } else {
                Log.i(TAG, "Url found in cache!");
            }
        });
    }

    /**
     * Deletes all cached Bitmaps in both memory and disk cache.
     */
    public void clearCache() {
        mDiskCache.clear();
        mMemoryCache.clear();
    }

}