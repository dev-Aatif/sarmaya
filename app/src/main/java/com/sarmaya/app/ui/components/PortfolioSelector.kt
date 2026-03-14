package com.sarmaya.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarmaya.app.data.Portfolio

@Composable
fun PortfolioSelector(
    activePortfolio: Portfolio?,
    allPortfolios: List<Portfolio>,
    onPortfolioSelected: (Long) -> Unit,
    onCreatePortfolio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPortfolioMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Portfolio") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Portfolio Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreatePortfolio(newName)
                            newName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPortfolioMenu = true }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                activePortfolio?.name ?: "All Portfolios",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = showPortfolioMenu,
            onDismissRequest = { showPortfolioMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            allPortfolios.forEach { portfolio ->
                DropdownMenuItem(
                    text = { Text(portfolio.name) },
                    onClick = {
                        onPortfolioSelected(portfolio.id)
                        showPortfolioMenu = false
                    },
                    trailingIcon = {
                        if (portfolio.id == activePortfolio?.id) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Create New Portfolio", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                onClick = {
                    showPortfolioMenu = false
                    showCreateDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )
        }
    }
}
