package com.krumin.tonguecoinsmanager.ui.navigation

sealed class Screen(val route: String) {
    object List :
        Screen(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ROUTE_LIST)

    object DailyRiddle :
        Screen(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ROUTE_DAILY_RIDDLE)

    object Edit :

        Screen(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ROUTE_EDIT_FULL) {
        fun createRoute(photoId: String?): String {
            val base =
                com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ROUTE_EDIT_BASE
            val arg = com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ARG_ID
            return if (photoId != null) "$base?$arg=$photoId" else base
        }
    }

    companion object {
        const val ARG_ID =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.ARG_ID
        const val RESULT_KEY =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.KEY_RESULT
        const val RESULT_ADD =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.RESULT_ADD
        const val RESULT_EDIT =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.RESULT_EDIT
        const val RESULT_DELETE =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Navigation.RESULT_DELETE
    }
}
