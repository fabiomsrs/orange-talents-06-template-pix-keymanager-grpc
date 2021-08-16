package br.com.zup.edu.shared.response

import br.com.zup.edu.TipoChave
import br.com.zup.edu.shared.BankAccountDto
import br.com.zup.edu.shared.BankOwnerDto
import java.time.LocalDateTime

class BCBPixConsultaResponse(val keyType: TipoChave,
                             val key: String,
                             val bankAccount: BankAccountDto,
                             val owner: BankOwnerDto,
                             val createdAt: LocalDateTime
)