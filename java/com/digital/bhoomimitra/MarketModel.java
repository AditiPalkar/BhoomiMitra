package com.digital.bhoomimitra;

import java.util.List;

public class MarketModel {
    public List<Record> records;

    public static class Record {
        public String state;
        public String district;
        public String market;
        public String commodity;
        public String variety;
        public String arrival_date;
        public String min_price;
        public String max_price;
        public String modal_price;

        // ADDED: To store distance for sorting
        public double distance = 0.0;

        // NOTE: The Gov API usually doesn't give Lat/Long.
        // We will simulate Lat/Long for the demo to make the sorting work.
        public double latitude = 0.0;
        public double longitude = 0.0;
    }
}