package com.krumin.tonguecoinsmanager.util

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

@Composable
fun MarkdownText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    val annotatedString = remember(text, linkColor) {
        parseMarkdown(text, linkColor)
    }
    Text(
        text = annotatedString,
        style = style,
        modifier = modifier
    )
}

fun parseMarkdown(text: String, linkColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val matcher =
            Pattern.compile("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(\\[.*?\\]\\(.*?\\))").matcher(text)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            val match = matcher.group()
            when {
                match.startsWith("**") && match.endsWith("**") -> {
                    if (match.length >= 4) {
                        val content = match.substring(2, match.length - 2)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(content)
                        pop()
                    } else {
                        append(match)
                    }
                }

                match.startsWith("*") && match.endsWith("*") -> {
                    if (match.length >= 2) {
                        val content = match.substring(1, match.length - 1)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(content)
                        pop()
                    } else {
                        append(match)
                    }
                }

                match.startsWith("[") && match.contains("](") && match.endsWith(")") -> {
                    val labelEnd = match.indexOf("]")
                    val label = match.substring(1, labelEnd)
                    val url = match.substring(labelEnd + 2, match.length - 1)

                    pushStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = linkColor
                        )
                    )
                    append(label)
                    pop()
                    addStringAnnotation("URL", url, this.length - label.length, this.length)
                }
            }
            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
