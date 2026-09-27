package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HyperionEmblem
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun AboutScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "About Hyperion",
            subtitle = "Version & Architecture",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Branding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.5.dp, MaritimeGoldBright)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HyperionEmblem(size = 56.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HYPERION",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaritimeGoldBright
                    )

                    Text(
                        text = "Maritime Licensure Reviewer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SeaCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SeaCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Aligned with MARINA & STCW Competency Standards",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Version 2.4.0 • Build 2026.09 (Release)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite.copy(alpha = 0.6f)
                    )
                }
            }

            // Developer Notes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ARCHITECTURE & PEDAGOGICAL DESIGN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AboutFeatureItem(
                        icon = Icons.Default.Storage,
                        title = "Offline-First Mobile Architecture",
                        description = "Designed for seamless shipboard study at sea without internet connection. State, question bank, bookmarks, and mock exam records persist locally."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AboutFeatureItem(
                        icon = Icons.Default.Psychology,
                        title = "Bloom's Cognitive Scoring Protocol",
                        description = "Questions carry difficulty-weighted point values: Easy (+3 pts), Moderate (+6 pts), and Difficult (+9 pts) with strict 70.0% cutoff calculation per competence."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AboutFeatureItem(
                        icon = Icons.Default.DeveloperBoard,
                        title = "Synthetic Item Bank",
                        description = "All exercises and scenarios are independently synthesized educational aids formulated for self-assessment and theoretical competency review."
                    )
                }
            }

            // Compliance & Non-Affiliation Declaration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SeaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STRICT NON-AFFILIATION NOTICE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hyperion is an independently authored review platform. It is NOT affiliated with, sponsored by, or endorsed by the Maritime Industry Authority (MARINA), International Maritime Organization (IMO), PRC, or CHED. Statutory competence designations (F1, F2, F3, C1..C11, OIC-NW, GOC) are cited strictly for curriculum organization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                        fontSize = 12.sp
                    )
                }
            }

            // Button to View Full Legal Agreement
            Button(
                onClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaritimeGoldBright,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Full Legal Terms & Academic Disclaimer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AboutFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaritimeGoldBright,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
