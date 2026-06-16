import os
import glob

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if 'menuAnchor()' in content:
        content = content.replace('menuAnchor()', 'menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)')

    if 'KeyboardArrowRight' in content:
        content = content.replace('androidx.compose.material.icons.filled.KeyboardArrowRight', 'androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight')
        content = content.replace('Icons.Default.KeyboardArrowRight', 'androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight')

    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk('/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/main/java/com/sarmaya/app/ui'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file))

