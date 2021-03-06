package migueldaipre.com.acaiapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import info.hoang8f.widget.FButton;
import migueldaipre.com.acaiapp.Adapter.CartAdapter;
import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Common.Config;
import migueldaipre.com.acaiapp.Database.DatabaseKK;
import migueldaipre.com.acaiapp.Helper.RecyclerItemTouchHelper;
import migueldaipre.com.acaiapp.Interface.RecyclerItemTouchHelperListener;
import migueldaipre.com.acaiapp.Model.MyResponse;
import migueldaipre.com.acaiapp.Model.Notification;
import migueldaipre.com.acaiapp.Model.Order;
import migueldaipre.com.acaiapp.Model.Request;
import migueldaipre.com.acaiapp.Model.Sender;
import migueldaipre.com.acaiapp.Model.Token;
import migueldaipre.com.acaiapp.Model.User;
import migueldaipre.com.acaiapp.Remote.APIService;
import migueldaipre.com.acaiapp.Remote.IGoogleService;
import migueldaipre.com.acaiapp.ViewHolder.CartViewHolder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Cart extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener, RecyclerItemTouchHelperListener {

    private static final int PAYPAL_REQUEST_CODE = 9999;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    public Button txtTotalPrice;
    Button btnPlace;

    List<Order> cart = new ArrayList<>();

    CartAdapter adapter;

    Place shippingAddress;

    IGoogleService mGoogleMapService;
    APIService mService;

    RelativeLayout rootLayout;

    //paypal payments
    static PayPalConfiguration config = new PayPalConfiguration().environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(Config.PAYPAL_CLIENT_ID);

    String address,comment;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    private static final int LOCATION_REQUEST_CODE = 9999;

    private static final int PLAY_SERVICES_REQUEST = 9997;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig .initDefault(new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Lato/Lato-Reg.ttf")
                                        .setFontAttrId(R.attr.fontPath).build());

        setContentView(R.layout.activity_cart);

        mGoogleMapService = Common.getGoogleMapAPI();

        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        //runtime permission 4 location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(this,new String[]
                    {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,

                    },LOCATION_REQUEST_CODE);
        }
        else    {

            if (checkPlayServices())    {

                buildGoogleAPIClient();
                createLocationRequest();
            }
        }

        //init paypal
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        startService(intent);

        //init service
        mService = Common.getFCMService();

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        recyclerView = (RecyclerView)findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Swipe to delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        txtTotalPrice = (Button) findViewById(R.id.total);
        btnPlace = (Button) findViewById(R.id.btnPlaceOrder);

        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cart.size() > 0)
                    showAlertDialog();
                else
                    Toast.makeText(Cart.this, "Seu carrinho está vazio.", Toast.LENGTH_SHORT).show();
            }
        });
            loadListFood();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private synchronized void buildGoogleAPIClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))  {

                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_REQUEST).show();
            }
            else    {
                Toast.makeText(this, "Este dispositivo não é suportado.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("Mais uma Etapa...");
        alertDialog.setMessage("Digite Seu Endereço : ");

        final LayoutInflater inflater = this.getLayoutInflater();
        View order_address_comment = inflater.inflate(R.layout.order_address_comment,null);

        //final MaterialEditText edtAddress = (MaterialEditText)order_address_comment.findViewById(R.id.edtAddress);

        final PlaceAutocompleteFragment edtAddress = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        //hide search icon b4 fragment
        edtAddress.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);

        //set hint for autocomplete text
        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setHint("Digite seu endereço");

        //set text size
        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setTextSize(14);

        //get address from place & autocomplete
        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                shippingAddress = place;
            }

            @Override
            public void onError(Status status) {
                Log.e("ERROR",status.getStatusMessage());
            }
        });

        final MaterialEditText edtComment = (MaterialEditText)order_address_comment.findViewById(R.id.edtComment);

        //radio
        final RadioButton rdiShipToAddress = (RadioButton)order_address_comment.findViewById(R.id.rdiShipToAddress);
        final RadioButton rdiHomeAddress = (RadioButton)order_address_comment.findViewById(R.id.rdiHomeAddress);
        final RadioButton rdiCOD = (RadioButton)order_address_comment.findViewById(R.id.rdiCOD);
        final RadioButton rdiPaypal = (RadioButton)order_address_comment.findViewById(R.id.rdiPaypal);
        final RadioButton rdiBalance = (RadioButton)order_address_comment.findViewById(R.id.rdiBalance);

        //on radio button selected
        rdiShipToAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b == true)  {

                    mGoogleMapService.getAddressName(String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false",
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude())).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                JSONArray resultArray = jsonObject.getJSONArray("results");

                                JSONObject firstObject = resultArray.getJSONObject(0);

                                address = firstObject.getString("formatted_address");
                                //set this address to edtAddress
                                //((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(Cart.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        rdiHomeAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)  {

                    if (Common.currentUser.getHomeAddress() != null || !TextUtils.isEmpty(Common.currentUser.getHomeAddress()))    {

                        address = Common.currentUser.getHomeAddress();
                        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setText(address);
                    }
                    else    {
                        Toast.makeText(Cart.this, "Por favor atualizar o Endereço Residencial.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        alertDialog.setView(order_address_comment);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                //add check condition here
                //if user select address from place fragment then just use it
                //if user select ship to this address, then get location from address and use it
                //if user select home address, then get homeAddress from profile and use it

                if (!rdiShipToAddress.isChecked() && !rdiHomeAddress.isChecked()) {
                    //if both are not selected
                    if (shippingAddress !=  null) {
                        address = shippingAddress.getAddress().toString();
                    }
                    else    {
                        Toast.makeText(Cart.this, "Insira seu endereço ou selecione uma opção.", Toast.LENGTH_LONG).show();

                        //fix crash fragment
                        getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)).commit();

                        return;
                    }
                }

                if (TextUtils.isEmpty(address)) {

                    Toast.makeText(Cart.this, "Endereço não preenchido.", Toast.LENGTH_SHORT).show();
                    //fix crash fragment
                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)).commit();

                    return;
                }

                    comment = edtComment.getText().toString();

                    // Check payment
                    if(!rdiCOD.isChecked() && !rdiPaypal.isChecked() && !rdiBalance.isChecked()){ // If both COD, Paypal and Balance is not checked
                        Toast.makeText(Cart.this, "Por favor selecione um método de pagamento.", Toast.LENGTH_LONG).show();
                        //fix crash fragment
                        getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)).commit();

                        return;
                    }else if(rdiPaypal.isChecked()) {

                        String formatAmount = txtTotalPrice.getText().toString()
                                .replace("R$", "")
                                .replace(".", "")
                                .replace(",", ".");


                        PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(formatAmount),
                                "BRL",
                                "Pedido Açai App",
                                PayPalPayment.PAYMENT_INTENT_SALE);

                        Intent intent = new Intent(getApplicationContext(), PaymentActivity.class);
                        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
                        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
                        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
                    }else if (rdiCOD.isChecked()){
                        Request request = new Request(
                                Common.currentUser.getPhone(),
                                Common.currentUser.getName(),
                                address,
                                txtTotalPrice.getText().toString(),
                                "0",    //status
                                comment,
                                "COD",
                                "Unpaid",
                                String.format("%s %s", mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                                cart);

                        //submit to firebase
                        String order_number = String.valueOf(System.currentTimeMillis());

                        requests.child(order_number).setValue(request);

                        //delete cart
                        new DatabaseKK(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                        sendNotificationOrder(order_number);

                        Toast.makeText(Cart.this, "Muito obrigado, pedido enviado.", Toast.LENGTH_LONG).show();
                        finish();
                    }else if(rdiBalance.isChecked()){
                        double amount = 0;
                        // First, get total price from txtTotalPrice
                        try {
                            amount = Common.formatCurrency(txtTotalPrice.getText().toString(), Locale.US).doubleValue();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        //after receive total price of this order, just compare with user balance
                        if(Common.currentUser.getBalance() >= amount){
                            // if balance >= amount
                            Request request = new Request(
                                    Common.currentUser.getPhone(),
                                    Common.currentUser.getName(),
                                    address,
                                    txtTotalPrice.getText().toString(),
                                    "0",    //status
                                    comment,
                                    "Balance",
                                    "Paid",
                                    String.format("%s %s", mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                                    cart);

                            //submit to firebase
                            final String order_number = String.valueOf(System.currentTimeMillis());

                            requests.child(order_number).setValue(request);

                            //delete cart
                            new DatabaseKK(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                            // Update Balance
                            double balance = Common.currentUser.getBalance() - amount;
                            Map<String, Object> update_balance = new HashMap<>();
                            update_balance.put("balance",balance);

                            FirebaseDatabase.getInstance()
                                    .getReference("User")
                                    .child(Common.currentUser.getPhone())
                                    .updateChildren(update_balance)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                //Refresh user
                                                FirebaseDatabase.getInstance()
                                                        .getReference("User")
                                                        .child(Common.currentUser.getPhone())
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                Common.currentUser = dataSnapshot.getValue(User.class);
                                                                sendNotificationOrder(order_number);
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }
                                                        });
                                            }
                                        }
                                    });

                        }else {
                            Toast.makeText(Cart.this, "Seu saldo é insuficiente.", Toast.LENGTH_LONG).show();
                        }

                    }

                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)).commit();

            }
        });
        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();

                //remove fragment
                getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)).commit();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)    {

            case LOCATION_REQUEST_CODE :
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)    {

                    if (checkPlayServices())    {

                        buildGoogleAPIClient();
                        createLocationRequest();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == RESULT_OK)    {
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null)   {
                    try {
                        String paymentDetail = confirmation.toJSONObject().toString(4);

                        JSONObject jsonObject = new JSONObject(paymentDetail);

                        Request request = new Request(
                                Common.currentUser.getPhone(),
                                Common.currentUser.getName(),
                                address,
                                txtTotalPrice.getText().toString(),
                                "0",    //status
                                comment,
                                "Paypal",
                                jsonObject.getJSONObject("response").getString("state"),    //state from json
                                String.format("%s %s",shippingAddress.getLatLng().latitude,shippingAddress.getLatLng().longitude),
                                cart);

                        //submit to firebase
                        String order_number = String.valueOf(System.currentTimeMillis());

                        requests.child(order_number).setValue(request);

                        //delete cart
                        new DatabaseKK(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                        sendNotificationOrder(order_number);

                Toast.makeText(Cart.this, "Muito obrigado, pedido enviado.", Toast.LENGTH_SHORT).show();
                finish();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (resultCode == Activity.RESULT_CANCELED)    {
                Toast.makeText(this, "Pagamento recusado.", Toast.LENGTH_SHORT).show();
            }
            else if (requestCode == PaymentActivity.RESULT_EXTRAS_INVALID)  {
                Toast.makeText(this, "Pagamento inválido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query data = tokens.orderByChild("serverToken").equalTo(true);

        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren())    {

                    Token isServerToken = postSnapShot.getValue(Token.class);

                    //create raw payload
                    Notification notification = new Notification("Açai Delivery","Você possui um novo pedido: "+order_number);
                    Sender content = new Sender(isServerToken.getToken(),notification);

                    mService.sendNotification(content)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                                    if(response.code() == 200){
                                        if (response.body().success == 1){
                                            Toast.makeText(Cart.this, "Muito Obrigado, Pedido feito.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }else {
                                            Toast.makeText(Cart.this, "Falhou...", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Log.e("ERROR", t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadListFood() {

        cart = new DatabaseKK(Cart.this).getCarts(Common.currentUser.getPhone());
        adapter = new CartAdapter(cart,this);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        //calculate total price
        int total = 0;
        for (Order order : cart)    {
            total += ((Integer.parseInt(order.getPrice())) * (Integer.parseInt(order.getQuantity())))
                    - ((Integer.parseInt(order.getDiscount())) * (Integer.parseInt(order.getQuantity())));

            Locale locale = new Locale("pt","BR");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

            txtTotalPrice.setText(fmt.format(total));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());

        return true;
    }

    private void deleteCart(int position) {
        //we will remove item by position
        cart.remove(position);
        //after that we will delete all data from sqlLite
        new DatabaseKK(this).cleanCart(Common.currentUser.getPhone());
        //now we will update new data

        for (Order item : cart)
            new DatabaseKK(this).addToCart(item);

         //refresh
        loadListFood();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)  {

            Log.d("LOCATION","Your Location : "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
        }
        else    {

            Log.d("LOCATION","Couldn't Get Your Location.");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if(viewHolder instanceof CartViewHolder){
            String name = ((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition()).getProductName();

            final Order deleteItem = ((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());
            final int deleteIndex = viewHolder.getAdapterPosition();

            adapter.removeItem(deleteIndex);
            new DatabaseKK(getBaseContext()).removeFromCart(deleteItem.getProductId(),Common.currentUser.getPhone());

            //Update Total
            //calculate total price
            int total = 0;
            List<Order> orders = new DatabaseKK(getBaseContext()).getCarts(Common.currentUser.getPhone());

            for (Order item : orders)    {
                //total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(item.getQuantity()));

                total += ((Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity())))
                        - ((Integer.parseInt(item.getDiscount())) * (Integer.parseInt(item.getQuantity())));


                Locale locale = new Locale("pt","BR");
                NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                txtTotalPrice.setText(fmt.format(total));
            }

            //Make Snackbar
            Snackbar snackbar = Snackbar.make(rootLayout,name+" removido do carrinho.",Snackbar.LENGTH_LONG);
            snackbar.setAction("Desfazer", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.restoreItem(deleteItem,deleteIndex);
                    new DatabaseKK(getBaseContext()).addToCart(deleteItem);

                    //Update Total
                    //calculate total price
                    int total = 0;
                    List<Order> orders = new DatabaseKK(getBaseContext()).getCarts(Common.currentUser.getPhone());

                    for (Order item : orders)    {
                        //total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(item.getQuantity()));

                        total += ((Integer.parseInt(item.getPrice())) * (Integer.parseInt(item.getQuantity())))
                                - ((Integer.parseInt(item.getDiscount())) * (Integer.parseInt(item.getQuantity())));


                        Locale locale = new Locale("pt","BR");
                        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                        txtTotalPrice.setText(fmt.format(total));
                    }
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

        }
    }
}
