package com.example.artviewerapp

data class ObjectDetailsResponse(
    val objectID: Int,
    val title: String?,
    val primaryImage: String?,
    val accessionYear: String?,
    val artistDisplayName: String?,
    val artistDisplayBio: String?
)

