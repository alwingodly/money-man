#!/bin/bash

# List of files to update
files=(
    "app/src/main/java/com/alwin/moneymanager/ui/debt/DebtListScreen.kt"
    "app/src/main/java/com/alwin/moneymanager/ui/debt/DebtDetailScreen.kt"
    "app/src/main/java/com/alwin/moneymanager/ui/saving/SavingListScreen.kt"
    "app/src/main/java/com/alwin/moneymanager/ui/saving/SavingDetailScreen.kt"
    "app/src/main/java/com/alwin/moneymanager/ui/emi/EmiDetailScreen.kt"
    "app/src/main/java/com/alwin/moneymanager/ui/emi/EmiClosedListScreen.kt"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "Processing $file..."
        
        # Add imports if not present
        if ! grep -q "import androidx.compose.foundation.background" "$file"; then
            sed -i '' '/^package /a\
\
import androidx.compose.foundation.background\
import androidx.compose.foundation.shape.RoundedCornerShape
' "$file"
        fi
        
        echo "  ✓ Added imports to $file"
    fi
done

echo "Done! Please review the changes and manually update Card() calls to Box() with macOS styling."
