package com.krumin.tonguecoinsmanager.ui.navigation

sealed class Screen(val route: String) {
    object List : Screen("list")

    object DailyRiddle : Screen("daily_riddle")

    object DailyBroadcast : Screen("daily_broadcast")

    object Edit : Screen("edit?id={id}") {
        fun createRoute(photoId: String?): String {
            return if (photoId != null) "edit?id=$photoId" else "edit"
        }
    }

    object SendFcm : Screen("send_fcm")

    companion object {
        const val ARG_ID = "id"
        const val RESULT_KEY = "action_result"
        const val RESULT_ADD = "added"
        const val RESULT_EDIT = "edited"
        const val RESULT_DELETE = "deleted"
        const val RESULT_BROADCAST_SAVED = "broadcast_saved"
    }
}
