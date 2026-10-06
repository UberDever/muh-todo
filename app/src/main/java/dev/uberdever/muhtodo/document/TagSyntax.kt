package dev.uberdever.muhtodo.document

object TagSyntax {
    private val token = Regex("#[\\p{L}\\p{Nd}_/.!?+*<>=:-]+")
    fun isValidToken(token: String): Boolean = this.token.matches(token)
    fun compareTokens(left: String, right: String): Int {
        var leftIndex = 0
        var rightIndex = 0
        while (leftIndex < left.length && rightIndex < right.length) {
            val leftPoint = left.codePointAt(leftIndex)
            val rightPoint = right.codePointAt(rightIndex)
            val order = leftPoint.compareTo(rightPoint)
            if (order != 0) return order
            leftIndex += Character.charCount(leftPoint)
            rightIndex += Character.charCount(rightPoint)
        }
        return left.length.compareTo(right.length)
    }
}
