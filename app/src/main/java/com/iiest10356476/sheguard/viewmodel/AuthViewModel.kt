package com.iiest10356476.sheguard.viewmodel

import androidx.lifecycle.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.iiest10356476.sheguard.data.models.User
import com.iiest10356476.sheguard.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Success messages
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Authentication state
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    sealed class AuthState {
        object Loading : AuthState()
        object SignedOut : AuthState()
        data class SignedIn(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
        data class EmailVerificationPending(val email: String) : AuthState()
    }

    init {
        // Don't check auth state automatically - let activities handle their own flow
        _authState.value = AuthState.SignedOut
    }

    // Sign up function
    fun signUp(email: String, password: String, fullName: String, dateOfBirth: String) {
        if (!validateSignUpInput(email, password, fullName, dateOfBirth)) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.signUp(email, password, fullName, dateOfBirth)
                .onSuccess { user ->
                    _isLoading.value = false
                    _currentUser.value = user
                    _authState.value = AuthState.EmailVerificationPending(user.email)
                    _successMessage.value = "Account created! Please check your email and then sign in."
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    val errorMsg = getErrorMessage(exception)
                    _errorMessage.value = errorMsg
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }

    // Sign in function
    fun signIn(email: String, password: String) {
        if (!validateSignInInput(email, password)) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.signIn(email, password)
                .onSuccess { user ->
                    _isLoading.value = false
                    _currentUser.value = user
                    _authState.value = AuthState.SignedIn(user)
                    _successMessage.value = "Welcome back, ${user.fullName}!"
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    val errorMsg = getErrorMessage(exception)
                    _errorMessage.value = errorMsg
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }

    // Reset password function
    fun resetPassword(email: String) {
        if (!validateEmail(email)) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.resetPassword(email)
                .onSuccess {
                    _isLoading.value = false
                    _successMessage.value = "Password reset email sent. Please check your inbox."
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    val errorMsg = getErrorMessage(exception)
                    _errorMessage.value = errorMsg
                }
        }
    }

    // Sign out function
    fun signOut() {
        repository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.SignedOut
        _successMessage.value = "Signed out successfully"
    }

    // Clear error message
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Clear success message
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // Validation functions
    fun validateSignUpInput(email: String, password: String, fullName: String, dateOfBirth: String): Boolean {
        return when {
            fullName.isBlank() -> {
                _errorMessage.value = "Please enter your full name"
                false
            }
            !validateEmail(email) -> {
                _errorMessage.value = "Please enter a valid email address"
                false
            }
            dateOfBirth.isBlank() -> {
                _errorMessage.value = "Please select your date of birth"
                false
            }
            !validatePassword(password) -> {
                _errorMessage.value = "Password must be at least 8 characters with uppercase, lowercase, number and special character"
                false
            }
            else -> true
        }
    }

    fun validateSignInInput(email: String, password: String): Boolean {
        return when {
            !validateEmail(email) -> {
                _errorMessage.value = "Please enter a valid email address"
                false
            }
            password.isBlank() -> {
                _errorMessage.value = "Please enter your password"
                false
            }
            else -> true
        }
    }

    fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        return password.matches(passwordPattern.toRegex())
    }


    // Convert Firebase exceptions to user-friendly messages
    private fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format or incorrect credentials."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthInvalidUserException -> "No account found with this email address."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            else -> exception.message ?: "An unexpected error occurred. Please try again."
        }
    }
}