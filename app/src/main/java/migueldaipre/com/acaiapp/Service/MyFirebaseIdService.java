package migueldaipre.com.acaiapp.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Model.Token;


public class MyFirebaseIdService extends FirebaseInstanceIdService  {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String tokenRefreshed = FirebaseInstanceId.getInstance().getToken();
        if (Common.currentUser != null) {
            updateTokenToFirebase(tokenRefreshed);
        }
    }

    private void updateTokenToFirebase(String tokenRefreshed) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");
        Token token = new Token(tokenRefreshed,false);  //false bcz : token sent from client side
        tokens.child(Common.currentUser.getPhone()).setValue(token);
    }
}
