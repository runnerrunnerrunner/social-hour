package socialhour.socialhour;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import socialhour.socialhour.model.EventData;
import socialhour.socialhour.model.EventItem;
import socialhour.socialhour.model.FriendData;
import socialhour.socialhour.model.FriendItem;
import socialhour.socialhour.model.GroupData;
import socialhour.socialhour.model.GroupItem;
import socialhour.socialhour.model.PrivateUserData;
import socialhour.socialhour.model.PublicUserData;
import socialhour.socialhour.tools.FirebaseData;

import static socialhour.socialhour.tools.FirebaseData.decodeEmail;
import static socialhour.socialhour.tools.FirebaseData.encodeEmail;


public class frontend_activity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    /*
            These values tell onActivityResult how to process the data from
            the activity. The actual value of the int doesn't matter.
     */
    private static final int request_code_add_event = 5;
    private static final int request_code_add_friend = 6;
    private static final int request_code_add_group = 7;
    private static final int request_code_edit_settings = 8;
    private static final int request_code_edit_event = 9;
    private static final int request_code_open_calendar = 10;

    private dashboard d;
    private friends_menu f;
    private groups_menu g;

    private FirebaseUser current_user_firebase;

    private boolean firstRun;

    private DatabaseReference public_event_database;
    private DatabaseReference private_user_database;
    private DatabaseReference public_user_database;

    private DatabaseReference friend_connection_database;
    private DatabaseReference group_database;

    private FirebaseDatabase fDatabase;
    public static PrivateUserData current_user_local;

    final int PRIVACY_PUBLIC = 1;

    private String firebase_email;

    //Moving everything out here because this was being activated before the private user data
    //was being loaded

    protected void addPublicEventListener(){
        public_event_database.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                EventItem event = dataSnapshot.getValue(EventItem.class);
                boolean duplicate_event = false;
                if(EventData.find_event(event) != -1){
                    duplicate_event = true;
                }
                //If the creator either the local usr or one created by a friend
                boolean permissions = false;
                String creator_email = FirebaseData.decodeEmail(event.get_creator().get_email());
                if(creator_email.compareTo(current_user_local.get_email())== 0){
                    permissions = true;
                }
                else if(current_user_local.find_friend(event.get_creator()) != -1 &&
                        event.get_privacy() == PRIVACY_PUBLIC){
                    permissions = true;
                }
                if(!duplicate_event && permissions){
                    EventData.add_event_from_firebase(event);
                    try {
                        d.updateAdapter();
                    } catch (NullPointerException e) {
                        Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                    }
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                EventItem event = dataSnapshot.getValue(EventItem.class);

                //This bit of code calculates whether the user is the event creator, or if the user
                //is friends with the creator and they have it set to public.
                boolean permissions = false;
                String creator_email = FirebaseData.decodeEmail(event.get_creator().get_email());
                if(creator_email.compareTo(current_user_local.get_email())== 0)
                    permissions = true;
                else if(current_user_local.find_friend(event.get_creator()) != -1 &&
                        event.get_privacy() == PRIVACY_PUBLIC)
                    permissions = true;

                //This bit of code checks whether or no
                boolean duplicate_event = false;
                if(EventData.find_event(event) != -1){
                    duplicate_event = true;
                }

                //Dunno why intellij is crying; we've literally hit the two lower test conditions
                if(permissions && duplicate_event) //event is cool there, we just need to update
                    EventData.modify_event_from_firebase(event);
                else if(permissions && !duplicate_event) //reached if the user gains permissions
                        EventData.add_event_from_firebase(event);
                else if (!permissions && duplicate_event) //reached if the event goes private
                    EventData.remove_event(event);

                //This lets us modify the adapter without worrying about getting knocked out
                try{
                    d.updateAdapter();
                } catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }
            //If the event no longer exists, let's get rid of it
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                EventItem event = dataSnapshot.getValue(EventItem.class);
                EventData.remove_event(event);
                try{
                    d.updateAdapter();
                } catch(NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("FAILED!!", null, null);
            }
        });
    }
    /*
        Establishes all of the friend connections between users. This isn't necessary for loading
        the events for Dashboard, but rather for loading the Friends page. Dashboard friends are
        monitored by the PrivateUserData class.
     */
    protected void addFriendEventListener(){
        friend_connection_database.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FriendItem friend = dataSnapshot.getValue(FriendItem.class);

                //Strings that we'll use through the course of the algorithm.
                String initiator_email = FirebaseData.decodeEmail(friend.get_initiator().get_email());
                String acceptor_email = FirebaseData.decodeEmail(friend.get_acceptor().get_email());

                //if getEmail() returns null we have a shit ton of problems
                //TODO: Implement some sort of error code to return to the user to handle this

                boolean relevant_connection = true;
                if(initiator_email.compareTo(firebase_email) != 0 &&
                        acceptor_email.compareTo(firebase_email) != 0){
                    relevant_connection = false;
                }

                //If the connection is already in the database, we shouldn't add it.
                //Also, it's definitely a new connection if FriendData.getListData() is null.
                boolean duplicate_connection = false;
                if(relevant_connection && FriendData.getListData() != null) {
                    for (FriendItem e : FriendData.getListData()) {
                        if (e.get_key().compareTo(friend.get_key()) == 0) {
                            duplicate_connection = true;
                            break;
                        }
                    }
                }
                //However, if we should add it
                if(relevant_connection && !duplicate_connection)
                    FriendData.add_connection_from_firebase(friend);


                //now, we'll check to see if there is a newly established friendship to the user
                boolean new_friend_connection = true;
                if(relevant_connection && friend.get_isAccepted()){
                    if(current_user_local.get_friends_list() != null) {
                        ArrayList<PublicUserData> usrlist = current_user_local.get_friends_list();
                        for (PublicUserData usr : usrlist) {
                            if (decodeEmail(usr.get_email()).compareTo(acceptor_email) == 0 ||
                                    decodeEmail(usr.get_email()).compareTo(initiator_email) == 0) {
                                new_friend_connection = false;
                                break;
                            }
                        }
                    }
                }
                if(new_friend_connection && friend.get_isAccepted()){
                    if(initiator_email.compareTo(firebase_email) == 0){
                        current_user_local.add_friend(friend.get_acceptor());
                    }
                    else{
                        current_user_local.add_friend(friend.get_initiator());
                    }
                    private_user_database.setValue(current_user_local);
                }
                try {
                    f.updateAdapter();
                } catch (NullPointerException e) {
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                FriendItem friend = dataSnapshot.getValue(FriendItem.class);
                String initiator_email = FirebaseData.decodeEmail(friend.get_initiator().get_email());
                String acceptor_email = FirebaseData.decodeEmail(friend.get_acceptor().get_email());
                String local_email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                //Test to see if at least one of the parties is the local user.
                boolean relevant_connection = true;
                if(initiator_email.compareTo(local_email) != 0 &&
                        acceptor_email.compareTo(local_email) != 0){
                    relevant_connection = false;
                }
                //If there is a friend connection and it's relevant, test to see if the user already
                //has a local friend involved.
                boolean new_friend_connection = true;
                if(relevant_connection && friend.get_isAccepted()) {
                    ArrayList<PublicUserData> usrlist = current_user_local.get_friends_list();
                    if (usrlist != null) {
                        for (PublicUserData usr : usrlist) {
                            if (decodeEmail(usr.get_email()).compareTo(acceptor_email) == 0 ||
                                    decodeEmail(usr.get_email()).compareTo(initiator_email) == 0) {
                                new_friend_connection = false;
                                break;
                            }
                        }
                    }
                }

                if(relevant_connection){
                    //If the connection at least involves the user, update it.
                    FriendData.update_friend(friend);
                    //If we should add a new friend, do so.
                    if(new_friend_connection){
                        if(initiator_email.compareTo(local_email) == 0){
                            current_user_local.add_friend(friend.get_acceptor());
                        }
                        else{
                            current_user_local.add_friend(friend.get_initiator());
                        }
                    }
                }

                //If this method is being called while we're on another activity, catch it.
                try {
                    f.updateAdapter();
                } catch (NullPointerException e) {
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }

            //if onChildremoved, either the user deleted the friend or denied the request.
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                FriendItem friend = dataSnapshot.getValue(FriendItem.class);
                FriendData.remove_friend(friend.get_key());
                try {
                    f.updateAdapter();
                } catch (NullPointerException e) {
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }
            //these methods never need to be properly overrided due to the nature of our database.
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("FAILED!!", null, null);
            }
        });
    }

    protected void addGroupListener(){
        group_database.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GroupItem group = dataSnapshot.getValue(GroupItem.class);
                String local_email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                boolean isRelevant = false;
                if(group.get_owner().get_email().compareTo(local_email) == 0)
                    isRelevant = true;
                else{
                    if(group.get_members() != null){
                        for(int i = 0; i < group.get_members().size(); i++){
                            if(group.get_members().get(i).get_email().compareTo(local_email) == 0){
                                isRelevant = true;
                                break;
                            }
                        }
                    }
                }
                boolean isDuplicate = false;
                if(GroupData.getListData() != null){
                    for(int i = 0; i < GroupData.get_group_count(); i++){
                        if(group.get_key().compareTo(GroupData.get_group(i).get_key()) == 0){
                            isDuplicate = true;
                            break;
                        }
                    }
                }
                if(!isDuplicate && isRelevant){
                    GroupData.add_group_from_firebase(group);
                    if(group.get_events() != null){
                        for(EventItem e: group.get_events()){
                            if(EventData.find_event(e) == -1)
                                EventData.add_event_from_firebase(e);
                        }
                    }
                }
                try{
                    d.updateAdapter();
                }catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
                try{
                    g.updateAdapter();
                }catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                GroupItem group = dataSnapshot.getValue(GroupItem.class);
                String local_email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                boolean isRelevant = false;
                if(group.get_owner().get_email().compareTo(local_email) == 0)
                    isRelevant = true;
                else{
                    if(group.get_members() != null){
                        for(int i = 0; i < group.get_members().size(); i++){
                            if(group.get_members().get(i).get_email().compareTo(local_email) == 0){
                                isRelevant = true;
                                break;
                            }
                        }
                    }
                }
                if(isRelevant){
                    GroupData.update_group(group);
                    if(group.get_events() != null){
                        for(EventItem e: group.get_events()){
                            if(EventData.find_event(e) == -1)
                                EventData.add_event_from_firebase(e);
                        }
                    }
                }
                try{
                    g.updateAdapter();
                }catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
                try{
                    d.updateAdapter();
                }catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                GroupItem group = dataSnapshot.getValue(GroupItem.class);
                String local_email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                boolean isRelevant = false;
                if(group.get_owner().get_email().compareTo(local_email) == 0)
                    isRelevant = true;
                else{
                    if(group.get_members() != null){
                        for(int i = 0; i < group.get_members().size(); i++){
                            if(group.get_members().get(i).get_email().compareTo(local_email) == 0){
                                isRelevant = true;
                                break;
                            }
                        }
                    }
                }
                if(isRelevant){
                    GroupData.remove_group(group.get_key());
                }
                try{
                    g.updateAdapter();
                }catch (NullPointerException e){
                    Log.d("MainActivity", "WARNING: Can't update adapter because we're not on the main activity!");
                }
            }


            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    /*
        This pulls all data down to Firebase, starting with the data that goes into the
        PrivateUserData class, coming from private_user_database.

        Calls addPublicEventListener and addFriendEventListener to pull necessary data when done.
     */
    protected void pullPrivateDataListener(){
        private_user_database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){
                if(dataSnapshot.exists()) {
                    current_user_local = dataSnapshot.getValue(PrivateUserData.class);
                }
                if(current_user_local == null){
                    current_user_local = new PrivateUserData(current_user_firebase.getDisplayName(),
                            encodeEmail(current_user_firebase.getEmail()),
                            current_user_firebase.getPhotoUrl().toString(),
                            current_user_firebase.getProviderId(), new ArrayList<PublicUserData>(),
                            new ArrayList<GroupItem>(), new ArrayList<EventItem>());
                }
                public_user_database = fDatabase.getReference("public_user_data/" +
                        encodeEmail(current_user_local.get_email()));
                PublicUserData temp_user_data = new PublicUserData(current_user_local.get_photo(),
                        current_user_local.get_display_name(), current_user_local.get_email());
                public_user_database.setValue(temp_user_data);
                if(firstRun) {
                    addFriendEventListener();
                    addPublicEventListener();
                    addGroupListener();
                }
                firstRun = false;
            }
            @Override
            public void onCancelled(DatabaseError databaseError){
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frontend_activity);


        //allows us to get data of the user currently logged into firebase.
        current_user_firebase = FirebaseAuth.getInstance().getCurrentUser();

        firebase_email = current_user_firebase.getEmail();
        if(firebase_email == null){
            Toast.makeText(getApplicationContext(),
                    "ERROR: Not logged in! Sending back to main activity",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        //Fire up the databases that depend on recyclers (events, friends, groups)
        EventData.init();
        FriendData.init();
        GroupData.init();
        //Set up the title bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Social Hour");
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        //instance of FirebaseDatabase that all of the other databases will draw from.
        fDatabase = FirebaseDatabase.getInstance();

        //Grab the user's data from Google Firebase. This will allow us to change user settings
        //later on, and have them persist throughout the application. If the data doesn't already
        //exist, we'll make a new PrivateUserData and throw that shit in Google Firebase.

        /*
               These four databases currently hold everything we're working with in Google Firebase.
               private_user_database -
                        Pushes the data it grabs to PrivateUserData, stored in
                            static object current_user_local
                        REQUIREMEMTS: ANY DATA CHANGE TO PrivateUserData MUST BE PUSHED TO
                            private_user_database!
               public_user_database -
                        Manages all of the current users in the
         */

        private_user_database = fDatabase.getReference("private_user_data/" +
                encodeEmail(firebase_email));
        public_user_database = fDatabase.getReference("public_user_data/" +
                encodeEmail(firebase_email));
        public_event_database = fDatabase.getReference("public_event_data");
        friend_connection_database = fDatabase.getReference("friend_data/");

        //Pulls all of the necessary data from Google Firebase.
        firstRun = true;
        pullPrivateDataListener();

        group_database = fDatabase.getReference("group_data");


        //Sets up the initial behaviour of the persistent floating action buttons.

        //  Fab: Responsible for starting and finishing the activities adding events, friends,
        //      and groups.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] group_list = GroupData.get_group_names();
                Intent i = new Intent(getApplicationContext(), add_event_activity.class);
                i.putExtra("group_list", group_list);
                i.putExtra("request_code", request_code_add_event);
                startActivityForResult(i, request_code_add_event);
            }
        });

        //  Fabcal: Responsible for starting the calendar activity.
        FloatingActionButton fabcal = (FloatingActionButton) findViewById(R.id.fabcal);
        fabcal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), calendar_activity.class);
                startActivityForResult(i, request_code_open_calendar);
            }
        });
        /*
            Page Change listener that detects when the user flips a page, and changes the
            appearance and function accordingly. Currently, only the second button changes
            behaviour based on the page.
         */
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            public void onPageSelected(int position) {
                if(position == 0) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.pastel_red)));
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_testedit));
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String[] group_list = GroupData.get_group_names();
                            Intent i = new Intent(getApplicationContext(), add_event_activity.class);
                            i.putExtra("group_list", group_list);
                            i.putExtra("request_code", request_code_add_event);
                            startActivityForResult(i, request_code_add_event);
                        }
                    });
                }
                else if(position == 1) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.pastel_yellow)));
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_person_add_black_24dp));
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ArrayList<String> friend_list = FriendData.get_friends_emails(current_user_local.get_email());
                            ArrayList<String> request_list = FriendData.get_requests
                                    (FirebaseData.decodeEmail(current_user_local.get_email()));
                            Intent i = new Intent(getApplicationContext(), add_friends_activity.class);
                            i.putStringArrayListExtra("email_list", friend_list);
                            i.putStringArrayListExtra("request_list", request_list);
                            startActivityForResult(i, request_code_add_friend);
                        }
                    });
                }
                else if(position == 2) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.pastel_orange)));
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_group_add_black_24dp));
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ArrayList<String> friend_list = FriendData.get_friends_emails(current_user_local.get_email());
                            Intent i = new Intent(getApplicationContext(), add_group_activity.class);
                            i.putStringArrayListExtra("email_list", friend_list);
                            startActivityForResult(i, request_code_add_group);
                        }
                    });
                }

            }
        });
        if(current_user_local != null) {
            private_user_database.setValue(current_user_local);
        }
    }
    /*
        This is called when any of the subactivities finishes.
        Grabs all of the intent data from the subactivity, and either creates a new event, creates
        a new group, or modifies the user's settings based on the exit
        code of the activity and what activity actually exited.
    */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //First IF block: handle all of the incoming data from the add event activiy, provided
        //the user successfully creates an event
        if (requestCode == request_code_add_event && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();

            //grab the start and end date from the activity
            long start_date_millis = extras.getLong("start_date_millis");
            long end_date_millis = extras.getLong("end_date_millis");
            String start_date_timezone = extras.getString("start_date_timezone");
            String end_date_timezone = extras.getString("end_date_timezone");
            Calendar start_date = Calendar.getInstance();
            start_date.setTimeZone(TimeZone.getTimeZone(start_date_timezone));
            Calendar end_date = Calendar.getInstance();
            end_date.setTimeZone(TimeZone.getTimeZone(end_date_timezone));
            start_date.setTimeInMillis(start_date_millis);
            end_date.setTimeInMillis(end_date_millis);
            Date start_time = start_date.getTime();
            Date end_time = end_date.getTime();

            //get the privacy, event name, all day status, and event location
            int privacy = extras.getInt("event_privacy");
            String name = extras.getString("event_name");
            String location = extras.getString("event_location");
            boolean is_all_day = extras.getBoolean("is_all_day");

            String group = extras.getString("group");

            Date creation_date = new Date();

            if(group.compareTo("None") == 0){
                //set the creation date
                EventItem event = new EventItem(start_time, end_time, is_all_day,
                        name, location, privacy, current_user_local.getPublicData(),
                        creation_date, false);
                EventData.add_event_to_firebase(event);

                //add the event to the private user database aswell
                current_user_local.add_event(event);
                private_user_database.setValue(current_user_local);
            }
            else{
                EventItem item = new EventItem(start_time, end_time, is_all_day,
                        name, location, privacy,
                        new PublicUserData(current_user_local.get_photo(), group, group+"@socialHour.com"), creation_date, true);
                item.set_id(public_event_database.push().getKey());
                GroupData.add_event_to_group_firebase(group, item);
            }
            try {
                d.updateAdapter();
            }
            catch(NullPointerException e){}
            try{
                g.updateAdapter();
            }
            catch(NullPointerException e){}
        }
        else if (requestCode == request_code_edit_event && resultCode == RESULT_OK){
            //Grab the start and end date from the activity
            long start_date_millis = data.getExtras().getLong("start_date_millis");
            long end_date_millis = data.getExtras().getLong("end_date_millis");
            String start_date_timezone = data.getExtras().getString("start_date_timezone");
            String end_date_timezone = data.getExtras().getString("end_date_timezone");
            Calendar start_date = Calendar.getInstance();
            start_date.setTimeZone(TimeZone.getTimeZone(start_date_timezone));
            Calendar end_date = Calendar.getInstance();
            end_date.setTimeZone(TimeZone.getTimeZone(end_date_timezone));
            start_date.setTimeInMillis(start_date_millis);
            end_date.setTimeInMillis(end_date_millis);
            Date start_time = start_date.getTime();
            Date end_time = end_date.getTime();

            //get the privacy, event name, all day status, and event location
            int privacy = data.getExtras().getInt("event_privacy");
            String name = data.getExtras().getString("event_name");
            String location = data.getExtras().getString("event_location");
            boolean is_all_day = data.getExtras().getBoolean("is_all_day");

            //Update the creation date
            Date creation_date = new Date();

            //create the event and set the key to the same version of the key
            EventItem event = new EventItem(start_time, end_time, is_all_day,
                    name, location, privacy, current_user_local.getPublicData(),
                    creation_date, false);
            event.set_id(data.getExtras().getString("key"));

            //have eventdata replace the event
            EventData.modify_event_to_firebase(event);

            d.updateAdapter();
            current_user_local.modify_event(event);
            private_user_database.setValue(current_user_local);
        }

        //Second if block: user enters edit creation but cancels
        else if ((requestCode == request_code_add_event || resultCode == request_code_edit_event) &&
                resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Event creation cancelled.", Toast.LENGTH_SHORT).show();
        }
        //Third if block: user exits friend adding
        else if (requestCode == request_code_add_friend){
            f.updateAdapter();
        }
        //Fourth if block: user exits settings modification
        else if (requestCode == request_code_edit_settings){ //currently we don't allow the user to cancel
            current_user_local.set_display_name(data.getExtras().getString("display_name"));
            current_user_local.set_pref_default_privacy(data.getExtras().getInt("privacy"));
            current_user_local.set_pref_display_24hr(data.getExtras().getBoolean("is_24_hr"));
            private_user_database.setValue(current_user_local);
            PublicUserData temp_user_data = new PublicUserData(current_user_local.get_photo(),
                    current_user_local.get_display_name(), current_user_local.get_email());
            public_user_database.setValue(temp_user_data);
            d.resetAdapter();
        }
        else if(requestCode == request_code_add_group){
            if(resultCode == RESULT_OK){
                String description = data.getExtras().getString("description");
                String name = data.getExtras().getString("name");
                PublicUserData owner = current_user_local.getPublicData();
                ArrayList<String> emails = data.getStringArrayListExtra("email_list");
                if(current_user_local.convert_emails_to_users(emails) != null){
                    ArrayList<PublicUserData> members = current_user_local.convert_emails_to_users(emails);
                    GroupItem result = new GroupItem(new Date(), owner, members, description,
                            new ArrayList<EventItem>(), name, "NULL");
                    GroupData.add_group_to_firebase(result);
                }
            }
            else{
                Toast.makeText(this, "Group creation cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
        Overrides OnCreateOptionsMenu so that we can add an entry for entering the Settings
        activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_frontend_activity, menu);
        return true;
    }


    /*'
        Provides implementation for clicking a icon in the menu at the top right:
        For the foreseeable future, the only use for the menu is entering the Settings activity.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), edit_settings_activity.class);
            //We'll throw in the current user's settings so that we can automatically set them
            //in the settings activity.
            i.putExtra("display_name", current_user_local.get_display_name());
            i.putExtra("is_24_hr", current_user_local.get_pref_display_24hr());
            i.putExtra("privacy", current_user_local.get_pref_default_privacy());
            //start the settings activity
            startActivityForResult(i, request_code_edit_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch(position){
                case 0:
                    return new dashboard();
                case 1:
                    return new friends_menu();
                case 2:
                    return new groups_menu();
                default:
                    throw new RuntimeException("Error: Page not Found! If you're seeing this " +
                            "message please contact the developers.");
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Dashboard";
                case 1:
                    return "Friends";
                case 2:
                    return "Groups";
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    d = (dashboard) createdFragment;
                    break;
                case 1:
                    f = (friends_menu) createdFragment;
                    break;
                case 2:
                    g = (groups_menu) createdFragment;
                    break;
            }
            return createdFragment;
        }
    }
}
