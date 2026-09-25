package com.aman.app.ui.screens.client.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.SystemSettings
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.AmanTopAppBar
import com.aman.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    adminRepo: AdminRepository = remember { AdminRepositoryImpl() }
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf<SystemSettings?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            when (val res = adminRepo.getSystemSettings()) {
                is AmanResult.Success -> settings = res.data
                is AmanResult.Error -> {}
            }
        }
    }

    val whatsappNumber = settings?.contactInfo?.takeIf { it.isNotBlank() } ?: "+967770000000"
    val supportEmail = "support@aman-ye.com"
    val supportPhone = "+9671234567"

    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "المساعدة والدعم الفني",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundMuted)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Support channels card
                AmanCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "قنوات التواصل المباشر مع الدعم",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على أي وسيلة لبدء المحادثة أو التواصل المباشر",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // WhatsApp Clickable Row
                        ClickableSupportRow(
                            icon = Icons.Default.Chat,
                            iconTint = Color(0xFF25D366),
                            title = "واتساب خدمة العملاء (رد فوري)",
                            value = whatsappNumber,
                            onClick = {
                                val cleanNum = whatsappNumber.replace(Regex("[^0-9]"), "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNum"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp))

                        // Email Clickable Row
                        ClickableSupportRow(
                            icon = Icons.Default.Email,
                            iconTint = Primary,
                            title = "البريد الإلكتروني للدعم والمتابعة",
                            value = supportEmail,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp))

                        // Phone Clickable Row
                        ClickableSupportRow(
                            icon = Icons.Default.Phone,
                            iconTint = Primary,
                            title = "هاتف الاستفسارات المباشر",
                            value = supportPhone,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }

                // FAQs Header
                Text(
                    text = "الأسئلة الشائعة حول خدمة أمان",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                FaqCard(
                    question = "ما هي خدمة أمان لحماية الأرقام؟",
                    answer = "أمان هي المنظومة الأولى المتخصصة في الجمهورية اليمنية لحماية خطوط الهواتف المحمولة من الإلغاء أو إعادة السحب والبيع من قبل شركات الاتصالات نتيجة عدم الاستخدام، وخاصة للمغتربين ورجال الأعمال أو من يمتلكون أرقاماً مهمة مرتبطة بحسابات بنكية واجتماعية."
                )

                FaqCard(
                    question = "لماذا تقوم شركات الاتصالات بسحب الأرقام؟",
                    answer = "تنص لوائح شركات الاتصالات العاملة في اليمن (يمن موبايل، يو YOU، سبأفون، واي) على إلغاء الخط وإعادة طرحه للبيع إذا مضت فترة محددة دون إجراء شحن أو اتصال صادر أو دفع رسوم دورية."
                )

                FaqCard(
                    question = "كيف يحافظ تطبيق أمان على رقمي؟",
                    answer = "يقوم فريق ونظام أمان بجدولة عمليات دورية وشحن رصيد وتنشيط على خطك المسجل وفق جدول زمني استباقي يسبق مهلة الشركة، مما يضمن بقاء الخط نشطاً بصورة مستمرة في سجلات المزود دون انقطاع."
                )

                FaqCard(
                    question = "كيف أعرف متى تنتهي حماية رقمي؟",
                    answer = "يوفر التطبيق عداداً تنازلياً دقيقاً بالأيام المتبقية في شاشة تفاصيل الحماية، كما يرسل لك إشعاراً عندما يدخل الرقم في نطاق 'تحتاج تجديد' لتتمكن من التمديد بسهولة."
                )

                FaqCard(
                    question = "ماذا أفعل بعد إرسال طلب الحماية؟",
                    answer = "بمجرد تقديم طلب الحماية وإدخال بيانات سند التحويل، يقوم فريق المراجعة بتأكيد السند وتفعيل الحماية وإشعارك فوراً عبر التطبيق."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ClickableSupportRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextDisabled,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FaqCard(question: String, answer: String) {
    AmanCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            )
        }
    }
}
