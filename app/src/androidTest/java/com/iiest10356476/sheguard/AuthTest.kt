package com.iiest10356476.sheguard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iiest10356476.sheguard.viewmodel.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class AuthViewModelIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel()
    }

    @Test
    fun signInSuccess_integration() = runBlocking {
        val email = "joshuaponquett@gmail.com"
        val password = "TestAccount@123"

        val latch = CountDownLatch(1)

        viewModel.authState.observeForever { state ->
            if (state is AuthViewModel.AuthState.SignedIn || state is AuthViewModel.AuthState.Error) {
                latch.countDown()
            }
        }

        viewModel.signIn(email, password)

        // Wait up to 30 seconds for Firebase response if no response fail the test
        val completed = latch.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("Firebase did not respond in time", completed)

        val authState = viewModel.authState.value
        val user = viewModel.currentUser.value
        val message = viewModel.successMessage.value

        assertTrue(authState is AuthViewModel.AuthState.SignedIn, "User should be signed in")
        assertEquals(email, user?.email)
        assertEquals("Welcome back, ${user?.fullName}!", message)
    }

    @Test
    fun signInFail_integration() = runBlocking {
        val email = "joshuaponquett@gmail.com"
        val password = "wrongpassword"

        val latch = CountDownLatch(1)

        viewModel.authState.observeForever { state ->
            if (state is AuthViewModel.AuthState.SignedIn || state is AuthViewModel.AuthState.Error) {
                latch.countDown()
            }
        }

        viewModel.signIn(email, password)

        // Wait up to 30 seconds for Firebase response
        val completed = latch.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("Firebase did not respond in time", completed)

        val authState = viewModel.authState.value
        val errorMsg = viewModel.errorMessage.value
        //making sure the auth state changed to error
        assertTrue(authState is AuthViewModel.AuthState.Error, "Auth state should be error")
        println("Received error message: $errorMsg")
    }

    @Test
    fun signUpSuccess_integration() = runBlocking {
        // Use a unique email each run to avoid Firebase "email already in use"
        val uniqueEmail = "Joshuaponquett@gmail.com"
        val password = "TestAccount@123"
        val fullName = "Test User"
        val dateOfBirth = "2000-01-01"

        val latch = CountDownLatch(1)

        viewModel.authState.observeForever { state ->
            if (state is AuthViewModel.AuthState.EmailVerificationPending || state is AuthViewModel.AuthState.Error) {
                latch.countDown()
            }
        }

        viewModel.signUp(uniqueEmail, password, fullName, dateOfBirth)

        // Wait up to 30 seconds for Firebase response
        val completed = latch.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("Firebase did not respond in time", completed)

        val authState = viewModel.authState.value
        val user = viewModel.currentUser.value
        val message = viewModel.successMessage.value

        // The AuthState after signUp is EmailVerificationPending
        assertTrue(authState is AuthViewModel.AuthState.EmailVerificationPending, "User should be pending email verification")
        assertEquals(uniqueEmail, user?.email)
        assertEquals(
            "Account created! Please check your email and then sign in.",
            message
        )
    }

    @Test
    fun signUpFail_integration_existingEmail() = runBlocking {
        //using an existing developers email. firebase will return that this user  already exists error.
        val email = "davedior03@gmail.com"
        val password = "ValidPass@123"
        val fullName = "Test User"
        val dateOfBirth = "2000-01-01"

        val latch = CountDownLatch(1)
        viewModel.authState.observeForever { state ->
            if (state is AuthViewModel.AuthState.EmailVerificationPending || state is AuthViewModel.AuthState.Error) {
                latch.countDown()
            }
        }

        viewModel.signUp(email, password, fullName, dateOfBirth)

        val completed = latch.await(30, TimeUnit.SECONDS)
        Assert.assertTrue("Firebase did not respond in time", completed)

        val authState = viewModel.authState.value
        val errorMsg = viewModel.errorMessage.value

        assertTrue(authState is AuthViewModel.AuthState.Error, "Auth state should be error")
        println("Received error message: $errorMsg")
    }

}
