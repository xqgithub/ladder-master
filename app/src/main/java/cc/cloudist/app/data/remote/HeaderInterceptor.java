package cc.cloudist.app.data.remote;

import cc.cloudist.app.common.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.newBuilder()
                .header(Constants.HEADER_ID, Constants.APP_ID)
                .header(Constants.HEADER_KEY, Constants.REST_KEY)
                .header("Content-Type", "application/json")
                .method(original.method(), original.body())
                .build();

        return chain.proceed(request);
    }
}
