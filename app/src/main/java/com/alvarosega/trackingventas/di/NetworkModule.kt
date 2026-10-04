package com.alvarosega.trackingventas.di

import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.AuthInterceptor
import com.alvarosega.trackingventas.data.remote.TimeSyncInterceptor
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.sanitization.data.remote.SanitizationApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        timeSyncInterceptor: TimeSyncInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(timeSyncInterceptor) // <-- Calibra la hora en cada petición HTTP
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://alvarosega.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVisitaApiService(retrofit: Retrofit): VisitaApiService {
        return retrofit.create(VisitaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSanitizationApiService(retrofit: Retrofit): SanitizationApiService {
        return retrofit.create(SanitizationApiService::class.java)
    }
}