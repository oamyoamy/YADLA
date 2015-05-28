package com.kludgenics.cgmlogger.data.logdata.location.api

import android.location.Location
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.PlaceBuffer
import com.google.android.gms.location.places.PlaceFilter
import com.google.android.gms.location.places.PlaceLikelihoodBuffer
import com.google.android.gms.location.places.Places
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.kludgenics.cgmlogger.data.logdata.glucose.data.BloodGlucose
import com.kludgenics.cgmlogger.data.logdata.location.data.GeocodedLocation
import com.kludgenics.cgmlogger.data.logdata.location.data.GooglePlacesLocation
import com.kludgenics.cgmlogger.data.logdata.location.data.Position
import retrofit.RestAdapter
import retrofit.converter.GsonConverter
import retrofit.http.GET
import retrofit.http.Query
import rx.Observable
import rx.Subscriber
import rx.functions.Func1
import com.kludgenics.cgmlogger.data.logdata.glucose.data.asMgDl
import rx.lang.kotlin.*
import java.util.Date
import java.util.concurrent.TimeUnit
import com.kludgenics.cgmlogger.extension.*
/**
 * Created by matthiasgranberry on 5/23/15.
 */
class GooglePlacesApi(private val mClient: GoogleApiClient) : GeoApi {
    private val mGooglePlacesEndpoint: NearbySearchEndpoint

    {
        val adapter = RestAdapter.Builder().setEndpoint(PLACES_API_URL).setLogLevel(RestAdapter.LogLevel.FULL).setConverter(GsonConverter(GsonBuilder().excludeFieldsWithoutExposeAnnotation().create())).build()
        mGooglePlacesEndpoint = adapter.create<NearbySearchEndpoint>(javaClass<NearbySearchEndpoint>())
    }

    fun handleBg(bg: BloodGlucose) {
      bg.asMgDl()
    }
    override fun getCurrentLocation(): Observable<GeocodedLocation> {
        return getCurrentLocation("")
    }

    override fun getCurrentLocation(categories: String): Observable<GeocodedLocation> {
        // (1/2)* erfc(0.0059709(-118.7)) - (1/2)* erfc(0.0059709(118.7))
        // erf ~= 1- (1/(1 + .278393*x + .230389*x*x + .000972*x*x*x + .078108*x*x*x*x)^4)
        if (!mClient.isConnected())
            return Observable.empty()
        if (categories == "" || LocationServices.FusedLocationApi.getLocationAvailability(mClient).isLocationAvailable() == false) {
            return deferredObservable {
                Observable.create<GeocodedLocation>(object : Observable.OnSubscribe<GeocodedLocation> {
                    override fun call(subscriber: Subscriber<in GeocodedLocation>) {
                        val result = Places.PlaceDetectionApi.getCurrentPlace(mClient, null)

                        result.setResultCallback(object : ResultCallback<PlaceLikelihoodBuffer> {
                            override fun onResult(placeLikelihoods: PlaceLikelihoodBuffer) {
                                val attribution = placeLikelihoods.getAttributions()
                                placeLikelihoods.toObservable()
                                        .map { GooglePlacesLocation(it.getPlace(), it.getLikelihood(), attribution) }
                                        .subscribe(subscriber)
                                placeLikelihoods.release()
                            }
                        })
                    }
                }).cache()
            }
        } else {
            val location = LocationServices.FusedLocationApi.getLastLocation(mClient)
            Log.d(TAG, "Location: ${Date(location.getTime())} ${location.getLatitude()},${location.getLongitude()} ${location.getAccuracy()}")
            val responseObservable = mGooglePlacesEndpoint.nearbySearchObservable(MAPS_API_KEY, "${location.getLatitude()},${location.getLongitude()}", location.getAccuracy() * 2, categories)

            // Perform a Places API query on the returned results of the filtered WS call

            return deferredObservable {
                responseObservable.flatMap<GooglePlacesWebLocation>(RESPONSE_LOCATION_FUNC).flatMap<String>(LOCATION_ID_FUNC).toList()
                        .take(10) // API docs indicate a limit of 10 ID
                        .flatMap<GeocodedLocation>(object : Func1<List<String>, Observable<GeocodedLocation>> {
                            override fun call(placeIds: List<String>): Observable<GeocodedLocation> {
                                val filter = PlaceFilter(true, placeIds)
                                return Observable.create<GeocodedLocation>(object : Observable.OnSubscribe<GeocodedLocation> {
                                    override fun call(subscriber: Subscriber<in GeocodedLocation>) {
                                        Log.d(TAG, "filter: " + filter.getPlaceIds().join("|"))
                                        val result = Places.PlaceDetectionApi.getCurrentPlace(mClient, filter)

                                        result.setResultCallback(object : ResultCallback<PlaceLikelihoodBuffer> {
                                            override fun onResult(placeLikelihoods: PlaceLikelihoodBuffer) {
                                                val attribution = placeLikelihoods.getAttributions()
                                                val retObservable = placeLikelihoods.toObservable()
                                                        .filter {
                                                            val latlng = it.getPlace().getLatLng()
                                                            val loc = Location("mock")
                                                            loc.setLatitude(latlng.latitude)
                                                            loc.setLongitude(latlng.longitude)
                                                            val place = it.getPlace()
                                                            Log.d(TAG, "${place.getName()} (dist ${location.distanceTo(loc)}, pinside ${location.probabilityWithin(loc)}")
                                                            return@filter filter.matches(it.getPlace())
                                                        }
                                                        .map { GooglePlacesLocation(it.getPlace(), it.getLikelihood(), attribution) }
                                                        .subscribe(subscriber)
                                                placeLikelihoods.release()
                                            }
                                        })
                                    }
                                })
                            }
                        })
            }.cache()
        }
    }

