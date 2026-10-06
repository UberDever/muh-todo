package dev.uberdever.muhtodo.document

object TagSyntax {
    private val token = Regex("#[\\p{L}\\p{Nd}_/.!?+*<>=:-]+")
    fun isValidToken(token: String): Boolean = this.token.matches(token)
}
