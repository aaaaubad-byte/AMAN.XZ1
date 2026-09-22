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
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AmanTopAppBar(
                title = "سياسة الخصوصية",
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
                            text = "التزامنا التام بخصوصية وأمان بياناتك",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "نحن في تطبيق 'أمان' نضع سرية معلومات العملاء وأرقامهم المسجلة على رأس أولوياتنا الأمنية والمهنية.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                    }
                }

                PrivacyItemCard(
                    title = "1. البيانات التي نجمعها",
                    content = "نقتصر على جمع البيانات الضرورية لتقديم الخدمة: اسم العميل، البريد الإلكتروني، أرقام الهواتف المراد حمايتها، وبيانات سندات التحويل المالي."
                )

                PrivacyItemCard(
                    title = "2. الغرض من معالجة البيانات",
                    content = "تُستخدم البيانات فقط لغايات توثيق الحساب، والتواصل بشأن مواعيد التجديد، وتنفيذ عمليات التنشيط المطلوبة لدى مزودي الاتصالات."
                )

                PrivacyItemCard(
                    title = "3. عدم مشاركة البيانات مع أطراف خارجية",
                    content = "نلتزم بعدم بيع، أو تأجير، أو مشاركة أي من بيانات العملاء أو أرقامهم مع أي جهات خارجية أو إعلانية تحت أي ظرف."
                )

                PrivacyItemCard(
                    title = "4. الحماية والتشفير وقواعد RLS",
                    content = "تعتمد بنيتنا التحتية على أحدث تقنيات التشفير وقواعد أمان على مستوى الصفوف (Row Level Security)، مما يضمن عدم قدرة أي مستخدم على الاطلاع على أرقام أو بيانات عميل آخر."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacyItemCard(title: String, content: String) {
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
