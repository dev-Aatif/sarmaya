import os

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Fix fully qualified references
    content = content.replace('androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight', 'Icons.AutoMirrored.Filled.KeyboardArrowRight')
    
    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk('/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/main/java/com/sarmaya/app/ui'):
    for file in files:
        if file.endswith('.kt'):
            fix_file(os.path.join(root, file))
