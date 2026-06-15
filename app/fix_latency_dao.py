import os
import re

test_dir = '/home/archon/0_lab/01_dev/forks/Sarmaya/app/src/test/java/com/sarmaya/app'

for file in ['AdversarialDataLayerTest.kt', 'AdversarialV3Test.kt', 'AdversarialV4Test.kt']:
    path = os.path.join(test_dir, file)
    with open(path, 'r') as f:
        content = f.read()

    # Add insertTransactions to LatencyFakeTransactionDao
    content = re.sub(
        r'(class LatencyFakeTransactionDao.*?\{)',
        r'\1\n        override suspend fun insertTransactions(transactions: List<com.sarmaya.app.data.Transaction>) {}',
        content,
        flags=re.DOTALL
    )

    with open(path, 'w') as f:
        f.write(content)
