package me.saket.rxtest.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.rxtest.R;
import me.saket.rxtest.data.PhotoLoader;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.spinner_image_url) Spinner mImageUrlSpinner;
    @Bind(R.id.imageview) ImageView mImageView;
    @Bind(R.id.load_progress_indicator) ProgressBar mProgressIndicator;
    @Bind(R.id.btn_download_image) Button mDownloadButton;

    private PhotoLoader mPhotoLoader;
    private Subscription mImageLoadSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mPhotoLoader = PhotoLoader.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageLoadSubscription != null) {
            mImageLoadSubscription.unsubscribe();
        }
    }

    @OnClick(R.id.btn_download_image)
    void onDownloadButtonClick() {
        // Start progress indicator
        setProgressBarVisible(true);

        // Cancel any ongoing load
        if (mImageLoadSubscription != null) {
            mImageLoadSubscription.unsubscribe();
        }

        // And download image
        mImageLoadSubscription = Observable.just(getSelectedImageUrl())
                .flatMap(loadImageBitmapFromUrl())
                .compose(applySchedulers())
                .doOnEach(notif -> setProgressBarVisible(false))
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onNext(Bitmap bitmap) {
                        mImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleImageDownloadError(e);
                        mImageLoadSubscription.unsubscribe();
                    }

                    @Override
                    public void onCompleted() {
                        mImageLoadSubscription.unsubscribe();
                    }
                });
    }

    @OnClick(R.id.btn_clear_cache)
    void onClearCacheButtonClick() {
        mPhotoLoader.clearCache();
    }

    /**
     * Shows / hides the progress indicator and alongside, the download button
     * in a reverse manner.
     */
    private void setProgressBarVisible(boolean progressVisible) {
        mProgressIndicator.setVisibility(progressVisible ? View.VISIBLE : View.GONE);
        mDownloadButton.setVisibility(progressVisible ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private Func1<String, Observable<? extends Bitmap>> loadImageBitmapFromUrl() {
        return imageUrl -> mPhotoLoader.load(imageUrl);
    }

    @NonNull
    private String getSelectedImageUrl() {
        return mImageUrlSpinner.getSelectedItem().toString();
    }

    private void handleImageDownloadError(Throwable e) {
        Snackbar.make(mImageView, "Download failed brah", Snackbar.LENGTH_SHORT).show();
        e.printStackTrace();
    }

    /**
     * Simple transformer that will take an observable and
     * 1. Schedule it on a worker thread
     * 2. Observe on main thread
     */
    public static <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}