package migueldaipre.com.acaiapp.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;


public interface IGoogleService {

    @GET
    Call<String> getAddressName(@Url String url);
}
