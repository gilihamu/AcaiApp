package migueldaipre.com.acaiapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Database.DatabaseKK;
import migueldaipre.com.acaiapp.Interface.ItemClickListener;
import migueldaipre.com.acaiapp.Model.Food;
import migueldaipre.com.acaiapp.Model.Order;
import migueldaipre.com.acaiapp.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food,FoodViewHolder> adapter;

    //search function
    FirebaseRecyclerAdapter<Food,FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    DatabaseKK localDB;

    //facebook share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    SwipeRefreshLayout swipeRefreshLayout;

    //create target from picasso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //create photo frm bitmap
            SharePhoto photo = new SharePhoto.Builder().setBitmap(bitmap).build();

            if (ShareDialog.canShow(SharePhotoContent.class))   {
                SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();

                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath).build());

        setContentView(R.layout.activity_food_list);

        //init facebook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDark,android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,android.R.color.holo_blue_dark);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //get intent here
                if (getIntent() != null)    {
                    categoryId = getIntent().getStringExtra("CategoryId");
                }
                if (!categoryId.isEmpty() && categoryId != null)    {

                    if (Common.isConnectedToInternet(getBaseContext())) {
                        loadListFood(categoryId);
                    }
                    else {
                        Toast.makeText(FoodList.this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //get intent here
                if (getIntent() != null)    {
                    categoryId = getIntent().getStringExtra("CategoryId");
                }
                if (!categoryId.isEmpty() && categoryId != null)    {

                    if (Common.isConnectedToInternet(getBaseContext())) {
                        loadListFood(categoryId);
                    }
                    else {
                        Toast.makeText(FoodList.this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                //search
                materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
                materialSearchBar.setHint("Procurar");

                loadSuggest();

                materialSearchBar.setCardViewElevation(10);

                materialSearchBar.addTextChangeListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        List<String> suggest = new ArrayList<String>();
                        for (String search: suggestList){
                            if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase())){
                                suggest.add(search);
                            }
                        }
                        materialSearchBar.setLastSuggestions(suggest);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                    @Override
                    public void onSearchStateChanged(boolean enabled) {
                        // When Search Bar is close
                        // Restore original suggest adapter
                        if (!enabled){
                            recyclerView.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onSearchConfirmed(CharSequence text) {
                        // When Search finish
                        // Show result of search adapter
                        startSearch(text);

                    }

                    @Override
                    public void onButtonClicked(int buttonCode) {

                    }
                });
            }
        });

        localDB = new DatabaseKK(this);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

    }

    private void startSearch(CharSequence text) {

        Query searchByName = foodList.orderByChild("name").equalTo(text.toString());

        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                                                            .setQuery(searchByName,Food.class).build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull final Food model) {

                viewHolder.food_name.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.food_image);

                final Food local = model;

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, "" + model.getName(), Toast.LENGTH_SHORT).show();
                        // start Food detail activity
                        Intent intentFoodDetail = new Intent(FoodList.this, FoodDetail.class);
                        intentFoodDetail.putExtra("FoodId", searchAdapter.getRef(position).getKey());

                        startActivity(intentFoodDetail);
                    }
                });
            }

            @Override
            public FoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item,parent,false);

                return new FoodViewHolder(itemView);
            }
        };
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter);
    }

    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName());
                        }
                        materialSearchBar.setLastSuggestions(suggestList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void loadListFood(String categoryId) {

        Query searchByName = foodList.orderByChild("menuId").equalTo(categoryId);

        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class).build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder viewHolder, final int position, @NonNull final Food model) {
                viewHolder.food_name.setText(model.getName());
                viewHolder.food_price.setText(String.format("$ %s",model.getPrice().toString()));

                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.food_image);

                //quick cart
                viewHolder.quick_cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new DatabaseKK(getBaseContext()).addToCart(new Order(
                                adapter.getRef(position).getKey(),
                                model.getName(),
                                "1",
                                model.getPrice(),
                                model.getDiscount(),
                                model.getImage()));

                        Toast.makeText(FoodList.this, "Added To Cart", Toast.LENGTH_SHORT).show();
                    }
                });

                //add favourites
                if (localDB.isFavourites(adapter.getRef(position).getKey()))    {
                    viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                }

                //click to share
                viewHolder.share_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Picasso.with(getBaseContext()).load(model.getImage()).into(target);
                    }
                });

                //click to change status of the favourite
                viewHolder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (!localDB.isFavourites(adapter.getRef(position).getKey()))   {
                            localDB.addToFavourites(adapter.getRef(position).getKey());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+" was added to Favourites", Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            localDB.removeFromFavourites(adapter.getRef(position).getKey());
                            viewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+" was Removed From Favourites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, ""+local.getName(), Toast.LENGTH_SHORT).show();

                        //start the activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",adapter.getRef(position).getKey());    //send food to new activity
                        startActivity(foodDetail);
                    }
                });
            }

            @Override
            public FoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemViw = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item,parent,false);

                return new FoodViewHolder(itemViw);
            }
        };
        //set adapter
        adapter.startListening();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
        //searchAdapter.stopListening();
    }
}
