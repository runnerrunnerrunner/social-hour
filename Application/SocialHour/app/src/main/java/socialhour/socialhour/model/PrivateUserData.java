package socialhour.socialhour.model;

/**
 * Created by michael on 5/9/17.
 */

import java.util.ArrayList;
import java.util.Date;

import static java.util.Collections.sort;

public class PrivateUserData {
    private ArrayList<PublicUserData> friends_list;
    private ArrayList<GroupItem> group_list;
    private ArrayList<EventItem> event_list;
    private String display_name;
    private String email;
    private String photo;
    private String provider_id;
    private Date date_created;
    private boolean pref_display_24hr;
    private int pref_default_privacy;

    final int PRIVACY_PUBLIC = 1;
    final int PRIVACY_PRIVATE = 2;

    public PrivateUserData(String display_name, String email, String photo, String provider_id,
                           ArrayList<PublicUserData> p_friends_list,
                           ArrayList<GroupItem> p_group_list,
                           ArrayList<EventItem> p_event_list){
        this.display_name = display_name;
        this.email = email;
        this.photo = photo;
        this.provider_id = provider_id;
        friends_list = new ArrayList<PublicUserData>(p_friends_list);
        group_list = new ArrayList<GroupItem>(p_group_list);
        event_list = new ArrayList<EventItem>(p_event_list);
        date_created = new Date();
        this.pref_display_24hr = false;
        pref_default_privacy = PRIVACY_PUBLIC;
    }

    public PrivateUserData(){
        //empty constructor for god knows what reason why
    }


    public String get_display_name(){return display_name;}
    public String get_email(){return email;}
    public String get_photo(){return photo;}
    public String get_provider_id() {return provider_id;}
    public ArrayList<PublicUserData> get_friends_list() {return friends_list;}
    public ArrayList<GroupItem> get_group_list() {return group_list;}
    public ArrayList<EventItem> get_event_list() {return event_list;}
    public Date get_date_created() {return date_created;}
    public boolean get_pref_display_24hr() {return pref_display_24hr;}
    public int get_pref_default_privacy() {return pref_default_privacy;}

    public void sort_event_list(){
        sort(event_list);
    }
    public void add_event(EventItem event){
        if(event_list == null){
            event_list = new ArrayList<EventItem>();
        }
        event_list.add(event);
        sort(event_list);
    }
    public void add_friend(PublicUserData user){
        friends_list.add(user);
        sort(friends_list);
    }


    public void set_display_name(String display_name){this.display_name = display_name;}
    public void set_email(String email){this.email = email;}
    public void set_photo(String photo){this.photo = photo;}
    public void set_provider_id(String provider_id){this.provider_id = provider_id;}
    public void set_date_created(Date date_created){this.date_created = date_created;}
    public void set_event_list(ArrayList<EventItem> event_list){this.event_list = event_list;}
    public void set_friend_list(ArrayList<PublicUserData> friends_list){this.friends_list = friends_list;}
    public void set_group_list(ArrayList<GroupItem> group_list){this.group_list = group_list;}
    public void set_pref_display_24hr(boolean pref_display_24hr){this.pref_display_24hr = pref_display_24hr;}
    public void set_pref_default_privacy(int pref_default_privacy){this.pref_default_privacy = pref_default_privacy;}

}