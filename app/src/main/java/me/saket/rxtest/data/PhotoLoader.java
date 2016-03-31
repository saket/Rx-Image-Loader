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
    public Observable<Image> load(String imageUrl) {
        return Observable.just(imageUrl).flatMap(findImageSource());
    }

    private Func1<String, Observable<Image>> findImageSource() {
        return imageUrl -> {
            Log.d(TAG, "Loading image Url: " + imageUrl);

            // Plan A: Check in memory
            final Observable<Image> memoryCacheLoadObs = loadFromCache(imageUrl, mMemoryCache);

            // Plan B: Look into files
            // (And save into memory cache)
            final Observable<Image> diskImageObservable = loadFromCache(imageUrl, mDiskCache)
                    .doOnNext(saveToCache(imageUrl, mMemoryCache));

            // Plan C: Hit the network
            // (And save into both memory and disk cache for future calls)
            final Observable<Image> networkLoadObs = Observable.defer(() -> {
                Log.i(TAG, "Downloading from the Internet");
                return mNetworkClient
                        .loadImage(imageUrl)
                        .map(bitmap -> new Image(bitmap, Image.Source.NETWORK))
                        .doOnNext(saveToCache(imageUrl, mMemoryCache))
                        .doOnNext(saveToCache(imageUrl, mDiskCache));
            });

            return Observable
                    .concat(memoryCacheLoadObs, diskImageObservable, networkLoadObs)
                     // Calling first() will stop the stream as soon as one item is emitted.
                     // This way, the cheapest source (memory) gets to emit first and the
                     // most expensive source (network) is only reached when no other source
                     // could emit any cached image.
                    .first(cachedImage -> cachedImage != null);
        };
    }

    private Action1<Image> saveToCache(String imageUrl, BitmapCache bitmapCache) {
        return image -> {
            if (image == null) {
                return;
            }

            Log.i(TAG, "Saving to: " + bitmapCache.getName());
            bitmapCache.save(imageUrl, image.bitmap);
        };
    }

    /**
     * Returns a stream of the cached bitmap in <var>whichBitmapCache</var>.
     * The emitted item can be null if this cache source does not have anything to offer.
     */
    private Observable<Image> loadFromCache(String imageUrl, BitmapCache whichBitmapCache) {
        return Observable
                .just(whichBitmapCache.get(imageUrl))
                .filter(bitmap -> bitmap != null)
                .compose(logCacheSource(whichBitmapCache))
                .map(bitmap -> new Image(bitmap, whichBitmapCache.getSource()));
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