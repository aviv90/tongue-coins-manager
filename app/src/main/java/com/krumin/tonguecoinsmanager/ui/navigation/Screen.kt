package com.krumin.tonguecoinsmanager.ui.navigation

import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig

sealed class Screen(val route: String) {
    object List :
        Screen(AppConfig.Navigation.ROUTE_LIST)

    object DailyRiddle :
        Screen(AppConfig.Navigation.ROUTE_DAILY_RIDDLE)
    
    object DailyBroadcast :
        Screen(AppConfig.Navigation.ROUTE_DAILY_BROADCAST)

    object Edit :

        Screen(AppConfig.Navigation.ROUTE_EDIT_FULL) {
        fun createRoute(photoId: String?): String {
            val base =
                AppConfig.Navigation.ROUTE_EDIT_BASE
            val arg = AppConfig.Navigation.ARG_ID
            return if (photoId != null) "$base?$arg=$photoId" else base
        }
    }

    companion object {
        const val ARG_ID =
            AppConfig.Navigation.ARG_ID
        const val RESULT_KEY =
            AppConfig.Navigation.KEY_RESULT
        const val RESULT_ADD =
            AppConfig.Navigation.RESULT_ADD
        const val RESULT_EDIT =
            AppConfig.Navigation.RESULT_EDIT
        const val RESULT_DELETE =
            AppConfig.Navigation.RESULT_DELETE
    }
}
