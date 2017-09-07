package com.base.common.imageloader;

import android.content.Context;

/**
 * Created by Anthony on 2016/3/3.
 * Class Note:use this class to load image,single instance
 */
public class ImageLoader {

    public static final int PIC_LARGE = 0;
    public static final int PIC_MEDIUM = 1;
    public static final int PIC_SMALL = 2;

    public static final int LOAD_STRATEGY_NORMAL = 0;
    public static final int LOAD_STRATEGY_ONLY_WIFI = 1;

    private static ImageLoader sInstance;
    private ILoaderProvider mProvider;

    private ImageLoader() {
        mProvider = new FrescoLoaderProvider();
    }

    //single instance
    public static ImageLoader getInstance() {
        if (sInstance == null) {
            synchronized (ImageLoader.class) {
                if (sInstance == null) {
                    sInstance = new ImageLoader();
                    return sInstance;
                }
            }
        }
        return sInstance;
    }


    public void loadImage(Context context, Image img) {
        mProvider.loadImage(context, img);
    }
}
