package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.TelecomProvider
import com.aman.app.data.repository.CustomerNumberRepository
import com.aman.app.data.repository.CustomerNumberRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NumbersListUiState {
    data object Loading : NumbersListUiState
    data class Success(val numbers: List<CustomerNumber>) : NumbersListUiState
    data class Error(val message: String) : NumbersListUiState
}

sealed interface AddNumberUiState {
    data object Idle : AddNumberUiState
    data object Submitting : AddNumberUiState
    data class Success(val number: CustomerNumber) : AddNumberUiState
    data class Error(val message: String) : AddNumberUiState
}

class ClientNumbersViewModel(
    private val numberRepo: CustomerNumberRepository = CustomerNumberRepositoryImpl()
) : ViewModel() {

    private val _listState = MutableStateFlow<NumbersListUiState>(NumbersListUiState.Loading)
    val listState: StateFlow<NumbersListUiState> = _listState.asStateFlow()

    private val _addState = MutableStateFlow<AddNumberUiState>(AddNumberUiState.Idle)
    val addState: StateFlow<AddNumberUiState> = _addState.asStateFlow()

    private val _detectedProvider = MutableStateFlow<TelecomProvider?>(null)
    val detectedProvider: StateFlow<TelecomProvider?> = _detectedProvider.asStateFlow()

    fun loadNumbers(customerId: String?) {
        if (customerId.isNullOrBlank()) {
            _listState.value = NumbersListUiState.Success(emptyList())
            return
        }
        _listState.value = NumbersListUiState.Loading
        viewModelScope.launch {
            when (val res = numberRepo.getNumbersByCustomer(customerId)) {
                is AmanResult.Success -> {
                    _listState.value = NumbersListUiState.Success(res.data)
                }
                is AmanResult.Error -> {
                    _listState.value = NumbersListUiState.Error(res.error.message)
                }
            }
        }
    }

    /**
     * كشف مزود الخدمة تلقائياً عبر قاعدة البيانات بناءً على البادئة المدخلة.
     * مزود الخدمة يُحدد حصراً عبر قاعدة بيانات V7 ولا يتم اختياره يدوياً من العميل.
     */
    fun onPhoneNumberChanged(input: String) {
        val cleanNumber = input.filter { it.isDigit() }
        if (cleanNumber.length >= 2) {
            viewModelScope.launch {
                when (val res = numberRepo.detectProviderFromPrefix(cleanNumber)) {
                    is AmanResult.Success -> {
                        _detectedProvider.value = res.data
                    }
                    is AmanResult.Error -> {
                        _detectedProvider.value = null
                    }
                }
            }
        } else {
            _detectedProvider.value = null
        }
    }

    fun submitNewNumber(phoneNumber: String) {
        val cleanNumber = phoneNumber.filter { it.isDigit() }
        if (cleanNumber.isBlank()) {
            _addState.value = AddNumberUiState.Error("يرجى إدخال رقم الهاتف")
            return
        }

        val provider = _detectedProvider.value
        if (provider == null) {
            _addState.value = AddNumberUiState.Error("لم يتم التعرف على شركة الاتصالات التابع لها هذا الرقم")
            return
        }

        if (cleanNumber.length != provider.numberLength) {
            _addState.value = AddNumberUiState.Error("طول الرقم غير صحيح. يجب أن يتكون من ${provider.numberLength} أرقام لمزود ${provider.name}")
            return
        }

        _addState.value = AddNumberUiState.Submitting
        viewModelScope.launch {
            when (val res = numberRepo.registerCustomerNumber(cleanNumber)) {
                is AmanResult.Success -> {
                    _addState.value = AddNumberUiState.Success(res.data)
                }
                is AmanResult.Error -> {
                    _addState.value = AddNumberUiState.Error(res.error.message)
                }
            }
        }
    }

    fun resetAddState() {
        _addState.value = AddNumberUiState.Idle
        _detectedProvider.value = null
    }
}
