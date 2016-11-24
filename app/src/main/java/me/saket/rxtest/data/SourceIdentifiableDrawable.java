package me.saket.rxtest.data;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import static junit.framework.Assert.assertNotNull;

/**
 * A Drawable whose source can be identified by calling {@link #getImageSource()}.
 */
public class SourceIdentifiableDrawable extends BitmapDrawable {

    /**
     * One of {@link ImageSource#MEMORY}, {@link ImageSource#DISK} and {@link ImageSource#NETWORK}.
     */
    private ImageSource mImageSource;

    public SourceIdentifiableDrawable(Bitmap bitmap, ImageSource source) {
        super(bitmap);
        assertNotNull(bitmap);
        mImageSource = source;
    }

    public ImageSource getImageSource() {
        return mImageSource;
    }

}