package com.weyya.app.ui

import org.junit.Rule

/**
 * Base for ViewModel tests: installs the [MainDispatcherRule] so `viewModelScope.launch` blocks
 * run under the test scheduler. Subclasses add their own mocks/`@Before`; this centralizes the
 * dispatcher wiring so it changes in one place.
 */
abstract class ViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
