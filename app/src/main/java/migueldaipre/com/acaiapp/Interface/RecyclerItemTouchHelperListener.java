package migueldaipre.com.acaiapp.Interface;

import android.support.v7.widget.RecyclerView;

/**
 * Created by Miguel on 13/02/2018.
 */

public interface RecyclerItemTouchHelperListener {
    void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
}
