import os

filepath = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/main/java/com/sarmaya/app/ui/screens/DashboardScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('            androidx.compose.material3.pulltorefresh.PullToRefreshContainer(\n                modifier = Modifier.align(Alignment.TopCenter),\n                state = pullToRefreshState,\n            )\n', '')

with open(filepath, 'w') as f:
    f.write(content)
