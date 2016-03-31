package me.saket.rxtest.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Persists Bitmaps in files in the cache directory (See {@link Context#getCacheDir()}).
 */
public class DiskBitmapCache implements BitmapCache {

    private static final Object LOCK = new Object();
    private static volatile DiskBitmapCache sDiskCache;

    private final File mCacheDirectory;

    public DiskBitmapCache(Context context) {
        mCacheDirectory = context.getCacheDir();
    }

    public static DiskBitmapCache getInstance(Context context) {
        if (sDiskCache == null) {
            synchronized (LOCK) {
                // Another null check is required if a 2nd thread manages to get
                // queued for this synchronized block while the 1st thread was
                // already executing inside this block, instantiating the object.
                if (sDiskCache == null) {
                    sDiskCache = new DiskBitmapCache(context);
                }
            }
        }
        return sDiskCache;
    }

    @Override
    public String getName() {
        return "Disk Cache";
    }

    @Override
    public ImageSource getSource() {
        return ImageSource.DISK;
    }

    @Override
    public boolean containsKey(String key) {
        synchronized (mCacheDirectory) {
            final Bitmap existingBitmap = get(key);
            return existingBitmap != null;
        }
    }

    @Override
    public Bitmap get(@NonNull String key) {
        synchronized (mCacheDirectory) {
            final String cacheFileName = encodeKey(key);
            final File[] foundCacheFiles = mCacheDirectory.listFiles((dir, filename) -> {
                return filename.equals(cacheFileName);
            });

            if (foundCacheFiles == null || foundCacheFiles.length < 1) {
                // No cached object found for this key
                return null;
            }

            // Read and return its contents
            return readBitmapFromFile(foundCacheFiles[0]);
        }
    }

    @Override
    public void save(String key, Bitmap bitmapToSave) {
        final String cacheFileName = encodeKey(key);
        final File cacheFile = new File(mCacheDirectory, cacheFileName);

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
            saveBitmapToFile(bitmapToSave, fileOutputStream);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {
        synchronized (mCacheDirectory) {
            final File[] cachedFiles = mCacheDirectory.listFiles();
            if (cachedFiles != null) {
                for (final File cacheFile : cachedFiles) {
                    cacheFile.delete();
                }
            }
            mCacheDirectory.delete();
        }
    }

// ======== UTILITY ======== //

    /**
     * Escapes characters in a key (which may be a Url) so that it can be
     * safely used as a File name.
     *
     * This is required because otherwise keys having "\\" may be considered
     * as directory path separators.
     */
    private String encodeKey(String toEncodeString) {
        try {
            return URLEncoder.encode(toEncodeString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap readBitmapFromFile(File foundCacheFile) {
        try {
            final FileInputStream fileInputStream = new FileInputStream(foundCacheFile);
            return BitmapFactory.decodeStream(fileInputStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void saveBitmapToFile(Bitmap bitmapToSave, FileOutputStream fileOutputStream)
            throws IOException {
        try {
            bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

        } finally {
            fileOutputStream.close();
        }
    }

}