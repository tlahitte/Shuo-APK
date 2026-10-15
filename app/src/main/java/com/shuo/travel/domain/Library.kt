package com.shuo.travel.domain

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val color: String,
    val darkColor: String = color,
    val icon: String,
    val order: Int,
)

@Serializable
data class Phrase(
    val id: String,
    val chinese: String,
    val pinyin: String,
    val french: String,
    val english: String? = null,
    val keywords: List<String> = emptyList(),
    val categoryId: String,
    val favorite: Boolean = false,
    val createdAt: Long = 0,
    val modifiedAt: Long = 0,
    val lastUsedAt: Long? = null,
    val builtIn: Boolean = false,
)

@Serializable
data class Library(val version: Int = 1, val categories: List<Category>, val phrases: List<Phrase>)

val CategoryIcons = listOf("train-front", "utensils", "bed-double", "shopping-bag", "signpost", "cross", "messages-square", "wallet", "landmark", "ellipsis")
