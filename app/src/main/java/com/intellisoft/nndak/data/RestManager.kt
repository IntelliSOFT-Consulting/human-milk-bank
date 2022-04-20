package com.intellisoft.nndak.data

import android.content.Context
import android.util.Log
import com.intellisoft.nndak.api.AuthInterceptor
import com.intellisoft.nndak.api.AuthService
import com.intellisoft.nndak.utils.Constants.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class RestManager {
    private lateinit var apiService: AuthService

    fun getService(context: Context): AuthService {

        // Initialize ApiService if not initialized yet
        if (!::apiService.isInitialized) {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okhttpClient(context))
                .build()

            apiService = retrofit.create(AuthService::class.java)
        }

        return apiService
    }

    /**
     * Initialize OkhttpClient with our interceptor
     */
    private fun okhttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
    }

    fun loginUser(context: Context, data: LoginData, onResult: (AuthResponse?) -> Unit) {
        getService(context).loginUser(data).enqueue(
            object : Callback<AuthResponse> {
                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Timber.e("onFailure " + t.localizedMessage)
                    onResult(null)
                }

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    Timber.e("onResponse " + response.body())
                    onResult(response.body())

                }
            }
        )
    }

    fun loadUser(context: Context, onResult: (UserResponse?) -> Unit) {
        getService(context).loadUser().enqueue(
            object : Callback<UserResponse> {
                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Timber.e("onFailure " + t.localizedMessage)
                    onResult(null)

                }

                override fun onResponse(
                    call: Call<UserResponse>,
                    response: Response<UserResponse>
                ) {
                    Timber.e("onResponse " + response.headers())
                    onResult(response.body())
                }
            }
        )
    }

    fun resetPassword(context: Context, data: LoginData, onResult: (AuthResponse?) -> Unit) {
        getService(context).resetPassword(data).enqueue(
            object : Callback<AuthResponse> {
                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Timber.e("onFailure " + t.localizedMessage)
                    onResult(null)

                }

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    Timber.e("onResponse " + response.body())
                    onResult(response.body())
                }
            }
        )
    }


}