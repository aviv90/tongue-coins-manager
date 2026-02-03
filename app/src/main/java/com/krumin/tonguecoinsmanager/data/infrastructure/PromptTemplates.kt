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
            
            הנחיות קריטיות:
            - חשוב מאוד: אל תגלה את התשובה (הביטוי עצמו) בתוך הקטגוריות.
            - הקטגוריות צריכות לשמש כרמז כללי בלבד, כיוון תמתי רחב.
            - אל תאכיל את המשתמש בכפית - תן לו לנחש את הביטוי מהתמונה ומהרמזים הכלליים.
            - החזר לכל היותר 2 קטגוריות רלוונטיות.
            - הקטגוריות חייבות להיות תמציתיות (1-3 מילים).
            - השתמש בעברית בלבד.
            - התמקד בתמות רחבות כמו: "סלנג", "יהדות", "צבא", "אוכל", "היסטוריה", "מוזיקה", "רגשות", "טבע" וכו'.
            - המטרה היא לעזור למשתמש למצוא ולארגן את התמונות לפי נושאים כלליים מבלי להרוס את המשחק.
            
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
        return "Edit this image based on the following instruction: $userPrompt"
    }
}
