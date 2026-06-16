import os

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    content = content.replace('Icons.AutoMirrored.Filled.KeyboardArrowRight', 'Icons.Filled.KeyboardArrowRight')
    content = content.replace('androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight', 'androidx.compose.material.icons.filled.KeyboardArrowRight')
    
    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk('/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/main/java/com/sarmaya/app/ui'):
    for file in files:
        if file.endswith('.kt'):
            fix_file(os.path.join(root, file))
