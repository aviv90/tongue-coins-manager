package com.krumin.tonguecoinsmanager.data.infrastructure

object PromptTemplates {
    fun getCategoryGenerationPrompt(
        title: String,
        distinctContextPhotos: Map<String, String>
    ): String {
        val examples = distinctContextPhotos.entries
            .shuffled()
            .take(12)
            .joinToString("\n") { (t, c) -> "ביטוי: $t -> קטגוריות: $c" }

        return """
            אתה מומחה עילית לשפה העברית, מטבעות לשון (Idioms), תרבות ישראלית וסלנג.
            תפקידך להציע קטגוריות רלוונטיות, קצרות וחכמות עבור ביטוי (מטבע לשון) נתון.
            
            הנחיות:
            - החזר לכל היותר 2 קטגוריות רלוונטיות.
            - הקטגוריות חייבות להיות תמציתיות (1-3 מילים).
            - השתמש בעברית בלבד.
            - התמקד בתמות כמו: "סלנג", "יהדות", "צבא", "אוכל", "היסטוריה", "מוזיקה", "רגשות" וכו'.
            - המטרה היא לעזור למשתמש למצוא ולארגן את התמונות לפי נושאים.
            
            פורמט פלט:
            - החזר את התשובה כמערך JSON של מחרוזות בלבד.
            - דוגמה: ["סלנג עברי", "משפחה"]
            
            הקשר מהמאגר הקיים (דוגמאות לסגנון):
            $examples
            
            משימה נוכחית:
            ביטוי: $title
            החזר אך ורק את מערך ה-JSON (מקסימום 2 פריטים).
        """.trimIndent()
    }

    fun getImageEditingPrompt(userPrompt: String): String {
        return "ערוך את התמונה המצורפת בהתאם להוראה הבאה: $userPrompt"
    }
}
