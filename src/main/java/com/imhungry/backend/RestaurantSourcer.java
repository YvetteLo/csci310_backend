package com.imhungry.backend;

import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.NearbySearchRequest;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by calebthomas on 2/28/19.
 */
public class RestaurantSourcer {

    public static List<Restaurant> searchRestaurants(String keyword, int maxRestaurants) {
        List<Restaurant> restaurants = new ArrayList<>();


        // Get all restaurants from google API
        // TODO: create better method for storing API key
        LatLng tommy = new LatLng(34.020633, -118.285468);
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("API KEY")
                .build();

        NearbySearchRequest req = new NearbySearchRequest(geoApiContext);
        req.location(tommy)
                .keyword(keyword)
                .rankby(RankBy.DISTANCE)
                .custom("type", "restaurant");

        PlacesSearchResponse response = req.awaitIgnoreError();

        PlacesSearchResult[] placesSearchResults = response.results;
        int resultsIndex = 0;
        int i = 0;
        while(i < maxRestaurants && resultsIndex < placesSearchResults.length) {

            // Get distance from tommy trojan to the restaurant
            DistanceMatrixApiRequest distanceRequest = new DistanceMatrixApiRequest(geoApiContext);
            distanceRequest.origins(tommy).destinations(placesSearchResults[resultsIndex].vicinity);
            DistanceMatrix distanceResponse = null;
            try {
                distanceResponse = distanceRequest.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Get detailed information about the location
            PlaceDetailsRequest placeDetailsRequest = new PlaceDetailsRequest(geoApiContext);
            placeDetailsRequest.placeId(placesSearchResults[resultsIndex].placeId);
            PlaceDetails placeDetails = null;

            try {
                placeDetails = placeDetailsRequest.await();
            } catch(Exception e) {
                e.printStackTrace();
            }

            restaurants.add(new Restaurant(
                    placesSearchResults[resultsIndex].placeId,
                    placesSearchResults[resultsIndex].name,
                    placesSearchResults[resultsIndex].vicinity,
                    placeDetails.formattedPhoneNumber,
                    placeDetails.website,
                    placesSearchResults[resultsIndex].rating,
                    placeDetails.priceLevel,
                    distanceResponse.rows[0].elements[0].duration.humanReadable
            ));

            // iterate
            i++;
            resultsIndex++;

            // Get next page
            if(resultsIndex == placesSearchResults.length) {

                req = new NearbySearchRequest(geoApiContext);
                req.pageToken(response.nextPageToken);
                placesSearchResults = req.awaitIgnoreError().results;
                resultsIndex = 0;
            }
        }

        return restaurants;
    }

    public static Restaurant getRestaurantDetails(String placeId) {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("API KEY")
                .build();

        PlaceDetailsRequest req = new PlaceDetailsRequest(geoApiContext);
        req.placeId(placeId);

        PlaceDetails place = req.awaitIgnoreError();

        return new Restaurant(
                placeId,
                place.name,
                place.formattedAddress,
                place.formattedPhoneNumber,
                place.website,
                place.rating,
                place.priceLevel,
                "Placeholder distance"
        );
    }
}