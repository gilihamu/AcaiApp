package migueldaipre.com.acaiapp.Remote;

import migueldaipre.com.acaiapp.Model.MyResponse;
import migueldaipre.com.acaiapp.Model.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAA4F_zoa0:APA91bECVEy16drqp5pOvzLsByZRWXiiWi9OeNhvRxriN9KFirnLcPCf_RRjh1gMCYw8jRbN11KummTz3CM_8QbJEtanp9K-v3LyYPq6BwVEMzHFVac4eB95m3-KyW5Jak4qFoA0FAyY"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
