package com.example.scanworld

data class ProductResponse(
    val code: String,
    val product: ProductData?
)

data class ProductData(
    val id: String,
    val brands: String?,
    val brands_tags: List<String>?,
    val product_name: String?,
    val generic_name: String?,
    val generic_name_en: String?,
    val generic_name_fr: String?,
    val link: String?,
    val countries_tags: List<String>?
)