    override fun search(position: Position): Observable<GeocodedLocation> {
        return search(position, null)
    }

    override fun search(position: Position, categories: String?): Observable<GeocodedLocation> {
        return Observable.create<GeocodedLocation>(object : Observable.OnSubscribe<GeocodedLocation> {
            override fun call(subscriber: Subscriber<in GeocodedLocation>) {
            }
        })
    }

    override fun autoComplete(position: Position, query: String): Observable<AutoCompleteResult> {
        return autoComplete(position, query, null)
    }

    override fun autoComplete(position: Position, query: String, categories: String?): Observable<AutoCompleteResult> {
        return Observable.create {

        }
    }

    override fun getInfo(id: String): Observable<GeocodedLocation> {
        val placeId = id

        return Observable.create<GeocodedLocation>(object : Observable.OnSubscribe<GeocodedLocation> {

            override fun call(subscriber: Subscriber<in GeocodedLocation>) {
                val result = Places.GeoDataApi.getPlaceById(mClient, placeId)
                result.setResultCallback(object : ResultCallback<PlaceBuffer> {
                    override fun onResult(places: PlaceBuffer) {
                        for (place in places) {
                            if (!subscriber.isUnsubscribed())
                                subscriber.onNext(GooglePlacesLocation(place, 1.0.toFloat(), places.getAttributions()))
                        }
                        val status = places.getStatus()
                        if (!status.isSuccess()) {
                            // @TODO propagate this error somehow
                            //subscriber.onError();
                        }
                        places.release()
                        if (!subscriber.isUnsubscribed())
                            subscriber.onCompleted()
                    }
                })
            }
        })
    }

    trait NearbySearchEndpoint {

        GET("/maps/api/place/nearbysearch/json")
        public fun nearbySearch(Query("key") key: String, Query("location") location: String, Query("radius") radius: Float): NearbySearchResponse

        GET("/maps/api/place/nearbysearch/json")
        public fun nearbySearch(Query("key") key: String, Query("location") location: String, Query("radius") radius: Float, Query("types") types: String): NearbySearchResponse

        GET("/maps/api/place/nearbysearch/json")
        public fun nearbySearchObservable(Query("key") key: String, Query("location") location: String, Query("radius") radius: Float): Observable<NearbySearchResponse>

        GET("/maps/api/place/nearbysearch/json")
        public fun nearbySearchObservable(Query("key") key: String, Query("location") location: String, Query("radius") radius: Float, Query("types") types: String): Observable<NearbySearchResponse>


    }

    class NearbySearchResponse {
        Expose
        var htmlAttributions: String? = null
        Expose
        var results: List<GooglePlacesWebLocation>? = null
    }

    companion object {
        private val PLACES_API_URL = "https://maps.googleapis.com/"
        private val MAPS_API_KEY = "AIzaSyCah6xLW8jBtxdgmZrFVLrQx7rWQbDkSxI"
        private val TAG = "GooglePlacesApi"

        public val RESPONSE_LOCATION_FUNC: Func1<NearbySearchResponse, Observable<GooglePlacesWebLocation>> = object : Func1<NearbySearchResponse, Observable<GooglePlacesWebLocation>> {
            override fun call(nearbySearchResponse: NearbySearchResponse): Observable<GooglePlacesWebLocation> {
                return Observable.from<GooglePlacesWebLocation>(nearbySearchResponse.results)
            }
        }
        private val LOCATION_ID_FUNC = object : Func1<GooglePlacesWebLocation, Observable<String>> {
            override fun call(googlePlacesWebLocation: GooglePlacesWebLocation): Observable<String> {
                Log.d("LOCATION_ID_FUNC", googlePlacesWebLocation.getName().toString() + ": " + googlePlacesWebLocation.getId())

                return Observable.just<String>(googlePlacesWebLocation.getId())
            }
        }
    }
}