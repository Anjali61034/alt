package com.navigine.navigine.demo.utils

import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.models.VenueIconObj


object VenueIconsListProvider {
    @JvmField
    var VenueIconsList: MutableList<VenueIconObj?> = ArrayList<VenueIconObj?>()

    init {
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_lift, "Lifts"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_stairs, "Stairs"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_cosmetics, "Cosmetics"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_jewellery, "Jewellery"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_gifts, "Gifts"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_toilet, "Toilet"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_cloakroom, "Cloakrooms"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_traveller_goods, "Traveller"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_meeting_rooms, "Meeting"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_police, "Police"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_services, "Services"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_wifi, "Wifi"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_train, "Train"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_general, "General"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_escalator, "Escalator"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_car_services, "Car"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_atm_banks, "Banking"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_florists, "Florists"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_pets, "Pets"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_footwear, "Footwear"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_food, "Cafes"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_beauty, "Hairdressing & Beauty"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_homeware, "Homeware"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_leisure, "Leisure"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_music, "Music"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_newsagents, "Newsagents"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_optometrists, "Optometrists"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_phones, "Phones"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_pharmacies, "Pharmacies"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_supermarket, "Supermarket"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_clothing, "Clothing"))
        VenueIconsList.add(VenueIconObj(R.drawable.ic_venue_children_room, "Children"))
    }
}