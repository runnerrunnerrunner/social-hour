package socialhour.socialhour.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

import socialhour.socialhour.R;
import socialhour.socialhour.frontend_activity;
import socialhour.socialhour.model.FriendData;
import socialhour.socialhour.model.FriendItem;
import socialhour.socialhour.model.PublicUserData;
import socialhour.socialhour.tools.FirebaseData;

import static java.util.Collections.sort;

/**
 * Serves as an adapter for searching for new friends
 * Created by michael on 5/3/17.
 */

public class Public_User_Search_Adapter
        extends RecyclerView.Adapter<Public_User_Search_Adapter.ViewHolder> implements Filterable{
    private ArrayList<PublicUserData> fArrayList;
    private ArrayList<PublicUserData> fFilteredList;
    private ArrayList<String> requests;
    private  Context context;

    public Public_User_Search_Adapter(ArrayList<PublicUserData> arrayList, Context context, ArrayList<String> requests) {
        fArrayList = arrayList;
        fFilteredList = arrayList;
        this.context = context;
        this.requests = requests;
    }

    public boolean if_request(String email){
        if(requests != null){
            for(int i = 0; i < requests.size(); i++){
                if(FirebaseData.decodeEmail(email)
                        .compareTo(FirebaseData.decodeEmail(requests.get(i))) == 0)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Public_User_Search_Adapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.friend_result_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Public_User_Search_Adapter.ViewHolder viewHolder, int i){
        viewHolder.friends_text.setText(fFilteredList.get(i).get_display_name() + " (" +
                fFilteredList.get(i).get_email() +")");
        viewHolder.friend = fFilteredList.get(i);
        viewHolder.publicData = frontend_activity.current_user_local.getPublicData();
        if(if_request(fFilteredList.get(i).get_email())){
            viewHolder.add_button.setImageResource(R.drawable.ic_timer_black_24dp);
        }
        else{
            viewHolder.add_button.setImageResource(R.drawable.ic_add_black_24dp);
        }
        Picasso.with(context).load(fFilteredList.get(i).get_profile_picture()).into(viewHolder.imageView);
    }

    @Override
    public int getItemCount() {
        return fFilteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    fFilteredList = fArrayList;
                } else {
                    ArrayList<PublicUserData> filteredList = new ArrayList<>();
                    for (PublicUserData f : fArrayList) {
                        if (f.get_display_name().toLowerCase().contains(charString) || f.get_email().toLowerCase().contains(charString)) {
                            filteredList.add(f);
                        }
                    }
                    fFilteredList = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = fFilteredList;
                return filterResults;
            }
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                fFilteredList = (ArrayList<PublicUserData>) filterResults.values;
                sort(fFilteredList);
                notifyDataSetChanged();
            }
        };
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView friends_text;
        private ImageView imageView;
        private ImageButton add_button;
        private FriendItem new_connection;
        private PublicUserData friend;
        private PublicUserData publicData;
        private String local_email;

        public ViewHolder(View view) {
            super(view);
            friends_text = (TextView) view.findViewById(R.id.friends_text);
            imageView = (ImageView) view.findViewById(R.id.friendImageView);
            add_button = (ImageButton) view.findViewById(R.id.add_friend_button);
            local_email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            add_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!ifConnectionExists(friend)){
                        new_connection = new FriendItem(publicData, friend, new Date(), null, false);
                        FriendData.add_connection_to_firebase(new_connection);
                        add_button.setImageResource(R.drawable.ic_timer_black_24dp);
                    }
                }
            });
        }
        public boolean ifConnectionExists(PublicUserData other){
            for(FriendItem f : FriendData.getListData()) {
                if (FirebaseData.decodeEmail(f.get_initiator().get_email())
                        .compareTo(local_email) == 0) {
                    if (FirebaseData.decodeEmail(f.get_acceptor().get_email())
                            .compareTo(FirebaseData.decodeEmail(other.get_email())) == 0) {
                        return true;
                    }
                } else if (FirebaseData.decodeEmail(f.get_initiator().get_email())
                        .compareTo(FirebaseData.decodeEmail(other.get_email())) == 0) {
                    if (FirebaseData.decodeEmail(f.get_acceptor().get_email())
                            .compareTo(local_email) == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}