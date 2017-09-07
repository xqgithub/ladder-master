package com.base.common.http;

import com.base.common.BaseApplication;
import com.base.common.BuildConfig;
import com.base.common.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpClient {

    private static final String TAG = LogUtils.makeLogTag(HttpClient.class);

    private static HttpClient instance;
    private Retrofit retrofit;

    public static HttpClient getInstance(String baseUrl, Interceptor... interceptors) {
        if (instance == null) {
            synchronized (HttpClient.class) {
                if (instance == null) {
                    instance = new HttpClient(baseUrl, interceptors);
                }
            }
        }

        return instance;
    }

    public static HttpClient getInstance(String baseUrl) {
        return getInstance(baseUrl, null);
    }

    public HttpClient(String baseUrl, Interceptor... interceptors) {
        HttpLoggingInterceptor logInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        LogUtils.d(TAG, message);
                    }
                });
        logInterceptor.setLevel(LogUtils.DEBUG
                ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        File cacheFile = new File(BaseApplication.getInstance().getCacheDir(), "android");
        Cache cache = new Cache(cacheFile, 1024 * 1024 * 100); //100Mb

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .readTimeout(7676, TimeUnit.MILLISECONDS)
                .connectTimeout(7676, TimeUnit.MILLISECONDS)
                .addInterceptor(logInterceptor)
                .addNetworkInterceptor(new HttpCacheInterceptor())
                .cache(cache);

        if (interceptors != null) {
            for (Interceptor interceptor : interceptors)
            okHttpBuilder.addInterceptor(interceptor);
        }

        OkHttpClient okHttpClient = okHttpBuilder.build();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    public <T> T createService(Class<T> clz) {
        return retrofit.create(clz);
    }
}
