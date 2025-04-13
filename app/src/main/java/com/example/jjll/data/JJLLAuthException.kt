package com.example.jjll.data


/**
 * JJLL 應用程式中針對身份驗證特定錯誤的自訂異常類別。
 * 這有助於區分授權錯誤和一般網路或資料庫錯誤。
 *
 * @param message 解釋驗證錯誤的描述性訊息。
 * @param cause 此異常的根本原因（如果有）。
 */
class JJLLAuthException(
    override val message: String?,
    override val cause: Throwable? = null // Optional: include the original exception if needed
) : Exception(message, cause) {

    // You can add secondary constructors if needed, for example:
    constructor(message: String?) : this(message, null)
}