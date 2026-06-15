import os
import re

test_dir = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app'

for root, _, files in os.walk(test_dir):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()

            # Fix Stock(..., 0L) to Stock(..., priceUpdatedAt = 0L)
            content = re.sub(
                r'Stock\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*([0-9.]+)\s*,\s*0L\s*\)',
                r'Stock("\1", "\2", "\3", \4, priceUpdatedAt = 0L)',
                content
            )

            with open(path, 'w') as f:
                f.write(content)
