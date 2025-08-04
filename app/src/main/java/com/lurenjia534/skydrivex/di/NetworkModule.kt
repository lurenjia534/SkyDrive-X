package com.lurenjia534.skydrivex.di

import com.lurenjia534.skydrivex.data.remote.GraphApiService
import com.lurenjia534.skydrivex.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val authHeader = original.header("Authorization")
                val request = if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $authHeader")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .build()
    @Provides
    @Singleton
    fun provideGraphApiService(okHttpClient: OkHttpClient): GraphApiService =
        Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GraphApiService::class.java)

    @Provides
    @Singleton
    fun provideUserRepository(graphApiService: GraphApiService): UserRepository =
        UserRepository(graphApiService)
}