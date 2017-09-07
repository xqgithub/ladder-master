package cc.cloudist.app.data.remote;

import cc.cloudist.app.data.model.Response;
import cc.cloudist.app.data.model.ShadowSocks;

import java.util.List;

import cc.cloudist.app.data.model.User;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface CloudistService {

    String BASE_URL = "https://api.leancloud.cn/1.1/";

    @GET("classes/Shadowsocks")
    Observable<Response<List<ShadowSocks>>> getShadowSocksConfig();

    @GET("login")
    Observable<retrofit2.Response<User>> signIn(@Query("username") String username, @Query("password") String password);
}
