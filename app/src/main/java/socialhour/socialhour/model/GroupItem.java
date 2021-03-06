package socialhour.socialhour.model;

/**
 * Name: GroupItem.java
 * Author: Michael Rinehart
 * Organization: Drexel University
 * Date: 7 June 2017
 * Purpose: The official data structure for maintaining groups.
 *         The Group object controls who is the admin, who are the members, the creation date,
 *         the key for the entry in the database, any events associated with the group, the name,
 *         and the description of the group.
 */

import java.util.ArrayList;
import java.util.Date;

import socialhour.socialhour.tools.FirebaseData;


public class GroupItem{

    private String name;
    private String description;
    private ArrayList<PublicUserData> members;
    private ArrayList<EventItem> events;
    private PublicUserData owner;
    private Date creation_date;
    private String key;

    public GroupItem(Date creation_date, PublicUserData group_owner,
                     ArrayList<PublicUserData> members,
                     String description, ArrayList<EventItem> events,  String name, String key) {
        this.creation_date = creation_date;
        this.owner = group_owner;
        this.members = members;
        this.name = name;
        this.description = description;
        this.events = events;
        this.key = key; //KEY MUST BE SET BY OTHER CLASS
    }

    public GroupItem(){
        //we need a constructor with no arguments or Firebase will complain.
    }

    /*
        ACCESSORS - EACH RESPECTIVE METHOD MERELY RETURNS THE VALUE
     */
    public String get_name(){return this.name;}
    public String get_description(){return this.description;}
    public ArrayList<PublicUserData> get_members(){return this.members;}
    public ArrayList<EventItem> get_events(){return this.events;}
    public PublicUserData get_owner(){return this.owner;}
    public Date get_creation_date(){return this.creation_date;}
    public String get_key(){return this.key;}
    /*
        BASIC MUTATORS - EACH RESPECTIVE METHOD MERELY MODIFIES THE VALUE
                        IGNORE THE FACT THAT THESE "AREN'T" USED
        TODO: Change all of these to return false if value failed to update
     */
    public void set_name(String name){this.name = name;}
    public void set_description(String description){this.description = description;}
    public void set_events(ArrayList<EventItem> events){this.events = events;}
    public void set_members(ArrayList<PublicUserData> members){this.members = members;}
    public void set_owner(PublicUserData owner){this.owner = owner;}
    public void set_creation_date(Date creation_date){this.creation_date = creation_date;}
    public void set_key(String key){this.key = key;}

    /*
        ARRAYLIST MODIFIERS FOR SPECIFIC GROUPS
     */
    /*
        Adds a member to the members arraylist.
        Checks and makes sure that the member already isn't in there first, however.
     */
    public void add_member(PublicUserData member){
        boolean should_add = true;
        if(members != null) {
            for (PublicUserData usr : members) {
                //check to make sure the member isn't already in the arraylist
                if (FirebaseData.decodeEmail(member.get_email())
                        .compareTo(FirebaseData.decodeEmail(usr.get_email())) == 0) {
                    should_add = false;
                    break;
                }
            }
        }
        if(should_add)
            members.add(member);
    }

    public void add_event(EventItem event){
        if(events == null){
            events = new ArrayList<>();
            events.add(event);
        }
        else{
            events.add(event);
        }
    }

    /*
        Removes a member from the members arraylist.
        Returns a boolean to check if it was actually removed.
     */
    public boolean remove_member(PublicUserData member){
        if(members != null) {
            for (int i = 0; i < members.size(); i++) {
                if (FirebaseData.decodeEmail(members.get(i).get_email())
                        .compareTo(FirebaseData.decodeEmail(member.get_email())) == 0) {
                    members.remove(i);
                    return true;
                }
            }
        }
        return false;
    }
}