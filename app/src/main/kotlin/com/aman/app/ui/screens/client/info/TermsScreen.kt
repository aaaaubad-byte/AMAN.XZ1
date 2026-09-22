package com.aman.app.ui.screens.client.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.app.ui.components.AmanCard
import com.aman.app.ui.components.AmanTopAppBar
import com.aman.app.ui.theme.*

@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "الشروط والأحكام",
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
                AmanCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "اتفاقية استخدام خدمة أمان لحماية الأرقام",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "آخر تحديث: 2026",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "باستخدامك لتطبيق أمان، فإنك تقر وتوافق على البنود والشروط الموضحة أدناه، والتي تنظم العلاقة بينك وبين إدارة الخدمة:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                    }
                }

                TermsSectionCard(
                    title = "1. طبيعة الخدمة",
                    content = "خدمة 'أمان' هي وسيط إداري وتقني يقوم بجدولة وتنفيذ عمليات دورية للحفاظ على بقاء خط الهاتف نشطاً لدى شركة الاتصالات المحلية المعنية، وفقاً لمدد باقة الحماية المختارة من قِبل العميل."
                )

                TermsSectionCard(
                    title = "2. مسؤولية العميل عن صحة البيانات",
                    content = "يتحمل العميل كامل المسؤولية القانونية والفنية عن صحة رقم الهاتف المدخل في حسابه. ولا يتحمل التطبيق أي مسؤولية في حال إدخال رقم خاطئ أو رقم لا تعود ملكيته للعميل."
                )

                TermsSectionCard(
                    title = "3. آلية تفعيل الحماية والسداد",
                    content = "لا تُعد الحماية مفعلة بمجرد إنشاء الطلب، بل تتطلب مراجعة السند المالي واعتماده من قِبل إدارة الخدمة. تبدأ فترة الحماية الفعلية وتُحسب الأيام من تاريخ الاعتماد الرسمي للطلب."
                )

                TermsSectionCard(
                    title = "4. التجديد ومسؤولية المتابعة",
                    content = "يوفر التطبيق تنبيهات وإشعارات دورية لقرب انتهاء صلاحية الحماية. ويقع على عاتق العميل تقديم طلب تجديد وسداد الرسوم قبل انتهاء المدة بـ 5 أيام على الأقل لضمان عدم حدوث انقطاع في التنشيط."
                )

                TermsSectionCard(
                    title = "5. سياسة الاسترجاع والإلغاء",
                    content = "نظراً لأن تفعيل الحماية يترتب عليه تخصيص موارد وعمليات شحن رصيد وتنشيط لدى شركات الاتصالات، فإن المبالغ المدفوعة لباقات الحماية غير قابلة للاسترداد بعد اعتماد الطلب وتفعيله."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TermsSectionCard(title: String, content: String) {
    AmanCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            )
        }
    }
}
