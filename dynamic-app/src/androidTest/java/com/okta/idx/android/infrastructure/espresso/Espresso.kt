/*
 * Copyright 2021-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.idx.android.infrastructure.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import com.okta.idx.android.dynamic.R
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher

fun authenticatorViewInteraction(authenticatorTitle: String): ViewInteraction {
    waitForElementWithText(authenticatorTitle)
    return onView(
        first(
            allOf(
                withText(authenticatorTitle),
                withParent(withId(R.id.radio_group)),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            )
        )
    )
}

fun selectAuthenticator(authenticatorTitle: String) {
    authenticatorViewInteraction(authenticatorTitle).perform(click())
}

fun waitForElement(resourceId: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().resourceId(resourceId)
    if (!uiDevice.findObject(selector).waitForExists(10_000)) {
        // This will fail and nicely show the view hierarchy.
        onView(withResourceName(resourceId)).check(matches(isDisplayed()))
    }
}

fun waitForElementWithText(text: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().text(text)
    if (!uiDevice.findObject(selector).waitForExists(10_000)) {
        // This will fail and nicely show the view hierarchy.
        onView(withText(text)).check(matches(isDisplayed()))
    }
}

fun waitForElementToBeGone(resourceId: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().resourceId(resourceId)
    if (!uiDevice.findObject(selector).waitForExists(10_000)) {
        // This will fail and nicely show the view hierarchy.
        onView(withResourceName(resourceId)).check(doesNotExist())
    }
}

fun scrollToToBottom() {
    val appViews = UiScrollable(UiSelector().scrollable(true))
    assertThat(appViews.scrollToEnd(20, 5)).isTrue()
}

fun fillInEditText(resourceId: String, text: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().resourceId(resourceId)
    assertThat(uiDevice.findObject(selector).setText(text)).isTrue()
}

fun clickButtonWithText(text: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().text(text)
    assertThat(uiDevice.findObject(selector).click()).isTrue()
}

fun clickButtonWithTextMatching(text: String) {
    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val selector = UiSelector().textMatches(text)
    assertThat(uiDevice.findObject(selector).click()).isTrue()
}

fun <T> first(matcher: Matcher<T>): Matcher<T>? {
    return object : BaseMatcher<T>() {
        var isFirst = true
        override fun matches(item: Any): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return true
            }
            return false
        }

        override fun describeTo(description: Description) {
            description.appendText("should return first matching item")
        }
    }
}
