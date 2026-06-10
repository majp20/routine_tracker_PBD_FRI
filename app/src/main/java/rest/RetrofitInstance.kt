package rest

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    //This code is practically the same from the lectures, retrofit example
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(provideLoggingInterceptor()) //logs network requests
            .addInterceptor(provideHeaderInterceptor())//adds a header to the request
            .connectTimeout(15, TimeUnit.SECONDS)//wait up to 15secs for connection
            .readTimeout(15, TimeUnit.SECONDS)//wait up to 15sec for server response
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) //converts JSON into kotlin objects and vice versa
            .build()
    }


    //function that returns logging interceptor -> okHttp component that prints the information about
    //HTTP requests and responses to Logcat
    private fun provideLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply { //apply returns modified object inside its block
            level = HttpLoggingInterceptor.Level.BODY //logs body content as well
        }
    }


    // creates an interceptor that adds header to every http req
    private fun provideHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder() //creates a new req based on the original
                .addHeader("Accept", "application/json") //tells the server we want response in a json
                .build()
            chain.proceed(request)
        }
    }

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }

}