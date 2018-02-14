package migueldaipre.com.acaiapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.List;

import migueldaipre.com.acaiapp.Common.Common;
import migueldaipre.com.acaiapp.Database.DatabaseKK;
import migueldaipre.com.acaiapp.FoodDetail;
import migueldaipre.com.acaiapp.FoodList;
import migueldaipre.com.acaiapp.Interface.ItemClickListener;
import migueldaipre.com.acaiapp.Model.Favorites;
import migueldaipre.com.acaiapp.Model.Food;
import migueldaipre.com.acaiapp.Model.Order;
import migueldaipre.com.acaiapp.R;
import migueldaipre.com.acaiapp.ViewHolder.FavoritesViewHolder;

/**
 * Created by Miguel on 13/02/2018.
 */

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesViewHolder> {

    private Context context;
    private List<Favorites> favoritesList;

    public FavoritesAdapter(Context context, List<Favorites> favoritesList) {
        this.context = context;
        this.favoritesList = favoritesList;
    }

    @Override
    public FavoritesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.favorites_item,parent,false);
        return new FavoritesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FavoritesViewHolder viewHolder, final int position) {
        viewHolder.food_name.setText(favoritesList.get(position).getFoodName());
        viewHolder.food_price.setText(String.format("$ %s",favoritesList.get(position).getFoodPrice().toString()));

        Picasso.with(context).load(favoritesList.get(position).getFoodImage()).into(viewHolder.food_image);


        //quick cart
        viewHolder.quick_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isExists = new DatabaseKK(context).checkFoodExists(favoritesList.get(position).getFoodId(), Common.currentUser.getPhone());
                if(!isExists) {
                    new DatabaseKK(context).addToCart(new Order(
                            Common.currentUser.getPhone(),
                            favoritesList.get(position).getFoodId(),
                            favoritesList.get(position).getFoodName(),
                            "1",
                            favoritesList.get(position).getFoodPrice(),
                            favoritesList.get(position).getFoodDiscount(),
                            favoritesList.get(position).getFoodImage()));

                }else {
                    new DatabaseKK(context).increaseCart(Common.currentUser.getPhone(), favoritesList.get(position).getFoodId());
                }
                Toast.makeText(context, "Adicionado ao Carrinho", Toast.LENGTH_SHORT).show();
            }
        });


        final Favorites local = favoritesList.get(position);
        viewHolder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                //Toast.makeText(FoodList.this, ""+local.getName(), Toast.LENGTH_SHORT).show();

                //start the activity
                Intent foodDetail = new Intent(context,FoodDetail.class);
                foodDetail.putExtra("FoodId", favoritesList.get(position).getFoodId());    //send food to new activity
                context.startActivity(foodDetail);
            }
        });

    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    public void removeItem(int position){
        favoritesList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Favorites item, int position){
        favoritesList.add(position,item);
        notifyItemInserted(position);
    }

    public Favorites getItem(int position){
        return favoritesList.get(position);
    }
}
