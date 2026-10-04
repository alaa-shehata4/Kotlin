package com.example.carebrief.core.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PEOPLE = "people"
    const val NOTES = "notes"
    const val PLAN = "plan"
    const val MORE = "more"

    const val PROFILE = "profile/{recipientId}"
    const val NOTE_NEW = "note/new?recipientId={recipientId}"
    const val ANALYSIS = "analysis/{recipientId}"
    const val SUMMARY = "summary/{recipientId}"
    const val PLAN_DETAIL = "plan/{recipientId}"
    const val PLAN_EDIT = "plan/edit/{recipientId}"

    fun profile(recipientId: String) = "profile/$recipientId"
    fun noteNew(recipientId: String = "sarah") = "note/new?recipientId=$recipientId"
    fun analysis(recipientId: String) = "analysis/$recipientId"
    fun summary(recipientId: String) = "summary/$recipientId"
    fun planDetail(recipientId: String) = "plan/$recipientId"
    fun planEdit(recipientId: String) = "plan/edit/$recipientId"
}
