package com.saketkhundia.pocketserver.server.security

object RequestValidation {
    private val allowedMethods = setOf("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
    fun isMethodAllowed(m: String) = m.uppercase() in allowedMethods
    fun validateSearchQuery(q: String?): Boolean = (q?.length ?: 0) <= 256
    fun validateSortParam(sort: String?): Boolean = sort == null || sort in setOf("name", "size", "date", "name_desc", "size_desc", "date_desc")
}
