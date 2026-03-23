---
trigger: always_on
glob: "**/*.{kt,swift,xml,json,md}"
description: "Project-wide standards for Clean Architecture, MVI, performance, and UI/UX."
---

# 📜 Tongue Coins - Code Style & Project Guidelines

מסמך זה מהווה את "עשרת הדיברות" של הפרויקט. יש להיצמד אליו בכל פעולה - מפיתוח לוגיקה ועד עיצוב ממשק.

## 🏛 ארכיטקטורה וסטנדרטים (Architecture & Patterns)

1. **Clean Architecture + MVI:** הפרדת תחומי אחריות מלאה וקשיחה.
    * **Data Layer:** Repositories, DataSources, Services.
    * **Domain Layer:** UseCases, Models (Plain objects only).
    * **Presentation Layer:** ViewModels (MVI), UI Components.
2. **Single Source of Truth (SSOT):** המידע מוצג תמיד מתוך ה-DB או מה-Settings. הלוגיקה המקומית (
   Offline First) היא העוגן.
3. **No Code Duplication:** "אל תנבחו פעמיים". לוגיקה חייבת להיות Centralized בצורה אופטימלית לשימוש
   חוזר (Reuse).
4. **Design Patterns:** שימוש במיטב ה-Design Patterns. מחיקת קוד/משאבים/פרמטרים שאינם בשימוש (Dead
   Code Elimination).
5. **No Fully Qualified Names:** תמיד להשתמש ב-Imports ובשמות מקוצרים.
6. **Maximum DRY & Reuse:** מקסימום שימוש חוזר בקוד קיים. לוגיקה מרוכזת (Centralized) במקום אחד
   בלבד. אפס שכפול — עקרון DRY (Don't Repeat Yourself) במלוא העוצמה.

## 💻 כתיבת קוד (Coding Standards)

1. **Clean & Readable:** הקוד חייב להיות מודרני, קריא, קל לתחזוקה וטסטבילי (Testable).
2. **Naming:** שמות משתנים, פונקציות וחבילות (Packages) חייבים להיות מדויקים, אופטימליים ובעלי
   משמעות ברורה.
3. **Comments & Logs:** מחיקת הערות מיותרות או כפולות. אין להשאיר `printStackTrace` או לוגים מיותרים
   ב-Production.
4. **Concurrency & Safety:**
    * מניעה מוחלטת של **Race Conditions**.
    * שימוש ב-Mutex/Locks איפה שנדרש.
    * ביצועים מקסימליים: עבודה על ה-Main Thread רק מתי שחייב (UI). כל השאר (IO, Network) מתבצע ברקע.

## 🎨 עיצוב ומשאבים (UI, UX & Resources)

1. **No Hardcoded Values:** חל איסור מוחלט על ערכים קשיחים (Strings, Dimens, Magic Numbers). הכל
   חייב לצאת ממשאבים (Resources) ייעודיים.
2. **Theme Driven UI:** שימוש ב-Themes וערכי צבעים מוגדרים. אין להשתמש בצבעים (Colors) בצורה
   Hardcoded.
3. **Modern Aesthetics:** עיצוב עכשווי, משחקי, אינטואיטיבי וזורם. חוויית המשתמש צריכה להיות מהנה
   ומשחית (Playful).
4. **Responsiveness:** תמיכה מלאה בכל גדלי המסכים והמכשירים.
5. **Font Capping:** שינוי גודל פונט במכשיר לא ישפיע על התצוגה (כפי שהוגדר בפרויקט).

## 🚀 ביצועים וטיפול בשגיאות (Performance & Error Handling)

1. **Speed is Key:** ביצועים מהירים ויעילים ביותר (זיכרון וזמן ריצה).
2. **Resilient Networking:** מסכי שגיאה עם מנגנון התאוששות אוטומטי (Auto-recovery) בחזרת תקשורת.
3. **Friendly Feedback:** שגיאות יוצגו למשתמש ב-Snackbars בשפה ידידותית וקלילה (ברוח האפליקציה).
   לעולם לא להציג את הודעת השגיאה הגולמית (Raw Error).
4. **Silent Failure:** במצבים לא קריטיים שאינם פוגעים בחוויה - לשתוק.

## 🔄 סינכרון וחוויית משתמש (Sync & UX)

1. **Seamless Sync:** סינכרון אוטומטי ואופטימלי מול השרת (רשימת רמות, תכולה) מבלי לפגוע בחוויית
   המשחק.
2. **Auth Diversity:** תמיכה מלאה במשתמש אנונימי מול משתמש מחובר. שמירת התקדמות מקסימלית בשני
   המקרים.
3. **Logic Integrity:** זהירות מקסימלית בשינוי לוגיקה קיימת שעובדת. שמירה על Parity (זהות) בין
   אנדרואיד ל-iOS מלבד הניואנסים הפלטפורמיים המוגדרים (Sign-in providers).
4. **Backward Compatibility:** כל שינוי חייב לספק תמיכה מלאה לאחור (Backward Compatibility)
   למשתמשים קיימים. אסור לשבור פונקציונליות קיימת במשחק או לגרום לבעיות (נתונים, שמירת
   התקדמות, מסכי משחק וכו').

## 🧠 הגישה האינטליגנטית

* **Be Critical:** אל תהיה "Yes-man". אם גישה מסוימת שלי (המשתמש) אינה מדויקת או מוטעית - תקן אותי
  והצע את הדרך הנכונה והעדכנית ביותר.
* **Modern Inspiration:** שאב השראה מהמשחקים המודרניים והטובים ביותר בשוק.
