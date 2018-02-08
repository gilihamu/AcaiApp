package migueldaipre.com.acaiapp.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import migueldaipre.com.acaiapp.Model.User;
import migueldaipre.com.acaiapp.Remote.APIService;
import migueldaipre.com.acaiapp.Remote.FCMRetrofitClient;
import migueldaipre.com.acaiapp.Remote.IGoogleService;
import migueldaipre.com.acaiapp.Remote.RetrofitClient;

public class Common {

    public static User currentUser;

    public static String PHONE_TEXT = "userPhone";

    private static final String BASE_URL = "https://fcm.googleapis.com/";

    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";

    public static final String INTENT_FOOD_ID = "FoodId";

    public static APIService getFCMService(){
        return FCMRetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static IGoogleService getGoogleMapAPI(){
        return RetrofitClient.getGoogleClient(GOOGLE_API_URL).create(IGoogleService.class);
    }

    public static final String DELETE = "Delete";

    public static final String USER_KEY = "User";
    public static final String PWD_KEY = "Password";

    public static String convertCodeToStatus(String status) {
        if (status.equals("0")){
            return "Pedido Feito";
        } else if(status.equals("1")) {
            return "Em transporte";
        } else {
            return "Entregue";
        }
    }

    public static boolean isConnectedToInternet(Context context)    {

        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null)    {

            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();

            if (info != null)   {

                for (int i = 0; i <info.length;i++)   {

                    if (info[i].getState() == NetworkInfo.State.CONNECTED)  {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
