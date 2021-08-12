package br.com.zup.edu.shared


data class BCBPixRequest(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccountDto,
    val owner: BankOwnerDto
)

data class BankAccountDto(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: String
)

data class BankOwnerDto(
    val type: String,
    val name: String,
    val taxIdNumber: String
)
