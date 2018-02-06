package migueldaipre.com.acaiapp.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import migueldaipre.com.acaiapp.Cart;
import migueldaipre.com.acaiapp.Database.DatabaseKK;
import migueldaipre.com.acaiapp.Model.Order;
import migueldaipre.com.acaiapp.R;
import migueldaipre.com.acaiapp.ViewHolder.CartViewHolder;


public class CartAdapter extends RecyclerView.Adapter<CartViewHolder> {

    private List<Order> listData = new ArrayList<>();
    private Cart cart;

    public CartAdapter(List<Order> listData, Cart cart) {
        this.listData = listData;
        this.cart = cart;
    }

    @Override
    public CartViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(cart);
        View itemView = inflater.inflate(R.layout.cart_layout,parent,false);
        return new CartViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(CartViewHolder holder, final int position) {

        Picasso.with(cart.getBaseContext())
                .load(listData.get(position).getImage())
                .resize(70,70)
                .centerCrop()
                .into(holder.cart_image);

        holder.btn_quantity.setNumber(listData.get(position).getQuantity());

        holder.btn_quantity.setOnValueChangeListener(new ElegantNumberButton.OnValueChangeListener() {
            @Override
            public void onValueChange(ElegantNumberButton view, int oldValue, int newValue) {
                Order order = listData.get(position);
                order.setQuantity(String.valueOf(newValue));
                new DatabaseKK(cart).updateCart(order);

                //calculate total price
                int total = 0;
                List<Order> orders = new DatabaseKK(cart).getCarts();

                for (Order item : orders)    {
                    //total += (Integer.parseInt(order.getPrice())) * (Integer.parseInt(item.getQuantity()));

                    total += ((Integer.parseInt(order.getPrice())) * (Integer.parseInt(order.getQuantity())))
                            - ((Integer.parseInt(order.getDiscount())) * (Integer.parseInt(order.getQuantity())));


                    Locale locale = new Locale("en","US");
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                    cart.txtTotalPrice.setText(fmt.format(total));
                }
            }
        });

            Locale locale = new Locale("en","US");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            //int price = (Integer.parseInt(listData.get(position).getPrice()))*(Integer.parseInt(listData.get(position).getQuantity()));
            int price = ((Integer.parseInt(listData.get(position).getPrice()))*(Integer.parseInt(listData.get(position).getQuantity())))
                    - ((Integer.parseInt(listData.get(position).getDiscount())) * (Integer.parseInt(listData.get(position).getQuantity())));

            holder.txt_price.setText(fmt.format(price));
            holder.txt_cart_name.setText(listData.get(position).getProductName());
    }
    @Override
    public int getItemCount() {
        return listData.size();
    }
}