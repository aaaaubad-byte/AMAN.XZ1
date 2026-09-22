package com.aman.app.ui.screens.client.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.AmanTopAppBar
import com.aman.app.ui.theme.*

@Composable
fun HelpScreen(onBack: () -> Unit) {
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
                        Spacer(modifier = Modifier.height(12.dp))

                        SupportRow(
                            icon = Icons.Default.Chat,
                            title = "واتساب خدمة العملاء",
                            value = "+967 770 000 000"
                        )
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

                        SupportRow(
                            icon = Icons.Default.Email,
                            title = "البريد الإلكتروني للدعم",
                            value = "support@aman-ye.com"
                        )
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

                        SupportRow(
                            icon = Icons.Default.Phone,
                            title = "هاتف الاستفسارات",
                            value = "01-234567"
                        )
                    }
                }

                // FAQs Header
                Text(
                    text = "الأسئلة الشائعة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                FaqCard(
                    question = "ما هي خدمة أمان لحماية الأرقام؟",
                    answer = "أمان هي أول خدمة متخصصة في اليمن لحماية خطوط الهواتف المحمولة من الإلغاء أو إعادة البيع من قبل شركات الاتصالات نتيجة عدم الاستخدام، خاصة للمغتربين أو من يمتلكون أرقاماً ثانوية مهمة."
                )

                FaqCard(
                    question = "لماذا تقوم شركات الاتصالات بسحب الأرقام؟",
                    answer = "تنص لوائح شركات الاتصالات (يمن موبايل، يو، سبأفون، واي) على إلغاء الخط وسحبه في حال مرور فترة زمنية (تتراوح بين 60 إلى 90 يوماً) دون إجراء أي عملية مالية أو شحن رصيد أو نشاط صادر."
                )

                FaqCard(
                    question = "كيف يحافظ تطبيق أمان على رقمي؟",
                    answer = "يقوم فريق ونظام أمان بجدولة عمليات تنشيط دورية ودفعات منتظمة على خطك المسجل وفق جدول زمني دقيق يسبق مهلة الشركة، مما يبقي الخط نشطاً بصورة مستمرة في سجلات المزود."
                )

                FaqCard(
                    question = "كيف أعرف متى تنتهي حماية رقمي؟",
                    answer = "يعرض لك التطبيق عداداً تنازلياً دقيقاً بالأيام المتبقية لكل رقم محمي، كما يرسل لك إشعاراً عندما يدخل الرقم في نطاق 'يحتاج تجديد' قبل 10 أيام من الانتهاء."
                )

                FaqCard(
                    question = "ماذا أفعل بعد إرسال طلب الحماية؟",
                    answer = "بمجرد تقديم طلب الحماية وإدخال بيانات السند المالي، يقوم فريق المراجعة بتأكيد السند خلال ساعات معدودة، وتفعيل الحماية وإشعارك فوراً."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SupportRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
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
                    lineHeight = 20.sp
                )
            )
        }
    }
}
