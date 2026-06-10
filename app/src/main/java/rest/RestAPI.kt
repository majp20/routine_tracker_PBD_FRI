package rest

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query


interface RestAPI  {

    //function that returns openWeather json response object
    @GET("weather")
    suspend fun getWeather(
        @Query("q") location: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" //we set this field to metric to get celsius
    ): JsonObject
}


