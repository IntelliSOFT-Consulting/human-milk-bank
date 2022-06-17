package com.intellisoft.nndak.data

import android.content.Context
import android.util.Log
import com.intellisoft.nndak.FhirApplication
import com.intellisoft.nndak.api.AuthInterceptor
import com.intellisoft.nndak.api.AuthService
import com.intellisoft.nndak.charts.DHMModel
import com.intellisoft.nndak.charts.Statistics
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class RestManager {
    private lateinit var apiService: AuthService

    private fun getService(context: Context): AuthService {
        val base = FhirApplication.getServerURL(context)
        Timber.e(base)
        // Initialize ApiService if not initialized yet
        if (!::apiService.isInitialized) {
            val retrofit = base?.let {
                Retrofit.Builder()
                    .baseUrl(it)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okhttpClient(context))
                    .build()
            }

            if (retrofit != null) {
                apiService = retrofit.create(AuthService::class.java)
            }
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
                    onResult(response.body())
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }

                }
            }
        )
    }

    fun loadUser(context: Context, onResult: (UserResponse?) -> Unit) {
        getService(context).loadUser().enqueue(
            object : Callback<UserResponse> {
                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    onResult(null)

                }

                override fun onResponse(
                    call: Call<UserResponse>,
                    response: Response<UserResponse>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
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
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
                }
            }
        )
    }

    fun loadStatistics(context: Context, onResult: (Statistics?) -> Unit) {
        getService(context).loadStatistics().enqueue(
            object : Callback<Statistics> {
                override fun onFailure(call: Call<Statistics>, t: Throwable) {
                    Timber.e("onFailure " + t.localizedMessage)
                    onResult(null)

                }

                override fun onResponse(
                    call: Call<Statistics>,
                    response: Response<Statistics>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
                }
            }
        )
    }
    fun loadDonorMilk(context: Context, onResult: (DHMModel?) -> Unit) {
        getService(context).loadDonorMilk().enqueue(
            object : Callback<DHMModel> {
                override fun onFailure(call: Call<DHMModel>, t: Throwable) {
                    Timber.e("onFailure " + t.localizedMessage)
                    onResult(null)

                }

                override fun onResponse(
                    call: Call<DHMModel>,
                    response: Response<DHMModel>
                ) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
                }
            }
        )
    }



}