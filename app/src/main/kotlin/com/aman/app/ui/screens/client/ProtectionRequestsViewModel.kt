package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.*
import com.aman.app.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProtectionRequestsListUiState {
    data object Loading : ProtectionRequestsListUiState
    data class Success(val requests: List<ProtectionRequest>) : ProtectionRequestsListUiState
    data class Error(val message: String) : ProtectionRequestsListUiState
}

sealed interface CreateRequestUiState {
    data object Loading : CreateRequestUiState
    data class Ready(
        val availableNumbers: List<CustomerNumber>,
        val selectedNumber: CustomerNumber?,
        val availablePlans: List<ProtectionPlan>,
        val selectedPlan: ProtectionPlan?,
        val availablePaymentMethods: List<PaymentMethod>,
        val selectedPaymentMethod: PaymentMethod?
    ) : CreateRequestUiState
    data object Submitting : CreateRequestUiState
    data class SubmittedSuccess(val request: ProtectionRequest) : CreateRequestUiState
    data class Error(val message: String) : CreateRequestUiState
}

enum class RequestFilterTab(val titleAr: String) {
    ALL("الكل"),
    PENDING("قيد المراجعة"),
    APPROVED("معتمدة"),
    REJECTED("مرفوضة")
}

class ProtectionRequestsViewModel(
    private val requestRepo: ProtectionRequestRepository = ProtectionRequestRepositoryImpl(),
    private val numberRepo: CustomerNumberRepository = CustomerNumberRepositoryImpl(),
    private val planRepo: ProtectionPlanRepository = ProtectionPlanRepositoryImpl(),
    private val paymentRepo: PaymentMethodRepository = PaymentMethodRepositoryImpl()
) : ViewModel() {

    private val _listState = MutableStateFlow<ProtectionRequestsListUiState>(ProtectionRequestsListUiState.Loading)
    val listState: StateFlow<ProtectionRequestsListUiState> = _listState.asStateFlow()

    private val _selectedFilterTab = MutableStateFlow(RequestFilterTab.ALL)
    val selectedFilterTab: StateFlow<RequestFilterTab> = _selectedFilterTab.asStateFlow()

    private val _createState = MutableStateFlow<CreateRequestUiState>(CreateRequestUiState.Loading)
    val createState: StateFlow<CreateRequestUiState> = _createState.asStateFlow()

    private var currentCustomerId: String? = null
    private var existingPendingRequests: List<ProtectionRequest> = emptyList()

    fun setFilterTab(tab: RequestFilterTab) {
        _selectedFilterTab.value = tab
    }

    fun loadRequests(customerId: String?) {
        if (customerId.isNullOrBlank()) {
            _listState.value = ProtectionRequestsListUiState.Success(emptyList())
            return
        }

        _listState.value = ProtectionRequestsListUiState.Loading
        viewModelScope.launch {
            when (val res = requestRepo.getCustomerRequests(customerId)) {
                is AmanResult.Success -> {
                    _listState.value = ProtectionRequestsListUiState.Success(res.data)
                }
                is AmanResult.Error -> {
                    _listState.value = ProtectionRequestsListUiState.Error(res.error.message)
                }
            }
        }
    }

    fun prepareCreateForm(customerId: String?, preselectedNumberId: String?) {
        if (customerId.isNullOrBlank()) {
            _createState.value = CreateRequestUiState.Error("المستخدم غير مسجل الدخول")
            return
        }

        currentCustomerId = customerId
        _createState.value = CreateRequestUiState.Loading
        viewModelScope.launch {
            val numbersRes = numberRepo.getNumbersByCustomer(customerId)
            val paymentMethodsRes = paymentRepo.getActivePaymentMethods()
            val requestsRes = requestRepo.getCustomerRequests(customerId)

            if (requestsRes is AmanResult.Success) {
                existingPendingRequests = requestsRes.data.filter { it.status == RequestStatus.PENDING }
            }

            if (numbersRes is AmanResult.Success && paymentMethodsRes is AmanResult.Success) {
                val numbers = numbersRes.data
                val paymentMethods = paymentMethodsRes.data
                val preselected = numbers.find { it.id == preselectedNumberId }
                    ?: numbers.find { it.protectionStatus == NumberProtectionStatus.UNPROTECTED }
                    ?: numbers.firstOrNull()

                val plans = if (preselected != null) {
                    when (val plansRes = planRepo.getPlansByProvider(preselected.providerId)) {
                        is AmanResult.Success -> plansRes.data
                        is AmanResult.Error -> emptyList()
                    }
                } else emptyList()

                _createState.value = CreateRequestUiState.Ready(
                    availableNumbers = numbers,
                    selectedNumber = preselected,
                    availablePlans = plans,
                    selectedPlan = plans.firstOrNull(),
                    availablePaymentMethods = paymentMethods,
                    selectedPaymentMethod = paymentMethods.firstOrNull()
                )
            } else {
                _createState.value = CreateRequestUiState.Error("تعذر تحميل بيانات تقديم الطلب")
            }
        }
    }

    fun selectNumber(number: CustomerNumber) {
        val current = _createState.value
        if (current is CreateRequestUiState.Ready) {
            viewModelScope.launch {
                val plans = when (val res = planRepo.getPlansByProvider(number.providerId)) {
                    is AmanResult.Success -> res.data
                    is AmanResult.Error -> emptyList()
                }
                _createState.value = current.copy(
                    selectedNumber = number,
                    availablePlans = plans,
                    selectedPlan = plans.firstOrNull()
                )
            }
        }
    }

    fun selectPlan(plan: ProtectionPlan) {
        val current = _createState.value
        if (current is CreateRequestUiState.Ready) {
            _createState.value = current.copy(selectedPlan = plan)
        }
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        val current = _createState.value
        if (current is CreateRequestUiState.Ready) {
            _createState.value = current.copy(selectedPaymentMethod = paymentMethod)
        }
    }

    fun submitRequest(transferData: String) {
        val current = _createState.value
        if (current !is CreateRequestUiState.Ready) return

        val custId = currentCustomerId
        if (custId.isNullOrBlank()) {
            _createState.value = CreateRequestUiState.Error("المستخدم غير مسجل الدخول")
            return
        }

        val number = current.selectedNumber
        if (number == null) {
            _createState.value = CreateRequestUiState.Error("يرجى اختيار رقم الهاتف المراد حمايته")
            return
        }

        // Validate customer is operating on their own number
        if (number.customerId.isNotBlank() && number.customerId != custId) {
            _createState.value = CreateRequestUiState.Error("الرقم المحدد لا يتبع حساب هذا العميل")
            return
        }

        // Validate selected provider exists on number
        if (number.providerId.isBlank()) {
            _createState.value = CreateRequestUiState.Error("لم يتم تحديد شركة الاتصالات التابع لها هذا الرقم")
            return
        }

        // Validate duplicate pending protection request
        val isPending = number.protectionStatus == NumberProtectionStatus.PENDING ||
                existingPendingRequests.any { it.customerNumberId == number.id && it.status == RequestStatus.PENDING }
        if (isPending) {
            _createState.value = CreateRequestUiState.Error("يوجد بالفعل طلب حماية قيد المراجعة لهذا الرقم. لا يمكن تقديم طلب إضافي حتى تتم معالجة الطلب السابق.")
            return
        }

        val plan = current.selectedPlan
        if (plan == null) {
            _createState.value = CreateRequestUiState.Error("يرجى اختيار باقة الحماية")
            return
        }

        // Validate selected plan belongs to the selected provider
        if (plan.providerId != number.providerId) {
            _createState.value = CreateRequestUiState.Error("الباقة المختارة لا تتوافق مع شركة الاتصالات الخاصة بالرقم")
            return
        }

        val paymentMethod = current.selectedPaymentMethod
        if (paymentMethod == null) {
            _createState.value = CreateRequestUiState.Error("يرجى اختيار وسيلة الدفع")
            return
        }

        // Validate transfer data is present and not empty
        val cleanTransfer = transferData.trim()
        if (cleanTransfer.isBlank()) {
            _createState.value = CreateRequestUiState.Error("يرجى إدخال بيانات أو رقم الحوالة / السند المالي")
            return
        }

        _createState.value = CreateRequestUiState.Submitting
        viewModelScope.launch {
            when (val res = requestRepo.submitProtectionRequest(
                customerNumberId = number.id,
                planId = plan.id,
                paymentMethodId = paymentMethod.id,
                transferData = cleanTransfer
            )) {
                is AmanResult.Success -> {
                    _createState.value = CreateRequestUiState.SubmittedSuccess(res.data)
                }
                is AmanResult.Error -> {
                    _createState.value = CreateRequestUiState.Error(res.error.message)
                }
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateRequestUiState.Loading
    }
}
