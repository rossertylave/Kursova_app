package com.example.artviewerapp

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("public/collection/v1/objects/{objectID}")
    suspend fun getObjectDetailsByObjectID(
        @Path("objectID") objectID: Int
    ): ObjectDetailsResponse

    @GET("public/collection/v1/search")
    suspend fun searchArtworks(
        @Query("q") query: String
    ): SearchResponse
}

data class SearchResponse(
    val objectIDs: List<Int>?
)

