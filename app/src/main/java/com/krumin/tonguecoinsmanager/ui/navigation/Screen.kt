package com.krumin.tonguecoinsmanager.ui.navigation

sealed class Screen(val route: String) {
    object List : Screen("list")
    object Edit : Screen("edit?id={id}") {
        fun createRoute(photoId: String?) = if (photoId != null) "edit?id=$photoId" else "edit"
    }

    companion object {
        const val ARG_ID = "id"
        const val RESULT_KEY = "action_result"
        const val RESULT_ADD = "added"
        const val RESULT_EDIT = "edited"
        const val RESULT_DELETE = "deleted"
    }
}
