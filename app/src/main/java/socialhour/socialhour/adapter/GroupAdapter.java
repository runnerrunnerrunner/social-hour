package socialhour.socialhour.adapter;

import android.app.LauncherActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import socialhour.socialhour.R;

import socialhour.socialhour.model.*;

/**
 *
 * Created by michael on 3/15/17.
 */

import java.util.ArrayList;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {

    private LayoutInflater inflater;
    private Context context;

    public GroupAdapter(Context c) {
        this.inflater = LayoutInflater.from(c);
        context = c;
    }

    @Override
    public GroupHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.group_item, parent, false);
        return new GroupHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupHolder holder, int position) {
        GroupItem item = GroupData.get_group(position);
        holder.title.setText(item.get_name());
        holder.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_groups_white_24dp));
    }

    @Override
    public int getItemCount() {
        return GroupData.getListData().size();
    }

    class GroupHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private ImageView icon;
        private View container;
        public GroupHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.group_list_text);
            icon = (ImageView) itemView.findViewById(R.id.group_list_icon);
            container = itemView.findViewById(R.id.cont_group_root);
        }

    }

}
