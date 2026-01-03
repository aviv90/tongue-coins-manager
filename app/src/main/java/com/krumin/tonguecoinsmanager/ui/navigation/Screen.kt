package com.krumin.tonguecoinsmanager.ui.navigation

sealed class Screen(val route: String) {
    object List : Screen("list")
    object Edit : Screen("edit?id={id}") {
        fun createRoute(photoId: String?) = if (photoId != null) "edit?id=$photoId" else "edit"
    }

    companion object {
        const val ARG_ID = "id"
    }
}
