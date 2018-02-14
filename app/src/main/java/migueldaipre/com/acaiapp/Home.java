package migueldaipre.com.acaiapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.facebook.accountkit.AccountKit;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Database.DatabaseKK;
import migueldaipre.com.acaiapp.Interface.ItemClickListener;
import migueldaipre.com.acaiapp.Model.Banner;
import migueldaipre.com.acaiapp.Model.Category;
import migueldaipre.com.acaiapp.Model.Favorites;
import migueldaipre.com.acaiapp.Model.Token;
import migueldaipre.com.acaiapp.ViewHolder.MenuViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FirebaseDatabase database;
    DatabaseReference category;

    TextView txtFullName;

    RecyclerView recycler_menu;
    //RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Category,MenuViewHolder> adapter;

    SwipeRefreshLayout swipeRefreshLayout;

    CounterFab fab;

    HashMap<String,String> image_list;
    SliderLayout mSlider;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath).build());

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);

        //view
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDark,android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,android.R.color.holo_blue_dark);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Common.isConnectedToInternet(getBaseContext())) {
                    loadMenu();
                }
                else {
                    Toast.makeText(getBaseContext(), "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        //default load for first time
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (Common.isConnectedToInternet(getBaseContext())) {
                    loadMenu();
                }
                else {
                    Toast.makeText(getBaseContext(), "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        //init firebase
        database = FirebaseDatabase.getInstance();
        category = database.getReference("Category");

        FirebaseRecyclerOptions<Category> options = new FirebaseRecyclerOptions.Builder<Category>()
                .setQuery(category,Category.class).build();

        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder viewHolder, int position, @NonNull Category model) {

                viewHolder.txtMenuName.setText(model.getName());

                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.imageView);

                final Category clickItem = model;

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(Home.this, ""+clickItem.getName(), Toast.LENGTH_SHORT).show();

                        //get category id and send to activity
                        Intent foodList = new Intent(Home.this,FoodList.class);
                        //bcz category id is key so just get the key from there
                        foodList.putExtra("CategoryId",adapter.getRef(position).getKey());
                        startActivity(foodList);
                    }
                });
            }

            @Override
            public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item,parent,false);

                return new MenuViewHolder(itemView);
            }
        };

        fab = (CounterFab) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cartIntent = new Intent(Home.this,Cart.class);
                startActivity(cartIntent);
            }
        });

        fab.setCount(new DatabaseKK(this).getCountCart(Common.currentUser.getPhone()));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //set name for user
        View headerView = navigationView.getHeaderView(0);
        txtFullName = (TextView)headerView.findViewById(R.id.txtFullName);
        txtFullName.setText(Common.currentUser.getName());

        //load menu
        recycler_menu = (RecyclerView)findViewById(R.id.recycler_menu);
        //recycler_menu.setHasFixedSize(true);
        //layoutManager = new LinearLayoutManager(this);
        //recycler_menu.setLayoutManager(layoutManager);
        recycler_menu.setLayoutManager(new GridLayoutManager(this,2));

        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(recycler_menu.getContext(),
                R.anim.layout_fall_down);
        recycler_menu.setLayoutAnimation(controller);

        updateToken(FirebaseInstanceId.getInstance().getToken());

        //set slider
        setupSlider();
    }

    private void setupSlider() {
        mSlider = (SliderLayout)findViewById(R.id.slider);
        image_list = new HashMap<>();

        final DatabaseReference banners = database.getReference("Banner");

        banners.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren())  {
                    Banner banner = postSnapshot.getValue(Banner.class);

                    image_list.put(banner.getName()+"@@@"+banner.getId(),banner.getImage());
                }

                for (String key : image_list.keySet())  {

                    String[] keySplit = key.split("@@@");
                    String nameOfFood = keySplit[0];
                    String idOfFood = keySplit[1];

                    //create slider
                    final TextSliderView textSliderView = new TextSliderView(getBaseContext());

                    textSliderView.description(nameOfFood)
                            .image(image_list.get(key))
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                                @Override
                                public void onSliderClick(BaseSliderView slider) {
                                    Intent intent = new Intent(Home.this,FoodDetail.class);
                                    intent.putExtras(textSliderView.getBundle());
                                    startActivity(intent);
                                }
                            });
                    //add extra bundle
                    textSliderView.bundle(new Bundle());
                    textSliderView.getBundle().putString("FoodId",idOfFood);

                    mSlider.addSlider(textSliderView);

                    //remove after finishing
                    banners.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSlider.setPresetTransformer(SliderLayout.Transformer.Background2Foreground);
        mSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mSlider.setCustomAnimation(new DescriptionAnimation());
        mSlider.setDuration(4000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fab.setCount(new DatabaseKK(this).getCountCart(Common.currentUser.getPhone()));

        //fix
        if (adapter != null)    {
            adapter.startListening();
        }
    }

    private void updateToken(String token) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");
        Token data = new Token(token,false);  //false bcz : token sent from client side
        tokens.child(Common.currentUser.getPhone()).setValue(data);
    }

    private void loadMenu() {

        adapter.startListening();
        recycler_menu.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);

        //animation
        recycler_menu.getAdapter().notifyDataSetChanged();
        recycler_menu.scheduleLayoutAnimation();
    }



    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
        mSlider.stopAutoCycle();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        if (item.getItemId() == R.id.menu_search)   {
            startActivity(new Intent(Home.this, SearchActivity.class));
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {
        }else if (id == R.id.nav_home_address)   {
            showHomeAddressDialog();
        }else if (id == R.id.nav_cart) {
            Intent cartIntent = new Intent(Home.this,Cart.class);
            startActivity(cartIntent);
        }else if (id == R.id.nav_favorites) {
            Intent favIntent = new Intent(Home.this,FavoritesActivity.class);
            startActivity(favIntent);
        }else if (id == R.id.nav_orders) {
            Intent orderStatusIntent = new Intent(Home.this,OrderStatus.class);
            orderStatusIntent.putExtra("userPhone", Common.currentUser.getPhone());
            startActivity(orderStatusIntent);
        }else if (id == R.id.nav_update_name)  {
            showUpdateNameDialog();
        }else if(id == R.id.nav_setting){
            showSettingDialog();
        }else if (id == R.id.nav_log_out) {
            //logout
            AccountKit.logOut();
            // Intent signIn
            Intent signOutIntent = new Intent(Home.this,MainActivity.class);
            signOutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signOutIntent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showSettingDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Configurações");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_setting = inflater.inflate(R.layout.setting_layout,null);

        final CheckBox ckb_subscribe_new = (CheckBox)layout_setting.findViewById(R.id.ckb_sub_new);
        // Add Code remember state of Checkbox

        Paper.init(this);
        final String isSubscribe = Paper.book().read("sub_new");

        if(isSubscribe == null || TextUtils.isEmpty(isSubscribe) || isSubscribe.equals("false")){
            ckb_subscribe_new.setChecked(false);
        }else {
            ckb_subscribe_new.setChecked(true);
        }

        alertDialog.setView(layout_setting);

        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
                if (ckb_subscribe_new.isChecked()){
                    FirebaseMessaging.getInstance().subscribeToTopic(Common.topicName);
                    // Write value
                    Paper.book().write("sub_new", "true");
                }else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(Common.topicName);
                    // Write value
                    Paper.book().write("sub_new", "false");
                }
            }
        });
        alertDialog.show();
    }
    private void showHomeAddressDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Change Home Address");
        alertDialog.setMessage("Please Fill all information..");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_home = inflater.inflate(R.layout.home_address_layout,null);

        final MaterialEditText edtHomeAddress = (MaterialEditText)layout_home.findViewById(R.id.edtHomeAddress);

        alertDialog.setView(layout_home);

        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();

                //set new home address
                Common.currentUser.setHomeAddress(edtHomeAddress.getText().toString());

                FirebaseDatabase.getInstance().getReference("User").child(Common.currentUser.getPhone())
                        .setValue(Common.currentUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(Home.this, "Home Address Updated Successfully!!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        alertDialog.show();
    }

    private void showUpdateNameDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("UPDATE NAME");
        alertDialog.setMessage("Please Fill all information..");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_update_name = inflater.inflate(R.layout.update_name_layout,null);

        final MaterialEditText edtName = (MaterialEditText)layout_update_name.findViewById(R.id.edtName);

        alertDialog.setView(layout_update_name);
        //Button
        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //change password here

                final android.app.AlertDialog waitingDialog = new SpotsDialog(Home.this);
                waitingDialog.show();

                //update name
                Map<String,Object> update_name = new HashMap<>();
                update_name.put("name",edtName.getText().toString());

                FirebaseDatabase.getInstance().getReference("User")
                        .child(Common.currentUser.getPhone())
                        .updateChildren(update_name)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                waitingDialog.dismiss();
                                if (task.isSuccessful())    {

                                    Toast.makeText(Home.this, "Name Was Updated!!!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }
}
