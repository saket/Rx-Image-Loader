package me.saket.rxtest.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;

/**
 * {@link #loadImage(String)} is where you should start.
 */
public class NetworkClient {

    private static NetworkClient sNetworkClient;
    private final OkHttpClient mOkHttpClient;

    /**
     * Maximum time to wait before giving up (for connecting to a
     * server as well as waiting for its response).
     */
    long TIMEOUT_DELAY_SECS = 60;   // 60s

    public NetworkClient() {
        mOkHttpClient = new OkHttpClient();
    }

    public static NetworkClient getInstance() {
        if (sNetworkClient == null) {
            sNetworkClient = new NetworkClient();
        }
        return sNetworkClient;
    }

    /**
     * Loads an Image from the internet.
     */
    public Observable<Bitmap> loadImage(String imageUrl) {
        return Observable.fromCallable(() -> {
            final Request loadRequest = new Request.Builder()
                    .url(imageUrl)
                    .build();

            final Response response = mOkHttpClient
                    .newCall(loadRequest)
                    .execute();

            return BitmapFactory.decodeStream(response.body().byteStream());
        });
    }

}