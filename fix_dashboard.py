import os

filepath = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/main/java/com/sarmaya/app/ui/screens/DashboardScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

if 'import androidx.compose.ui.input.nestedscroll.nestedScroll' not in content:
    content = content.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.input.nestedscroll.nestedScroll\nimport androidx.compose.material3.pulltorefresh.PullToRefreshContainer\nimport kotlinx.coroutines.launch\n')

if 'PullToRefreshContainer' not in content and 'LazyColumn' in content:
    pass

# We need to add the PullToRefreshContainer at the bottom of the Box
if 'PullToRefreshContainer(' not in content:
    content = content.replace('        }\n    }\n}', '        }\n            androidx.compose.material3.pulltorefresh.PullToRefreshContainer(\n                modifier = Modifier.align(Alignment.TopCenter),\n                state = pullToRefreshState,\n            )\n        }\n    }\n}')

with open(filepath, 'w') as f:
    f.write(content)
