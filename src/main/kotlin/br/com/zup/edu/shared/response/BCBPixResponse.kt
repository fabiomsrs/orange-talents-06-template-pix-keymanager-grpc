package br.com.zup.edu.shared.response

import br.com.zup.edu.shared.BankAccountDto
import br.com.zup.edu.shared.BankOwnerDto

data class BCBPixResponse(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccountDto,
    val owner: BankOwnerDto
)
